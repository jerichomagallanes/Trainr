package com.jericx.trainr.presentation.workout.util

import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class WorkoutDateFormatterTest {

    private lateinit var originalTimeZone: TimeZone

    // Pinned so assertions don't depend on the machine's zone.
    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun dateOf(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month, day) }.timeInMillis

    @Test
    fun formatsTheFullDateShownOnARoutine() {
        val wednesday = dateOf(2025, Calendar.JULY, 23)

        assertThat(WorkoutDateFormatter.formatFullDate(wednesday, Locale.US))
            .isEqualTo("Wednesday, July 23, 2025")
    }

    @Test
    fun formatsTheWeekdayName() {
        assertThat(WorkoutDateFormatter.formatWeekday(dateOf(2025, Calendar.JULY, 21), Locale.US))
            .isEqualTo("Monday")
        assertThat(WorkoutDateFormatter.formatWeekday(dateOf(2025, Calendar.JULY, 25), Locale.US))
            .isEqualTo("Friday")
    }

    @Test
    fun collapsesTheMonthWhenAWeekStaysWithinOne() {
        val range = WorkoutDateFormatter.formatWeekRange(
            startMillis = dateOf(2025, Calendar.JULY, 21),
            endMillis = dateOf(2025, Calendar.JULY, 27),
            locale = Locale.US
        )

        assertThat(range).isEqualTo("July 21 – 27, 2025")
    }

    @Test
    fun repeatsTheMonthWhenAWeekCrossesOne() {
        val range = WorkoutDateFormatter.formatWeekRange(
            startMillis = dateOf(2025, Calendar.AUGUST, 26),
            endMillis = dateOf(2025, Calendar.SEPTEMBER, 1),
            locale = Locale.US
        )

        assertThat(range).isEqualTo("August 26 – September 1, 2025")
    }

    @Test
    fun repeatsTheYearWhenAWeekCrossesOne() {
        val range = WorkoutDateFormatter.formatWeekRange(
            startMillis = dateOf(2025, Calendar.DECEMBER, 29),
            endMillis = dateOf(2026, Calendar.JANUARY, 4),
            locale = Locale.US
        )

        assertThat(range).isEqualTo("December 29, 2025 – January 4, 2026")
    }

    @Test
    fun abbreviatesMonthNamesForTheProgressScreen() {
        val range = WorkoutDateFormatter.formatWeekRange(
            startMillis = dateOf(2025, Calendar.JULY, 21),
            endMillis = dateOf(2025, Calendar.JULY, 27),
            locale = Locale.US,
            abbreviated = true
        )

        assertThat(range).isEqualTo("Jul 21 – 27, 2025")
    }

    @Test
    fun localisesTheFullDateRatherThanReorderingEnglish() {
        val wednesday = dateOf(2025, Calendar.JULY, 23)

        val japanese = WorkoutDateFormatter.formatFullDate(wednesday, Locale.JAPANESE)

        assertThat(japanese).isNotEqualTo(WorkoutDateFormatter.formatFullDate(wednesday, Locale.US))
        assertThat(japanese).contains("2025")
        assertThat(japanese).doesNotContain("July")
        assertThat(japanese).doesNotContain("Wednesday")
    }

    @Test
    fun localisesWeekdayNames() {
        val monday = dateOf(2025, Calendar.JULY, 21)

        assertThat(WorkoutDateFormatter.formatWeekday(monday, Locale.JAPANESE))
            .doesNotContain("Monday")
    }
}
