package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

data class PlanRequest(
    val user: UserProfile,
    val weekNumber: Int,
    val startDateMillis: Long,
    val languageCode: String,
    val previousWeek: WeeklyWorkoutPlan? = null
)

// Generation either produces a plan or explains why it could not. It used to
// answer null and let callers quietly substitute the built-in week, which told
// the client their coach had written them a plan when it had not.
sealed interface PlanGenerationResult {
    data class Generated(val plan: WeeklyWorkoutPlan) : PlanGenerationResult

    sealed interface Failure : PlanGenerationResult

    // The request never reached the model: no network, or it timed out trying.
    data object Offline : Failure

    // The model answered, but never with a plan that held up.
    data object Failed : Failure
}

interface PlanGenerator {
    suspend fun generate(request: PlanRequest): PlanGenerationResult
}
