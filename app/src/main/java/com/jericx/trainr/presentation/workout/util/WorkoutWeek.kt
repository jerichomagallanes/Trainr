package com.jericx.trainr.presentation.workout.util

import java.util.Calendar

// Calendar rather than java.time: minSdk is 24 and desugaring is off.
object WorkoutWeek {

    // Local midnight of the Monday of the week containing nowMillis, computed
    // from the ISO weekday so the device locale's first-day-of-week can't move it.
    fun mondayOf(nowMillis: Long = System.currentTimeMillis()): Long {
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

    // Local midnight of the day containing nowMillis, so "has this date passed"
    // is answered by the calendar rather than by the time of day.
    fun startOfDay(nowMillis: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().run {
            timeInMillis = nowMillis
            val year = get(Calendar.YEAR)
            val month = get(Calendar.MONTH)
            val day = get(Calendar.DAY_OF_MONTH)
            clear()
            set(year, month, day)
            timeInMillis
        }

    fun dateOfDay(startDateMillis: Long, dayNumber: Int): Long =
        Calendar.getInstance().run {
            timeInMillis = startDateMillis
            add(Calendar.DAY_OF_YEAR, dayNumber - 1)
            timeInMillis
        }
}
