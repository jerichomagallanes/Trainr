package com.jericx.trainr.presentation.workout.util

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import java.util.Calendar
import java.util.TimeZone

// Calendar rather than java.time: minSdk is 24 and desugaring is off.
object WorkoutWeek {

    // A week's dates belong to the plan, not to where the phone is now, so a
    // date is an instant in this fixed zone and only today is read in the
    // device's own. Anchoring to the device zone re-dated the whole week when
    // the phone moved west.
    private val DATE_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

    fun dateOf(year: Int, month: Int, day: Int): Long =
        dateCalendar().run {
            set(year, month, day)
            timeInMillis
        }

    // The calendar date the device is living in, as a week date.
    fun startOfDay(
        nowMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): Long =
        Calendar.getInstance(zone).run {
            timeInMillis = nowMillis
            dateOf(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
        }

    // A week is over once its last day has passed, which is the rule the plan
    // list reads a single day by.
    fun hasEnded(
        startDateMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): Boolean = dateOfDay(startDateMillis, LAST_ISO_DAY) < startOfDay(nowMillis, zone)

    fun dateOfDay(startDateMillis: Long, dayNumber: Int): Long =
        dateCalendar().run {
            timeInMillis = startDateMillis
            add(Calendar.DAY_OF_YEAR, dayNumber - 1)
            timeInMillis
        }

    // ISO numbering, Monday 1 to Sunday 7.
    fun isoWeekdayOf(dateMillis: Long): Int =
        dateCalendar().run {
            timeInMillis = dateMillis
            (get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        }

    private fun dateCalendar(): Calendar = Calendar.getInstance(DATE_ZONE).apply { clear() }

    private const val LAST_ISO_DAY = 7
}

// The one fallback for a plan stored before start dates were written: the day
// it was made, read where the device was when it read it.
fun WeeklyWorkoutPlan.weekStartDate(zone: TimeZone = TimeZone.getDefault()): Long =
    startDateMillis ?: WorkoutWeek.startOfDay(createdAt, zone)
