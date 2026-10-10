package com.jericx.trainr.presentation.workout.util

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
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
                equipment = emptyList(),
                exercises = listOf(
                    WorkoutExercise(
                        name = "Movement",
                        isCompleted = status == WorkoutStatus.COMPLETED
                    )
                )
            )
        }
    )

    private fun set(number: Int, done: Boolean) =
        ExerciseSet(setNumber = number, isCompleted = done)

    private fun WeeklyWorkoutPlan.finishedEarlyWithNothingDone() = copy(
        workoutDays = workoutDays.map { day ->
            day.copy(exercises = day.exercises.map { it.copy(isCompleted = false) })
        }
    )

    @Test
    fun aFinishedWeekIsReadyForTheNext() {
        val finished = week(WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED)

        assertThat(finished.isReadyForTheNextWeek()).isTrue()
    }

    // Another week now would become the newest and take the title of current from the one being trained
    @Test
    fun aWeekStillBeingTrainedIsNot() {
        val started = week(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)

        assertThat(started.isReadyForTheNextWeek()).isFalse()
    }

    // Missed days must not strand the plan: once the dates have run out the week is not coming back
    @Test
    fun aWeekWhoseDatesHaveRunOutIsReadyEvenWithDaysMissed() {
        val missed = week(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)
        val eightDaysOn = WorkoutWeek.dateOfDay(start, 8)

        assertThat(missed.isReadyForTheNextWeek(nowMillis = eightDaysOn)).isTrue()
    }

    // Finishing every day early and logging nothing is not a week of training,
    // so it must not unlock the next one before the dates run out.
    @Test
    fun aWeekClosedWithNothingPerformedIsNotReady() {
        val closed = week(WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED)
            .finishedEarlyWithNothingDone()

        assertThat(closed.isReadyForTheNextWeek()).isFalse()
    }

    // Two sets of three is work done, and the exercise never ticks itself until
    // all of them are, so the day must not read as untrained.
    @Test
    fun aWeekClosedEarlyWithSetsLoggedIsReady() {
        val closed = week(WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED)
            .finishedEarlyWithNothingDone()
        val partlyTrained = closed.copy(
            workoutDays = closed.workoutDays.map { day ->
                day.copy(
                    exercises = day.exercises.map {
                        it.copy(sets = listOf(set(1, done = true), set(2, done = false)))
                    }
                )
            }
        )

        assertThat(partlyTrained.isReadyForTheNextWeek()).isTrue()
    }

    @Test
    fun aWeekWithNoDaysIsNotFinishedByBeingEmpty() {
        assertThat(week().isReadyForTheNextWeek()).isFalse()
    }
}
