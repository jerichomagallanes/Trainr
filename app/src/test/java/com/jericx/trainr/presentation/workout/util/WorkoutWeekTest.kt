package com.jericx.trainr.presentation.workout.util

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class WorkoutWeekTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance().run {
            clear()
            set(year, month, day, hour, 0)
            timeInMillis
        }

    // 2026-08-26 is a Wednesday; its ISO week starts Monday the 24th.
    @Test
    fun aMidweekMomentBelongsToThatWeeksMonday() {
        val wednesdayNoon = millisOf(2026, Calendar.AUGUST, 26, hour = 12)

        assertThat(WorkoutWeek.mondayOf(wednesdayNoon))
            .isEqualTo(millisOf(2026, Calendar.AUGUST, 24))
    }

    // Sunday closes the ISO week, so it must not roll forward to the next
    // Monday whatever the locale calls the first day of the week.
    @Test
    fun aSundayBelongsToTheMondayBeforeIt() {
        val sunday = millisOf(2026, Calendar.AUGUST, 30, hour = 23)

        assertThat(WorkoutWeek.mondayOf(sunday))
            .isEqualTo(millisOf(2026, Calendar.AUGUST, 24))
    }

    @Test
    fun aMondayIsItsOwnWeekStart() {
        val mondayEvening = millisOf(2026, Calendar.AUGUST, 24, hour = 21)

        assertThat(WorkoutWeek.mondayOf(mondayEvening))
            .isEqualTo(millisOf(2026, Calendar.AUGUST, 24))
    }

    @Test
    fun dayNumbersWalkMondayToSunday() {
        val monday = millisOf(2026, Calendar.AUGUST, 24)

        val weekdays = (1..7).map {
            WorkoutDateFormatter.formatWeekday(WorkoutWeek.dateOfDay(monday, it), Locale.US)
        }

        assertThat(weekdays).containsExactly(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        ).inOrder()
    }

    @Test
    fun theWeekCanCrossAMonthEnd() {
        val monday = millisOf(2026, Calendar.AUGUST, 31)

        assertThat(WorkoutWeek.dateOfDay(monday, 2))
            .isEqualTo(millisOf(2026, Calendar.SEPTEMBER, 1))
    }
}
