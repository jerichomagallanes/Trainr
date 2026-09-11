package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.diagnostics.NoBreadcrumbs
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.generation.SpentModels
import kotlinx.coroutines.delay

// Ask which movement fills each slot, repair what the schema could not rule
// out, and work out everything else here. Never ship a week the parser failed.
class GeminiPlanGenerator(
    private val client: PlanModelClient,
    private val promptBuilder: PlanPromptBuilder,
    private val catalog: ExerciseCatalog,
    private val spentModels: SpentModels,
    // Nothing from the profile goes in here. See Breadcrumbs.
    private val breadcrumbs: Breadcrumbs = NoBreadcrumbs
) : PlanGenerator {

    private val builder = PlanSkeletonBuilder(catalog)
    private val assembler = PlanAssembler(catalog)
    private val repair = PlanSelectionRepair()

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        if (catalog.all.isEmpty()) return PlanGenerationResult.Failed
        val skeleton = builder.build(request)
        if (!skeleton.isComplete) return PlanGenerationResult.Failed

        breadcrumbs.state("week", request.weekNumber.toString())
        breadcrumbs.state("movements_offered", skeleton.allowedKeys.size.toString())

        // Nothing left to choose, so asking would spend an allowance on nothing.
        if (skeleton.days.all { it.openSlots.isEmpty() }) {
            return assembler.assemble(skeleton, PlanSelection(), request)
                ?.let { PlanGenerationResult.Generated(it) }
                ?: PlanGenerationResult.Failed
        }

        val basePrompt = promptBuilder.userPrompt(request, skeleton)
        var feedback: List<String> = emptyList()
        var failure: PlanGenerationResult.Failure = PlanGenerationResult.Failed

        // Attempts are answers we could not use, and each costs a request from
        // a small daily allowance. Walking the model list costs nothing from
        // that budget, because the allowance is counted per model. Models
        // already known to be out of allowance today are not asked at all.
        val spent = spentModels.spentToday()
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

            val model = models[modelIndex]
            val prompt = if (feedback.isEmpty()) basePrompt else withFeedback(basePrompt, feedback)

            breadcrumbs.record("generation: asking $model, attempt ${attemptsSpent + 1}")

            val json = when (
                val answer = client.generate(
                    model = model,
                    systemInstruction = promptBuilder.systemInstruction(),
                    userPrompt = prompt,
                    skeleton = skeleton
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
                    breadcrumbs.record("generation: $model out of allowance")
                    spentModels.markSpent(model)
                    refusedOnQuota++
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                // Transient: ask the next model, do not spend an attempt, and
                // do not remember it.
                GeminiResponse.ModelUnavailable -> {
                    breadcrumbs.record("generation: $model unavailable")
                    failure = PlanGenerationResult.Failed
                    modelIndex++
                    continue
                }

                GeminiResponse.Failed -> null
            }

            // Every answer we cannot use spends an attempt and moves on: a
            // safety block on one model is often not one on the next, and the
            // next is a genuinely different opinion.
            attemptsSpent++
            modelIndex++

            if (json == null) {
                breadcrumbs.record("generation: $model gave no usable answer")
                failure = PlanGenerationResult.Failed
                continue
            }

            when (val repaired = repair.repair(json, skeleton)) {
                is SelectionRepairResult.Rejected -> {
                    // How many problems, never what they were: the messages
                    // quote the model's answer, written from the profile.
                    breadcrumbs.record("generation: answer rejected, ${repaired.problems.size} problems")
                    feedback = repaired.problems
                    failure = PlanGenerationResult.Failed
                }

                is SelectionRepairResult.Accepted -> {
                    breadcrumbs.record("generation: answer used, ${repaired.repairs} slots repaired")
                    assembler.assemble(skeleton, repaired.selection, request)?.let {
                        breadcrumbs.record("generation: plan accepted")
                        return PlanGenerationResult.Generated(it)
                    }
                    breadcrumbs.record("generation: the assembled week failed its own checks")
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
        appendLine("Choose again, fixing every problem listed.")
    }

    companion object {
        // Two, not three: with the answer this small, a third attempt can
        // only repeat a transport failure at the cost of one more request.
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_DELAY_MILLIS = 1_500L
    }
}
