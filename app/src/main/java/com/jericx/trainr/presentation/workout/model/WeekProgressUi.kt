package com.jericx.trainr.presentation.workout.model

// completionPercentage is carried rather than derived: the designs show 2/3 days
// at 70% and 1/3 at 30%, so it is not a function of the day counts.
data class WeekProgressUi(
    val weekNumber: Int,
    val completedDays: Int,
    val totalDays: Int,
    val completionPercentage: Int,
    val status: WeekStatus
)
