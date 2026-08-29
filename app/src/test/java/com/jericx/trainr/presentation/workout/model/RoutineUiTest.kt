package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoutineUiTest {

    private fun exercise(position: Int, minutes: Int = 5, isCompleted: Boolean = false) = ExerciseUi(
        position = position,
        name = "Exercise $position",
        description = "Description $position",
        minutes = minutes,
        detail = "$minutes minutes",
        isCompleted = isCompleted
    )

    private fun routineOf(vararg completed: Boolean) = RoutineUi(
        title = "Cardio & Core",
        exercises = completed.mapIndexed { index, done -> exercise(index + 1, isCompleted = done) }
    )

    @Test
    fun completionIsTheShareOfExercisesTicked() {
        assertThat(routineOf(true, false, false, false).completionPercentage).isEqualTo(25)
    }

    @Test
    fun completionRoundsToTheNearestPercent() {
        assertThat(routineOf(true, false, false).completionPercentage).isEqualTo(33)
    }

    @Test
    fun anUntouchedRoutineIsZeroPercent() {
        assertThat(routineOf(false, false, false).completionPercentage).isEqualTo(0)
    }

    @Test
    fun anEmptyRoutineIsZeroPercentRatherThanADivisionByZero() {
        assertThat(RoutineUi(title = "Empty", exercises = emptyList()).completionPercentage).isEqualTo(0)
    }

    @Test
    fun anEmptyRoutineIsNotComplete() {
        assertThat(RoutineUi(title = "Empty", exercises = emptyList()).isComplete).isFalse()
    }

    @Test
    fun aRoutineIsCompleteOnlyWhenEveryExerciseIs() {
        assertThat(routineOf(true, true, false).isComplete).isFalse()
        assertThat(routineOf(true, true, true).isComplete).isTrue()
    }

    @Test
    fun totalMinutesSumTheExercises() {
        val routine = RoutineUi(
            title = "Cardio & Core",
            exercises = listOf(exercise(1, minutes = 5), exercise(2, minutes = 10))
        )

        assertThat(routine.totalMinutes).isEqualTo(15)
    }

    @Test
    fun togglingFlipsOnlyTheExerciseAtThatPosition() {
        val toggled = routineOf(false, false, false).toggleCompleted(2)

        assertThat(toggled.exercises.map { it.isCompleted })
            .containsExactly(false, true, false).inOrder()
    }

    @Test
    fun togglingACompletedExerciseUnticksIt() {
        val toggled = routineOf(true, false, false).toggleCompleted(1)

        assertThat(toggled.completedCount).isEqualTo(0)
    }

    @Test
    fun togglingAnUnknownPositionChangesNothing() {
        val routine = routineOf(true, false, false)

        assertThat(routine.toggleCompleted(99)).isEqualTo(routine)
    }

    @Test
    fun completingAllTicksEveryExercise() {
        val completed = routineOf(true, false, false).completeAll()

        assertThat(completed.completionPercentage).isEqualTo(100)
        assertThat(completed.isComplete).isTrue()
    }
}
