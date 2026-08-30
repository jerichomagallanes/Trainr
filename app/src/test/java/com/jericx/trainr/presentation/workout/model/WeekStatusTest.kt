package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.StatusCompleted
import com.jericx.trainr.presentation.common.theme.StatusInProgress
import com.jericx.trainr.presentation.common.theme.StatusNotStarted
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class WeekStatusTest {

    private lateinit var originalTimeZone: TimeZone

    // Pinned so weekday assertions don't depend on the machine's zone.
    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun eachStatusHasItsOwnLabel() {
        val labels = WeekStatus.entries.map { it.labelRes }

        assertThat(labels).containsExactly(
            R.string.completed,
            R.string.in_progress,
            R.string.not_completed,
            R.string.skipped,
            R.string.upcoming
        )
        assertThat(labels.toSet()).hasSize(WeekStatus.entries.size)
    }

    // A missed week and a part-done week read the same in the design.
    @Test
    fun missedAndPartDoneWeeksShareAColour() {
        assertThat(WeekStatus.NOT_COMPLETED.chipColor).isEqualTo(StatusNotStarted)
        assertThat(WeekStatus.SKIPPED.chipColor).isEqualTo(StatusNotStarted)
        assertThat(WeekStatus.UPCOMING.chipColor).isEqualTo(StatusNotStarted)
        assertThat(WeekStatus.COMPLETED.chipColor).isEqualTo(StatusCompleted)
        assertThat(WeekStatus.IN_PROGRESS.chipColor).isEqualTo(StatusInProgress)
    }

    @Test
    fun sampleWeeksCoverEveryStatus() {
        val statuses = SampleWeeklyProgress.weeks.map { it.status }.toSet()

        assertThat(statuses).containsExactlyElementsIn(WeekStatus.entries)
    }

    @Test
    fun completedDaysNeverExceedTheTotal() {
        SampleWeeklyProgress.weeks.forEach { week ->
            assertThat(week.completedDays).isAtMost(week.totalDays)
            assertThat(week.completionPercentage).isIn(0..100)
        }
    }

    @Test
    fun percentageIsTheShareOfDaysCompleted() {
        assertThat(WeekProgressUi(1, 3, 3, WeekStatus.COMPLETED, startDateMillis = 0L, endDateMillis = 0L).completionPercentage).isEqualTo(100)
        assertThat(WeekProgressUi(1, 2, 3, WeekStatus.NOT_COMPLETED, startDateMillis = 0L, endDateMillis = 0L).completionPercentage).isEqualTo(67)
        assertThat(WeekProgressUi(1, 1, 3, WeekStatus.IN_PROGRESS, startDateMillis = 0L, endDateMillis = 0L).completionPercentage).isEqualTo(33)
        assertThat(WeekProgressUi(1, 0, 3, WeekStatus.SKIPPED, startDateMillis = 0L, endDateMillis = 0L).completionPercentage).isEqualTo(0)
    }

    // A plan with no scheduled days must not divide by zero.
    @Test
    fun percentageIsZeroWhenNoDaysAreScheduled() {
        assertThat(WeekProgressUi(1, 0, 0, WeekStatus.SKIPPED, startDateMillis = 0L, endDateMillis = 0L).completionPercentage).isEqualTo(0)
    }

    // The mockups label week 1 "Jul 22 – 28", but 22 July 2025 is a Tuesday, and
    // the plan lists Monday first. A Tuesday-aligned week would put Monday's
    // workout at the end of the week, so the weeks are Monday-aligned here.
    @Test
    fun everyWeekRunsMondayToSunday() {
        SampleWeeklyProgress.weeks.forEach { week ->
            val start = WorkoutDateFormatter.formatWeekday(week.startDateMillis, Locale.US)
            val end = WorkoutDateFormatter.formatWeekday(week.endDateMillis, Locale.US)

            assertThat(start).isEqualTo("Monday")
            assertThat(end).isEqualTo("Sunday")
        }
    }

    @Test
    fun weeksRunConsecutivelyAndAreSevenDaysApart() {
        val millisPerWeek = 7L * 24 * 60 * 60 * 1000
        val starts = SampleWeeklyProgress.weeks.map { it.startDateMillis }

        starts.zipWithNext().forEach { (a, b) ->
            assertThat(b - a).isEqualTo(millisPerWeek)
        }
    }
}
