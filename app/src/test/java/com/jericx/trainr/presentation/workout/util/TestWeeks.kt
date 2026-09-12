package com.jericx.trainr.presentation.workout.util

import java.util.Calendar

// Local midnight, from the ISO weekday so the device locale's
// first-day-of-week can't move it.
fun mondayOf(nowMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = nowMillis
    val isoDay = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
    calendar.add(Calendar.DAY_OF_YEAR, 1 - isoDay)

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    calendar.clear()
    calendar.set(year, month, day)
    return calendar.timeInMillis
}
