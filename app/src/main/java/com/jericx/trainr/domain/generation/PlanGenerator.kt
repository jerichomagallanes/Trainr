package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

data class PlanRequest(
    val user: UserProfile,
    val weekNumber: Int,
    val startDateMillis: Long,
    // Newest first. A stall is two short weeks and a ramp back spans three,
    // so one previous week is not enough to progress from.
    val history: List<WeeklyWorkoutPlan> = emptyList()
) {
    val previousWeek: WeeklyWorkoutPlan? get() = history.firstOrNull()
}

// Who chose the movements: the model, last week's cast carried forward, or
// the app's own ranking with no model at all.
enum class PlanSource { COACH, PROGRESSED, TEMPLATE }

sealed interface PlanGenerationResult {
    data class Generated(
        val plan: WeeklyWorkoutPlan,
        val source: PlanSource = PlanSource.COACH,
        // What the coach failed with, when this week was built in its place.
        // Null when nothing stood in for anything.
        val insteadOf: Failure? = null
    ) : PlanGenerationResult

    sealed interface Failure : PlanGenerationResult

    // The request never reached the model: no network, or it timed out trying.
    data object Offline : Failure

    // The model answered, but never with a plan that held up.
    data object Failed : Failure

    // Every model has spent its allowance for the day. Kept apart from Failed
    // because a retry here is a button the app already knows will fail.
    data object DailyLimitReached : Failure
}

interface PlanGenerator {
    suspend fun generate(request: PlanRequest): PlanGenerationResult
}
