package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

// Generation is a conversation with a deadline: ask, validate, and when the
// answer breaks the contract, ask again quoting every problem. After that,
// give up and let the caller fall back — never ship a plan that failed
// validation.
class GeminiPlanGenerator(
    private val client: GeminiClient,
    private val parser: GeneratedPlanParser,
    private val promptBuilder: PlanPromptBuilder
) : PlanGenerator {

    override suspend fun generate(request: PlanRequest): WeeklyWorkoutPlan? {
        val basePrompt = promptBuilder.userPrompt(request)
        var feedback: List<String> = emptyList()

        repeat(MAX_ATTEMPTS) {
            val prompt = if (feedback.isEmpty()) basePrompt else withFeedback(basePrompt, feedback)
            val json = client.generate(
                systemInstruction = promptBuilder.systemInstruction(),
                userPrompt = prompt,
                responseSchema = GENERATED_PLAN_SCHEMA
            ) ?: return null

            when (val result = parser.parse(
                json, request.user.id, request.weekNumber, request.startDateMillis
            )) {
                is PlanParseResult.Parsed -> {
                    val plan = result.plan
                    if (plan.workoutDays.size == request.user.workoutDaysPerWeek) return plan
                    feedback = listOf(
                        "plan: has ${plan.workoutDays.size} days but the client " +
                            "asked for exactly ${request.user.workoutDaysPerWeek}"
                    )
                }

                is PlanParseResult.Invalid -> feedback = result.errors
            }
        }
        return null
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
    }
}
