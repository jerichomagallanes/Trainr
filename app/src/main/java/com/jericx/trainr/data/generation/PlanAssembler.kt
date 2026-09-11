package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

// A skeleton and a selection made into a week and checked against the
// skeleton's own limits. Null means the app's arithmetic produced something
// the checks turn down.
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
