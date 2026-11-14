package com.jericx.trainr.domain.unstuck

import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay

data class AdjustmentSnapshot(
    val day: WorkoutDay,
    val user: UserProfile,
    val priority: GoalPriority? = null
)

data class GoalPriority(val catalogKey: String)

// Whole-session and remaining are different questions and answering one with
// the other silently subtracts time nobody measured.
enum class TimeScope { WHOLE_SESSION, REMAINING }

sealed interface AdjustmentConstraint {

    data class LessTime(val minutes: Int, val scope: TimeScope) : AdjustmentConstraint

    data class EquipmentUnavailable(
        val exerciseId: Long,
        val available: Set<Equipment>
    ) : AdjustmentConstraint
}
