package com.jericx.trainr.presentation.workout.util

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus

private const val DAYS_PER_WEEK = 7

// A week added sooner would become the newest, which is the week the app calls
// yours, so one still being trained would stop being the current one. Enforced
// on the write, not in a menu's visibility.
fun WeeklyWorkoutPlan.isReadyForTheNextWeek(
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    val allDone = workoutDays.isNotEmpty() &&
        workoutDays.all { it.status == WorkoutStatus.COMPLETED }
    val start = startDateMillis ?: return allDone
    val weekIsOver = nowMillis >= WorkoutWeek.dateOfDay(start, DAYS_PER_WEEK + 1)

    return allDone || weekIsOver
}
