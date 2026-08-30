package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import kotlinx.coroutines.delay

// Generation is a conversation with a deadline: ask, validate, and when the
// answer breaks the contract, ask again quoting every problem. After that,
// report why it could not be done — never ship a plan that failed validation,
// and never let the caller mistake a failure for a plan.
class GeminiPlanGenerator(
    private val client: GeminiClient,
    private val parser: GeneratedPlanParser,
    private val promptBuilder: PlanPromptBuilder
) : PlanGenerator {

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        val basePrompt = promptBuilder.userPrompt(request)
        var feedback: List<String> = emptyList()
        // Whatever went wrong last is what the client hears about. A failure to
        // reach the model at all is worth saying plainly, so it survives the
        // loop rather than being flattened into "something went wrong".
        var failure: PlanGenerationResult.Failure = PlanGenerationResult.Failed

        // Two budgets, deliberately separate. Attempts are answers we were
        // given and could not use, and there are few of them because each one
        // costs a request from a small daily allowance. Walking the model list
        // costs nothing from that budget: a model that will not answer has not
        // answered, and the free allowance is counted per model, so the next
        // one has its own.
        var modelIndex = 0
        var attemptsSpent = 0

        while (modelIndex < GeminiClient.MODELS.size && attemptsSpent < MAX_ATTEMPTS) {
            if (attemptsSpent > 0) delay(RETRY_DELAY_MILLIS * attemptsSpent)

            val prompt =
                if (feedback.isEmpty()) basePrompt else withFeedback(basePrompt, feedback)

            val json = when (
                val answer = client.generate(
                    model = GeminiClient.MODELS[modelIndex],
                    systemInstruction = promptBuilder.systemInstruction(),
                    userPrompt = prompt,
                    responseSchema = GENERATED_PLAN_SCHEMA
                )
            ) {
                is GeminiResponse.Text -> answer.value

                // Nothing is reachable, so no other model will be either.
                GeminiResponse.Unreachable -> return PlanGenerationResult.Offline

                // Spent, retired or overloaded. Ask the next model, and do not
                // count it against the attempts: asking again is the one thing
                // guaranteed not to help, and on a spent allowance every extra
                // call is a request the client no longer has.
                GeminiResponse.ModelUnavailable -> {
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                // An unusable answer is transient (congestion, a dropped
                // connection) as often as it is fatal, so it spends an attempt,
                // not all of them.
                GeminiResponse.Failed -> {
                    failure = PlanGenerationResult.Failed
                    attemptsSpent++
                    continue
                }
            }

            attemptsSpent++

            when (
                val result = parser.parse(
                    json, request.user.id, request.weekNumber, request.startDateMillis
                )
            ) {
                is PlanParseResult.Parsed -> {
                    val plan = result.plan
                    if (plan.workoutDays.size == request.user.workoutDaysPerWeek) {
                        return PlanGenerationResult.Generated(plan)
                    }
                    feedback = listOf(
                        "plan: has ${plan.workoutDays.size} days but the client " +
                            "asked for exactly ${request.user.workoutDaysPerWeek}"
                    )
                    failure = PlanGenerationResult.Failed
                }

                is PlanParseResult.Invalid -> {
                    feedback = result.errors
                    failure = PlanGenerationResult.Failed
                }
            }
        }
        return failure
    }

    private fun withFeedback(basePrompt: String, errors: List<String>) = buildString {
        append(basePrompt)
        appendLine()
        appendLine("Your previous answer was rejected for these reasons:")
        errors.forEach { appendLine("- $it") }
        appendLine("Produce the corrected plan, fixing every problem listed.")
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MILLIS = 1_500L
    }
}
