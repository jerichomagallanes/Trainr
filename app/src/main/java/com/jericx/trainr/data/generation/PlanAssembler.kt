package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

// A skeleton and whatever was chosen for it, made into a week and held to the
// same parser and limits whoever did the choosing. Null means the app's own
// arithmetic produced something it would reject from a model.
class PlanAssembler(catalog: ExerciseCatalog) {

    private val expander = PlanExpander(catalog)
    private val parser = GeneratedPlanParser(catalog)

    fun assemble(skeleton: PlanSkeleton, selection: PlanSelection, request: PlanRequest): WeeklyWorkoutPlan? =
        when (
            val parsed = parser.parse(
                expander.expand(skeleton, selection, request),
                request.user.id,
                request.weekNumber,
                request.startDateMillis,
                PlanLimits(
                    maxSetsPerSession = skeleton.maxSetsPerSession,
                    allowedKeys = skeleton.allowedKeys,
                    requiredPatterns = skeleton.requiredPatterns,
                    sessionMinutes = request.user.workoutDuration,
                    sessionCeilingMinutes = skeleton.sessionCeilingMinutes
                )
            )
        ) {
            is PlanParseResult.Parsed -> parsed.plan
            is PlanParseResult.Invalid -> null
        }
}
