package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

data class PlanRequest(
    val user: UserProfile,
    val weekNumber: Int,
    val startDateMillis: Long,
    // Newest first. A stall is two short weeks and a ramp back spans three,
    // so one previous week is not enough to progress from.
    val history: List<WeeklyWorkoutPlan> = emptyList(),
    // New movements were asked for, so last week's are not carried into it.
    val freshCast: Boolean = false
) {
    val previousWeek: WeeklyWorkoutPlan? get() = history.firstOrNull()
}

sealed interface PlanGenerationResult {
    data class Generated(val plan: WeeklyWorkoutPlan) : PlanGenerationResult

    // Nothing to build from: an empty catalog, or a week the app's own checks
    // turned down. Both are bugs rather than anything the client did, and
    // neither depends on a network this no longer touches.
    data object Failed : PlanGenerationResult
}

interface PlanGenerator {
    suspend fun generate(request: PlanRequest): PlanGenerationResult
}
