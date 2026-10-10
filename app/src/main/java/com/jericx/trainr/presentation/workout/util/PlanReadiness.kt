package com.jericx.trainr.presentation.workout.util

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

private const val DAYS_PER_WEEK = 7

// A week added sooner would become the newest, which is the week the app calls
// yours, so one still being trained would stop being the current one. Enforced
// on the write, not in a menu's visibility.
fun WeeklyWorkoutPlan.isReadyForTheNextWeek(
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    val allDone = workoutDays.isNotEmpty() && workoutDays.all { it.countsAsCompleted }
    val start = startDateMillis ?: WorkoutWeek.startOfDay(createdAt)
    val weekIsOver = nowMillis >= WorkoutWeek.dateOfDay(start, DAYS_PER_WEEK + 1)

    return allDone || weekIsOver
}
