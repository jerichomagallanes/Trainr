package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.diagnostics.NoBreadcrumbs
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.SpentModels
import kotlinx.coroutines.delay

// Generation is a conversation with a deadline: ask, validate, and when the
// answer breaks the contract, ask again quoting every problem. After that,
// report why it could not be done — never ship a plan that failed validation,
// and never let the caller mistake a failure for a plan.
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
        // Models already known to be out of allowance today are not asked at
        // all. Each one would cost a full round trip to be told what it told us
        // this morning, and with five in the chain that is where a client's
        // minutes of waiting went.
        val spent = spentModels.spentToday()
        breadcrumbs.state("week", request.weekNumber.toString())
        breadcrumbs.state("models_spent_today", spent.size.toString())
        val models = PlanModelClient.MODELS.filterNot { it in spent }
            // Everything is spent, so there is nothing to skip to. Ask anyway
            // rather than refusing offline: the reset may have just passed, or
            // the record may be wrong, and one wasted call beats telling a
            // client the app is broken.
            .ifEmpty { PlanModelClient.MODELS }

        // Only worth reporting when the allowance is the whole story. A run
        // that also hit a bad answer or an overloaded model has an ordinary
        // failure to report, and telling that client to come back tomorrow
        // would send them away from something a retry would fix.
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

                // Out of allowance for the day. Remembered, so the next
                // generation skips it instead of learning this again.
                GeminiResponse.QuotaSpent -> {
                    breadcrumbs.record("generation: ${models[modelIndex]} out of allowance")
                    spentModels.markSpent(models[modelIndex])
                    refusedOnQuota++
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                // Retired, overloaded or too slow. Ask the next model, and do
                // not count it against the attempts: asking again is the one
                // thing guaranteed not to help. Not remembered, because this
                // one may answer perfectly well in a minute.
                GeminiResponse.ModelUnavailable -> {
                    breadcrumbs.record("generation: ${models[modelIndex]} unavailable")
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                // An unusable answer is transient (congestion, a dropped
                // connection) as often as it is fatal, so it spends an attempt,
                // not all of them.
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
                    // The count, not the client's schedule: how many days came
                    // back is the model's answer, and comparing it to what was
                    // asked is the whole point of the check.
                    breadcrumbs.record("generation: wrong number of days back")
                    feedback = listOf(
                        "plan: has ${plan.workoutDays.size} days but the client " +
                            "asked for exactly ${request.user.workoutDaysPerWeek}"
                    )
                    failure = PlanGenerationResult.Failed
                }

                is PlanParseResult.Invalid -> {
                    // How many problems, never what they were: a validation
                    // message can quote the model's own text back, and that text
                    // was written from the profile.
                    breadcrumbs.record("generation: answer rejected, ${result.errors.size} problems")
                    feedback = result.errors
                    failure = PlanGenerationResult.Failed
                }
            }
        }

        // Every model that was asked refused on allowance, and nothing else
        // went wrong: the day's budget is the reason, and saying so is worth
        // more to the client than a retry that cannot succeed.
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
