package com.jericx.trainr.presentation.workout.util

import java.util.TimeZone

// From the ISO weekday so the device locale's first-day-of-week can't move it.
fun mondayOf(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val date = WorkoutWeek.startOfDay(nowMillis, zone)
    return WorkoutWeek.dateOfDay(date, 2 - WorkoutWeek.isoWeekdayOf(date))
}
