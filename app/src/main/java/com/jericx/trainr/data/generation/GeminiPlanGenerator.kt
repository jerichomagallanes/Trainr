package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.diagnostics.NoBreadcrumbs
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.SpentModels
import kotlinx.coroutines.delay

class GeminiPlanGenerator(
    private val client: PlanModelClient,
    private val parser: GeneratedPlanParser,
    private val promptBuilder: PlanPromptBuilder,
    private val spentModels: SpentModels,
    // Nothing from the profile goes in here. See Breadcrumbs.
    private val breadcrumbs: Breadcrumbs = NoBreadcrumbs
) : PlanGenerator {

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        val basePrompt = promptBuilder.userPrompt(request)
        var feedback: List<String> = emptyList()
        var failure: PlanGenerationResult.Failure = PlanGenerationResult.Failed

        // Attempts are answers we could not use, and each costs a request from
        // a small daily allowance. Walking the model list costs nothing from
        // that budget, because the allowance is counted per model. Models
        // already known to be out of allowance today are not asked at all.
        val spent = spentModels.spentToday()
        breadcrumbs.state("week", request.weekNumber.toString())
        breadcrumbs.state("models_spent_today", spent.size.toString())
        val models = PlanModelClient.MODELS.filterNot { it in spent }
            // Everything is spent, so ask anyway: the reset may have just
            // passed, or the record may be wrong.
            .ifEmpty { PlanModelClient.MODELS }

        var refusedOnQuota = 0

        var modelIndex = 0
        var attemptsSpent = 0

        while (modelIndex < models.size && attemptsSpent < MAX_ATTEMPTS) {
            if (attemptsSpent > 0) delay(RETRY_DELAY_MILLIS * attemptsSpent)

            val prompt =
                if (feedback.isEmpty()) basePrompt else withFeedback(basePrompt, feedback)

            breadcrumbs.record("generation: asking ${models[modelIndex]}, attempt ${attemptsSpent + 1}")

            val json = when (
                val answer = client.generate(
                    model = models[modelIndex],
                    systemInstruction = promptBuilder.systemInstruction(),
                    userPrompt = prompt
                )
            ) {
                is GeminiResponse.Text -> answer.value

                // Nothing is reachable, so no other model will be either.
                GeminiResponse.Unreachable -> {
                    breadcrumbs.record("generation: nothing reachable")
                    return PlanGenerationResult.Offline
                }

                // Out of allowance today, and remembered so the next
                // generation skips it.
                GeminiResponse.QuotaSpent -> {
                    breadcrumbs.record("generation: ${models[modelIndex]} out of allowance")
                    spentModels.markSpent(models[modelIndex])
                    refusedOnQuota++
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                // Transient: ask the next model, do not spend an attempt, and
                // do not remember it.
                GeminiResponse.ModelUnavailable -> {
                    breadcrumbs.record("generation: ${models[modelIndex]} unavailable")
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                // An unusable answer is as often transient as fatal, so it
                // spends one attempt, not all of them.
                GeminiResponse.Failed -> {
                    breadcrumbs.record("generation: ${models[modelIndex]} gave no usable answer")
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
                        breadcrumbs.record("generation: plan accepted")
                        return PlanGenerationResult.Generated(plan)
                    }
                    breadcrumbs.record("generation: wrong number of days back")
                    feedback = listOf(
                        "plan: has ${plan.workoutDays.size} days but the client " +
                            "asked for exactly ${request.user.workoutDaysPerWeek}"
                    )
                    failure = PlanGenerationResult.Failed
                }

                is PlanParseResult.Invalid -> {
                    // How many problems, never what they were: the messages
                    // can quote model text written from the profile.
                    breadcrumbs.record("generation: answer rejected, ${result.errors.size} problems")
                    feedback = result.errors
                    failure = PlanGenerationResult.Failed
                }
            }
        }

        // Only when the allowance is the whole story: any other failure is
        // worth a retry.
        if (refusedOnQuota == models.size) {
            breadcrumbs.record("generation: every model out of allowance")
            return PlanGenerationResult.DailyLimitReached
        }

        breadcrumbs.record("generation: gave up after $attemptsSpent attempts")
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
