package com.jericx.trainr.presentation.workout.util

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus

private const val DAYS_PER_WEEK = 7

// A plan takes one week at a time: the next is built when the one being trained
// is finished, or when its dates have run out and it is not coming back. Adding
// one sooner would make the new week the newest, which is the week the app calls
// yours — so a week still being trained would quietly stop being the current one.
//
// It lives here rather than in a menu's visibility because it protects the plan,
// not the layout: a screen may forget to ask, and the write must refuse anyway.
fun WeeklyWorkoutPlan.isReadyForTheNextWeek(
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    val allDone = workoutDays.isNotEmpty() &&
        workoutDays.all { it.status == WorkoutStatus.COMPLETED }
    val start = startDateMillis ?: return allDone
    val weekIsOver = nowMillis >= WorkoutWeek.dateOfDay(start, DAYS_PER_WEEK + 1)

    return allDone || weekIsOver
}
