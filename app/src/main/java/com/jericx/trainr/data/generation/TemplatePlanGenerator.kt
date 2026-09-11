package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder

// A whole week with no model at all: the skeleton, the top of every list, and
// the engine's numbers. It goes through the same parser and the same limits a
// model's answer would, so it can never hand over something the app would
// reject from anyone else.
class TemplatePlanGenerator(private val catalog: ExerciseCatalog) : PlanGenerator {

    private val builder = PlanSkeletonBuilder(catalog)
    private val expander = PlanExpander(catalog)
    private val parser = GeneratedPlanParser(catalog)

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        if (catalog.all.isEmpty()) return PlanGenerationResult.Failed
        val skeleton = builder.build(request)
        if (skeleton.days.isEmpty() || skeleton.days.any { it.slots.isEmpty() }) {
            return PlanGenerationResult.Failed
        }
        val generated = expander.expand(skeleton, PlanSelection(), request)
        return when (
            val parsed = parser.parse(
                generated, request.user.id, request.weekNumber, request.startDateMillis,
                limitsFor(skeleton, request)
            )
        ) {
            is PlanParseResult.Parsed -> PlanGenerationResult.Generated(parsed.plan)
            is PlanParseResult.Invalid -> PlanGenerationResult.Failed
        }
    }

    private fun limitsFor(skeleton: PlanSkeleton, request: PlanRequest) = PlanLimits(
        maxSetsPerSession = skeleton.maxSetsPerSession,
        allowedKeys = skeleton.allowedKeys,
        requiredPatterns = skeleton.requiredPatterns,
        sessionMinutes = request.user.workoutDuration,
        sessionCeilingMinutes = skeleton.sessionCeilingMinutes
    )
}
