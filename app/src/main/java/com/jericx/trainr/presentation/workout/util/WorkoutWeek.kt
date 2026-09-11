package com.jericx.trainr.presentation.workout.util

import java.util.Calendar

// Calendar rather than java.time: minSdk is 24 and desugaring is off.
object WorkoutWeek {

    // Local midnight, so "has this date passed" ignores the time of day.
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
