package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
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

    private fun generator(client: PlanModelClient) = GeminiPlanGenerator(
        client = client,
        parser = GeneratedPlanParser(),
        promptBuilder = PlanPromptBuilder()
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
}
