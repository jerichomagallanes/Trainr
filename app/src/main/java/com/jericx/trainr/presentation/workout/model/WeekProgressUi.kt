package com.jericx.trainr.presentation.workout.model

import kotlin.math.roundToInt

data class WeekProgressUi(
    val weekNumber: Int,
    val completedDays: Int,
    val totalDays: Int,
    val status: WeekStatus,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val planId: Long = 0
) {
    // Only a week that has not begun can be thrown away: anything else is a
    // record of training that happened.
    val canDelete: Boolean get() = status == WeekStatus.UPCOMING

    val completionPercentage: Int
        get() = if (totalDays == 0) 0 else (completedDays * 100.0 / totalDays).roundToInt()
}
