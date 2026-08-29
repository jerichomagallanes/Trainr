package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.StatusCompleted
import com.jericx.trainr.presentation.common.theme.StatusInProgress
import com.jericx.trainr.presentation.common.theme.StatusNotStarted
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import org.junit.Test

class WeekStatusTest {

    @Test
    fun eachStatusHasItsOwnLabel() {
        val labels = WeekStatus.entries.map { it.labelRes }

        assertThat(labels).containsExactly(
            R.string.completed,
            R.string.in_progress,
            R.string.not_completed,
            R.string.skipped
        )
        assertThat(labels.toSet()).hasSize(WeekStatus.entries.size)
    }

    // A missed week and a part-done week read the same in the design.
    @Test
    fun missedAndPartDoneWeeksShareAColour() {
        assertThat(WeekStatus.NOT_COMPLETED.chipColor).isEqualTo(StatusNotStarted)
        assertThat(WeekStatus.SKIPPED.chipColor).isEqualTo(StatusNotStarted)
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
    fun weeksRunConsecutivelyAndAreSevenDaysApart() {
        val millisPerWeek = 7L * 24 * 60 * 60 * 1000
        val starts = SampleWeeklyProgress.weeks.map { SampleWeeklyProgress.weekStartMillis(it.weekNumber) }

        starts.zipWithNext().forEach { (a, b) ->
            assertThat(b - a).isEqualTo(millisPerWeek)
        }
    }
}
