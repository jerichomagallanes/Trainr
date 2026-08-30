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

// Null means generation is unavailable or failed after retries; callers fall
// back to the built-in sample week rather than blocking the user.
interface PlanGenerator {
    suspend fun generate(request: PlanRequest): WeeklyWorkoutPlan?
}
