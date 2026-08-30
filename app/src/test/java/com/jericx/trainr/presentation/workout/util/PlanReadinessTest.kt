package com.jericx.trainr.presentation.workout.util

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import org.junit.Test
import java.util.concurrent.TimeUnit

class PlanReadinessTest {

    private val start = WorkoutWeek.startOfDay() - TimeUnit.DAYS.toMillis(2)

    private fun week(vararg statuses: WorkoutStatus) = WeeklyWorkoutPlan(
        userId = 1,
        weekNumber = 1,
        title = "Foundation",
        startDateMillis = start,
        workoutDays = statuses.mapIndexed { index, status ->
            WorkoutDay(
                dayNumber = index + 1,
                title = "Day ${index + 1}",
                status = status,
                duration = 45,
                exerciseCount = 1,
                equipment = emptyList()
            )
        }
    )

    @Test
    fun aFinishedWeekIsReadyForTheNext() {
        val finished = week(WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED)

        assertThat(finished.isReadyForTheNextWeek()).isTrue()
    }

    // The week still being trained is the week that is yours. Another one now
    // would become the newest and quietly take that title from it.
    @Test
    fun aWeekStillBeingTrainedIsNot() {
        val started = week(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)

        assertThat(started.isReadyForTheNextWeek()).isFalse()
    }

    // Missed days do not strand the plan: once the dates have run out the week
    // is not coming back, finished or not.
    @Test
    fun aWeekWhoseDatesHaveRunOutIsReadyEvenWithDaysMissed() {
        val missed = week(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)
        val eightDaysOn = WorkoutWeek.dateOfDay(start, 8)

        assertThat(missed.isReadyForTheNextWeek(nowMillis = eightDaysOn)).isTrue()
    }

    @Test
    fun aWeekWithNoDaysIsNotFinishedByBeingEmpty() {
        assertThat(week().isReadyForTheNextWeek()).isFalse()
    }
}
