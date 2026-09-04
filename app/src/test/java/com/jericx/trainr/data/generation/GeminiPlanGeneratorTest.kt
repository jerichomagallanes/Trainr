package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.diagnostics.NoBreadcrumbs
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.SpentModels
import com.jericx.trainr.domain.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeminiPlanGeneratorTest {

    // The model is asked through an interface, so these tests are about what
    // the generator does with an answer — retrying, walking the model list,
    // giving up — rather than about how the answer got here.
    private class FakeModelClient(answers: List<GeminiResponse>) : PlanModelClient {
        private val remaining = ArrayDeque(answers)
        val modelsAsked = mutableListOf<String>()
        val prompts = mutableListOf<String>()

        override suspend fun generate(
            model: String,
            systemInstruction: String,
            userPrompt: String
        ): GeminiResponse {
            modelsAsked += model
            prompts += userPrompt
            return remaining.removeFirstOrNull() ?: GeminiResponse.Failed
        }
    }

    private fun answering(vararg answers: GeminiResponse) = FakeModelClient(answers.toList())

    private fun text(body: String) = GeminiResponse.Text(body)

    // Remembers in memory what the real one remembers on disk.
    private class FakeSpentModels(initial: Set<String> = emptySet()) : SpentModels {
        private val spent = initial.toMutableSet()
        override fun spentToday(): Set<String> = spent
        override fun markSpent(model: String) { spent += model }
    }

    // Keeps everything it was told, so a test can read the whole trail.
    private class FakeBreadcrumbs : Breadcrumbs {
        val events = mutableListOf<String>()
        val state = mutableMapOf<String, String>()
        override fun record(event: String) { events += event }
        override fun state(key: String, value: String) { state[key] = value }
        fun everything() = events + state.keys + state.values
    }

    private fun generator(
        client: PlanModelClient,
        spentModels: SpentModels = FakeSpentModels(),
        breadcrumbs: Breadcrumbs = NoBreadcrumbs
    ) = GeminiPlanGenerator(
        client = client,
        parser = GeneratedPlanParser(),
        promptBuilder = PlanPromptBuilder(),
        spentModels = spentModels,
        breadcrumbs = breadcrumbs
    )

    private fun request(daysPerWeek: Int = 1) = PlanRequest(
        user = UserProfile(id = 7, firstName = "Jericho", age = 30, workoutDaysPerWeek = daysPerWeek),
        weekNumber = 1,
        startDateMillis = 1_000L,
        languageCode = "en"
    )

    private val validPlanJson = """
        {
          "title": "Week 1",
          "days": [
            {
              "dayNumber": 1,
              "title": "Full Body",
              "equipment": ["Dumbbells"],
              "exercises": [
                {
                  "exerciseKey": "goblet_squat",
                  "name": "Goblet Squats",
                  "measure": "WEIGHT_AND_REPS",
                  "durationMinutes": 8,
                  "prescription": "3 sets of 12 reps",
                  "instructions": "Squat holding a dumbbell at your chest.",
                  "restSeconds": 60,
                  "sets": [
                    { "reps": 12, "weightKg": 20 },
                    { "reps": 12, "weightKg": 20 },
                    { "reps": 12, "weightKg": 20 }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun aValidResponseBecomesAPlan() = runTest {
        val client = answering(text(validPlanJson))

        val plan = (generator(client).generate(request()) as PlanGenerationResult.Generated).plan!!

        assertThat(plan.userId).isEqualTo(7)
        assertThat(plan.startDateMillis).isEqualTo(1_000L)
        assertThat(plan.workoutDays.single().exercises.single().exerciseKey)
            .isEqualTo("goblet_squat")
        // The strongest model is asked first and, answering, is the only one asked.
        assertThat(client.modelsAsked).containsExactly(PlanModelClient.MODELS.first())
    }

    @Test
    fun anInvalidResponseIsRetriedWithTheValidationErrors() = runTest {
        val client = answering(text("""{ "title": " ", "days": [] }"""), text(validPlanJson))

        val plan = (generator(client).generate(request()) as PlanGenerationResult.Generated).plan

        assertThat(plan).isNotNull()
        assertThat(client.prompts).hasSize(2)
        assertThat(client.prompts[1]).contains("rejected")
        assertThat(client.prompts[1]).contains("plan: has no days")
    }

    @Test
    fun theWrongNumberOfDaysIsRejectedAndRetried() = runTest {
        val client = answering(text(validPlanJson), text(validPlanJson), text(validPlanJson))

        val result = generator(client).generate(request(daysPerWeek = 3))

        assertThat(result).isEqualTo(PlanGenerationResult.Failed)
        assertThat(client.prompts).hasSize(3)
        assertThat(client.prompts[1]).contains("asked for exactly 3")
    }

    @Test
    fun persistentGarbageGivesUpAfterThreeAttempts() = runTest {
        val client = answering(*Array(4) { text("not json at all") })

        assertThat(generator(client).generate(request())).isEqualTo(PlanGenerationResult.Failed)
        assertThat(client.prompts).hasSize(3)
    }

    // An answer that cannot be used is worth another go; a model that will not
    // answer is worth someone else.
    @Test
    fun aModelThatWillNotAnswerHandsOverToTheNextOne() = runTest {
        val client = answering(GeminiResponse.ModelUnavailable, text(validPlanJson))

        val result = generator(client).generate(request())

        assertThat(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
        assertThat(client.modelsAsked)
            .containsExactly(PlanModelClient.MODELS[0], PlanModelClient.MODELS[1])
            .inOrder()
    }

    @Test
    fun everyModelRefusingFailsSoftly() = runTest {
        val client = answering(
            *Array(PlanModelClient.MODELS.size) { GeminiResponse.ModelUnavailable }
        )

        assertThat(generator(client).generate(request())).isEqualTo(PlanGenerationResult.Failed)
        assertThat(client.modelsAsked).containsExactlyElementsIn(PlanModelClient.MODELS).inOrder()
    }

    // Asking a model that has run out is the one thing guaranteed not to help,
    // and every extra call is a request the client no longer has. So a refusal
    // moves along the list rather than spending an attempt — refusals still
    // leave the attempts intact for a model that will answer.
    @Test
    fun refusalsDoNotSpendTheAttemptsMeantForUnusableAnswers() = runTest {
        val client = answering(
            GeminiResponse.ModelUnavailable,
            GeminiResponse.ModelUnavailable,
            text("not json at all"),
            text(validPlanJson)
        )

        val result = generator(client).generate(request())

        // Two refusals, then a genuine answer that was unusable, then one that
        // was not: four calls, of which only the last two were attempts.
        assertThat(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
        assertThat(client.prompts).hasSize(4)
    }

    // Nothing is reachable, so no other model will be either: the list stops
    // rather than working through five models that cannot be called.
    @Test
    fun beingOfflineStopsTheListAtOnce() = runTest {
        val client = answering(GeminiResponse.Unreachable, text(validPlanJson))

        val result = generator(client).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.Offline)
        assertThat(client.modelsAsked).hasSize(1)
    }

    // An alias resolves onto a model that is already in the list and shares its
    // allowance, so it would add waiting rather than capacity: driving
    // gemini-3.5-flash-lite to its per-minute limit refuses
    // gemini-flash-lite-latest in the same breath. Checked here because the
    // list looks like somewhere you would helpfully add more names.
    @Test
    fun theModelListHoldsRealNamesRatherThanAliases() {
        assertThat(PlanModelClient.MODELS).isNotEmpty()
        assertThat(PlanModelClient.MODELS.filter { it.endsWith("-latest") }).isEmpty()
        assertThat(PlanModelClient.MODELS).containsNoDuplicates()
    }

    // The whole point of the change: a model that said it was out of allowance
    // this morning is not asked again this afternoon. Each pointless ask costs
    // a full round trip, and with five models that is where the minutes went.
    @Test
    fun `a model that is out of allowance is not asked again`() = runTest {
        val spent = FakeSpentModels()
        val first = answering(GeminiResponse.QuotaSpent, text(validPlanJson))

        generator(first, spent).generate(request())

        assertThat(spent.spentToday()).containsExactly(PlanModelClient.MODELS.first())

        val second = answering(text(validPlanJson))
        generator(second, spent).generate(request())

        assertThat(second.modelsAsked).doesNotContain(PlanModelClient.MODELS.first())
        assertThat(second.modelsAsked.first()).isEqualTo(PlanModelClient.MODELS[1])
    }

    // Overloaded or slow is not the same as out of allowance. It may answer
    // perfectly well a minute later, so remembering it would strike a healthy
    // model off the list for the rest of the day.
    @Test
    fun `a model that is merely unavailable is not remembered`() = runTest {
        val spent = FakeSpentModels()

        generator(answering(GeminiResponse.ModelUnavailable, text(validPlanJson)), spent)
            .generate(request())

        assertThat(spent.spentToday()).isEmpty()
    }

    // Everything is spent, so there is nothing to skip to. Asking anyway beats
    // refusing: the reset may have just passed, or the record may be stale.
    @Test
    fun `with every model spent it still asks rather than giving up`() = runTest {
        val spent = FakeSpentModels(PlanModelClient.MODELS.toSet())
        val client = answering(text(validPlanJson))

        val result = generator(client, spent).generate(request())

        assertThat(client.modelsAsked).isNotEmpty()
        assertThat(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
    }

    // Every model out of allowance is a different thing from a failed run, and
    // the client needs opposite advice for each: wait, or try again.
    @Test
    fun `every model out of allowance reports the daily limit`() = runTest {
        val client = answering(
            *Array(PlanModelClient.MODELS.size) { GeminiResponse.QuotaSpent }
        )

        val result = generator(client).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.DailyLimitReached)
    }

    // A run that also hit a bad answer has an ordinary failure to report.
    // Telling this client to come back tomorrow would send them away from
    // something a retry would have fixed.
    @Test
    fun `a mixed failure is not reported as the daily limit`() = runTest {
        val client = answering(
            GeminiResponse.QuotaSpent,
            GeminiResponse.Failed,
            GeminiResponse.Failed,
            GeminiResponse.Failed
        )

        val result = generator(client).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.Failed)
    }

    // Nothing reached the model at all, which says nothing about allowance.
    @Test
    fun `being offline is not reported as the daily limit`() = runTest {
        val result = generator(answering(GeminiResponse.Unreachable)).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.Offline)
    }

    // Models already known to be spent are skipped, so a client whose whole
    // chain was recorded this morning is told the truth without a single call.
    @Test
    fun `an already exhausted chain reports the limit`() = runTest {
        val spent = FakeSpentModels(PlanModelClient.MODELS.toSet())
        val client = answering(
            *Array(PlanModelClient.MODELS.size) { GeminiResponse.QuotaSpent }
        )

        val result = generator(client, spent).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.DailyLimitReached)
    }

    // The trail is what makes a crash report worth reading: it says which model
    // was asked, in what order, and what each one said.
    @Test
    fun `the trail records the walk through the models`() = runTest {
        val trail = FakeBreadcrumbs()
        val client = answering(
            GeminiResponse.QuotaSpent,
            GeminiResponse.ModelUnavailable,
            text(validPlanJson)
        )

        generator(client, breadcrumbs = trail).generate(request())

        assertThat(trail.events.any { it.contains("out of allowance") }).isTrue()
        assertThat(trail.events.any { it.contains("unavailable") }).isTrue()
        assertThat(trail.events).contains("generation: plan accepted")
        assertThat(trail.state["week"]).isEqualTo("1")
    }

    // The policy promises a crash report says what broke, not who the client is.
    // A breadcrumb is stored by Google and outlives the session, so no answer
    // the client gave may appear in one — and a validation message can quote the
    // model's own text, which was written from the profile.
    @Test
    fun `no answer the client gave reaches the trail`() = runTest {
        val trail = FakeBreadcrumbs()
        val profile = UserProfile(
            id = 1,
            firstName = "Jericho",
            age = 31,
            height = 178f,
            weight = 75f,
            injuries = listOf("Left rotator cuff"),
            workoutDaysPerWeek = 1
        )
        val client = answering(
            GeminiResponse.Failed,
            text("""{ "title": " ", "days": [] }"""),
            text(validPlanJson)
        )

        generator(client, breadcrumbs = trail).generate(
            PlanRequest(
                user = profile,
                weekNumber = 1,
                startDateMillis = 0L,
                languageCode = "en"
            )
        )

        val trailText = trail.everything().joinToString(" ")
        for (secret in listOf("Jericho", "31", "178", "75", "rotator cuff")) {
            assertThat(trailText).doesNotContain(secret)
        }
    }
}
