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
    // Training already done is still the client's to throw away — the app asks
    // first and says what goes, rather than deciding for them. Whether anything
    // was logged only changes how firmly it asks.
    val hasTraining: Boolean get() = completedDays > 0

    val completionPercentage: Int
        get() = if (totalDays == 0) 0 else (completedDays * 100.0 / totalDays).roundToInt()
}
