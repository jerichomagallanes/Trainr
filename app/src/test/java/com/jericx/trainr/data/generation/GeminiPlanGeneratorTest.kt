package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.diagnostics.NoBreadcrumbs
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.generation.SkeletonSlot
import com.jericx.trainr.domain.generation.SpentModels
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UserProfile
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Test

class GeminiPlanGeneratorTest {

    private class FakeModelClient(answers: List<GeminiResponse>) : PlanModelClient {
        private val remaining = ArrayDeque(answers)
        val modelsAsked = mutableListOf<String>()
        val prompts = mutableListOf<String>()
        val skeletons = mutableListOf<PlanSkeleton>()

        override suspend fun generate(
            model: String,
            systemInstruction: String,
            userPrompt: String,
            skeleton: PlanSkeleton
        ): GeminiResponse {
            modelsAsked += model
            prompts += userPrompt
            skeletons += skeleton
            return remaining.removeFirstOrNull() ?: GeminiResponse.Failed
        }
    }

    private fun answering(vararg answers: GeminiResponse) = FakeModelClient(answers.toList())

    private fun text(body: String) = GeminiResponse.Text(body)

    private class FakeSpentModels(initial: Set<String> = emptySet()) : SpentModels {
        private val spent = initial.toMutableSet()
        override fun spentToday(): Set<String> = spent
        override fun markSpent(model: String) { spent += model }
    }

    private class FakeBreadcrumbs : Breadcrumbs {
        val events = mutableListOf<String>()
        val state = mutableMapOf<String, String>()
        override fun record(event: String) { events += event }
        override fun state(key: String, value: String) { state[key] = value }
        fun everything() = events + state.keys + state.values
    }

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())

    private fun generator(
        client: PlanModelClient,
        spentModels: SpentModels = FakeSpentModels(),
        breadcrumbs: Breadcrumbs = NoBreadcrumbs
    ) = GeminiPlanGenerator(
        client = client,
        promptBuilder = PlanPromptBuilder(),
        catalog = catalog,
        spentModels = spentModels,
        breadcrumbs = breadcrumbs
    )

    private fun request(daysPerWeek: Int = 1) = PlanRequest(
        user = UserProfile(id = 7, firstName = "Jericho", age = 30, workoutDaysPerWeek = daysPerWeek),
        weekNumber = 1,
        startDateMillis = 1_000L
    )

    private fun skeleton(request: PlanRequest = request()) = PlanSkeletonBuilder(catalog).build(request)

    // What a model that did its job would send: one of each slot's own
    // movements, and a name for every session.
    private fun answerFor(
        request: PlanRequest = request(),
        pick: (SkeletonSlot) -> String = { it.candidates.first() }
    ): String = buildJsonObject {
        skeleton(request).days.filter { it.openSlots.isNotEmpty() }.forEach { day ->
            putJsonObject(day.id) {
                day.openSlots.forEach { put(it.id, pick(it)) }
                put("title", "Whole Body Strength")
            }
        }
    }.toString()

    private val validPlanJson get() = answerFor()

    @Test
    fun aValidResponseBecomesAPlan() = runTest {
        val client = answering(text(validPlanJson))

        val plan = (generator(client).generate(request()) as PlanGenerationResult.Generated).plan!!

        assertThat(plan.userId).isEqualTo(7)
        assertThat(plan.startDateMillis).isEqualTo(1_000L)
        assertThat(plan.workoutDays).isNotEmpty()
        assertThat(client.modelsAsked).containsExactly(PlanModelClient.MODELS.first())
    }

    @Test
    fun anInvalidResponseIsRetriedWithTheValidationErrors() = runTest {
        val client = answering(text("""{ "title": " ", "days": [] }"""), text(validPlanJson))

        val plan = (generator(client).generate(request()) as PlanGenerationResult.Generated).plan

        assertThat(plan).isNotNull()
        assertThat(client.prompts).hasSize(2)
        assertThat(client.prompts[1]).contains("rejected")
        assertThat(client.prompts[1]).contains("You left out day")
    }

    @Test
    fun persistentGarbageGivesUpAfterTwoAttempts() = runTest {
        val client = answering(*Array(4) { text("not json at all") })

        assertThat(generator(client).generate(request())).isEqualTo(PlanGenerationResult.Failed)
        assertThat(client.prompts).hasSize(2)
    }

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

    @Test
    fun refusalsDoNotSpendTheAttemptsMeantForUnusableAnswers() = runTest {
        val client = answering(
            GeminiResponse.ModelUnavailable,
            GeminiResponse.ModelUnavailable,
            text("not json at all"),
            text(validPlanJson)
        )

        val result = generator(client).generate(request())

        // Two refusals, an unusable answer, then a good one: four calls, only the last two attempts
        assertThat(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
        assertThat(client.prompts).hasSize(4)
    }

    @Test
    fun beingOfflineStopsTheListAtOnce() = runTest {
        val client = answering(GeminiResponse.Unreachable, text(validPlanJson))

        val result = generator(client).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.Offline)
        assertThat(client.modelsAsked).hasSize(1)
    }

    // An alias resolves onto a model already in the list and shares its allowance, so it adds
    // waiting rather than capacity: gemini-flash-lite-latest is gemini-3.5-flash-lite
    @Test
    fun theModelListHoldsRealNamesRatherThanAliases() {
        assertThat(PlanModelClient.MODELS).isNotEmpty()
        assertThat(PlanModelClient.MODELS.filter { it.endsWith("-latest") }).isEmpty()
        assertThat(PlanModelClient.MODELS).containsNoDuplicates()
    }

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

    // Unavailable may answer a minute later; remembering it would strike a healthy model off for the day
    @Test
    fun `a model that is merely unavailable is not remembered`() = runTest {
        val spent = FakeSpentModels()

        generator(answering(GeminiResponse.ModelUnavailable, text(validPlanJson)), spent)
            .generate(request())

        assertThat(spent.spentToday()).isEmpty()
    }

    // With nothing left to skip to, asking beats refusing: the reset may have passed or the record be stale
    @Test
    fun `with every model spent it still asks rather than giving up`() = runTest {
        val spent = FakeSpentModels(PlanModelClient.MODELS.toSet())
        val client = answering(text(validPlanJson))

        val result = generator(client, spent).generate(request())

        assertThat(client.modelsAsked).isNotEmpty()
        assertThat(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
    }

    @Test
    fun `every model out of allowance reports the daily limit`() = runTest {
        val client = answering(
            *Array(PlanModelClient.MODELS.size) { GeminiResponse.QuotaSpent }
        )

        val result = generator(client).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.DailyLimitReached)
    }

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

    @Test
    fun `being offline is not reported as the daily limit`() = runTest {
        val result = generator(answering(GeminiResponse.Unreachable)).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.Offline)
    }

    @Test
    fun `an already exhausted chain reports the limit`() = runTest {
        val spent = FakeSpentModels(PlanModelClient.MODELS.toSet())
        val client = answering(
            *Array(PlanModelClient.MODELS.size) { GeminiResponse.QuotaSpent }
        )

        val result = generator(client, spent).generate(request())

        assertThat(result).isEqualTo(PlanGenerationResult.DailyLimitReached)
    }

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

    // Breadcrumbs are stored by Google and outlive the session, so no answer the client gave may
    // appear in one, including by way of a validation message quoting the model's own text
    @Test
    fun `no answer the client gave reaches the trail`() = runTest {
        val trail = FakeBreadcrumbs()
        val profile = UserProfile(
            id = 1,
            firstName = "Jericho",
            age = 31,
            height = 178f,
            weight = 75f,
            injuries = listOf(Injury.SHOULDER),
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
                startDateMillis = 0L
            )
        )

        val trailText = trail.everything().joinToString(" ")
        for (secret in listOf("Jericho", "31", "178", "75", "rotator cuff")) {
            assertThat(trailText).doesNotContain(secret)
        }
    }

    @Test
    fun theMovementTheModelChoseIsTheOneTrained() = runTest {
        val slot = skeleton().days.flatMap { it.openSlots }.first { it.candidates.size > 1 }
        val second = slot.candidates[1]
        val client = answering(text(answerFor { if (it == slot) second else it.candidates.first() }))

        val plan = (generator(client).generate(request()) as PlanGenerationResult.Generated).plan

        assertThat(plan.workoutDays.flatMap { it.exercises }.map { it.exerciseKey }).contains(second)
    }

    // One slip in a week is the app's to fix; asking again would spend a
    // request from the day's allowance on it.
    @Test
    fun anAnswerWithASlipIsRepairedRatherThanAskedAgain() = runTest {
        val threeDays = request(daysPerWeek = 3)
        val slot = skeleton(threeDays).days.first { it.openSlots.isNotEmpty() }.openSlots.first()
        val trail = FakeBreadcrumbs()
        val client = answering(text(answerFor(threeDays) { if (it == slot) "not_a_movement" else it.candidates.first() }))

        val result = generator(client, breadcrumbs = trail).generate(threeDays)

        assertThat(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
        assertThat(client.prompts).hasSize(1)
        assertThat(trail.events).contains("generation: answer used, 1 slots repaired")
    }

    @Test
    fun anAnswerThatIsNotAnObjectIsSentBackWithoutQuotingIt() = runTest {
        val client = answering(text("Sure! Here is the week."), text(validPlanJson))

        generator(client).generate(request())

        assertThat(client.prompts[1]).contains(PlanSelectionRepair.NOT_AN_OBJECT)
        assertThat(client.prompts[1]).doesNotContain("Sure!")
    }

    @Test
    fun theModelIsGivenTheSkeletonToChooseWithin() = runTest {
        val client = answering(text(validPlanJson))

        generator(client).generate(request())

        assertThat(client.skeletons.single()).isEqualTo(skeleton())
    }

    // The model only ever chooses among movements: one that takes the top of
    // every list gets exactly the week the app would have built alone.
    @Test
    fun choosingEveryTopCandidateGivesTheTemplateWeek() = runTest {
        val coached = (generator(answering(text(validPlanJson))).generate(request()) as PlanGenerationResult.Generated).plan
        val template = (TemplatePlanGenerator(catalog).generate(request()) as PlanGenerationResult.Generated).plan

        fun shape(days: List<com.jericx.trainr.domain.model.WorkoutDay>) = days.map { day ->
            day.exercises.map { exercise ->
                exercise.exerciseKey to exercise.sets.map { Triple(it.targetReps, it.targetWeightKg, it.targetSeconds) }
            }
        }
        assertThat(shape(coached.workoutDays)).isEqualTo(shape(template.workoutDays))
    }
}
