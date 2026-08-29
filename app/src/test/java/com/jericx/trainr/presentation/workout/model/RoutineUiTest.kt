package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
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

    private fun routineWithSets() = RoutineUi(
        title = "Full Body Strength",
        exercises = listOf(
            exercise(1).copy(
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12, targetWeightKg = 20f) }
            ),
            exercise(2).copy(sets = listOf(ExerciseSet(setNumber = 1, targetReps = 20)))
        )
    )

    @Test
    fun loggingASetTouchesOnlyThatSetOfThatExercise() {
        val logged = routineWithSets()
            .updateSet(1, ExerciseSet(setNumber = 2, targetReps = 12, targetWeightKg = 20f, actualReps = 9, actualWeightKg = 22.5f, isCompleted = true))

        val first = logged.exercises.first()
        assertThat(first.sets[0].actualReps).isNull()
        assertThat(first.sets[1].actualReps).isEqualTo(9)
        assertThat(first.sets[1].actualWeightKg).isEqualTo(22.5f)
        assertThat(first.sets[1].isCompleted).isTrue()
        assertThat(first.sets[2].actualReps).isNull()
        assertThat(logged.exercises[1].sets.single().actualReps).isNull()
    }

    // A new set repeats the last target: the likeliest next thing is what you just did.
    @Test
    fun addingASetContinuesTheLastTarget() {
        val grown = routineWithSets().addSet(1).exercises.first()

        assertThat(grown.sets).hasSize(4)
        with(grown.sets.last()) {
            assertThat(setNumber).isEqualTo(4)
            assertThat(targetReps).isEqualTo(12)
            assertThat(targetWeightKg).isEqualTo(20f)
            assertThat(actualReps).isNull()
            assertThat(isCompleted).isFalse()
        }
    }

    @Test
    fun addingASetToAnExerciseWithNoneStartsAtOne() {
        val routine = RoutineUi(title = "New", exercises = listOf(exercise(1)))

        val grown = routine.addSet(1).exercises.single()

        assertThat(grown.sets.single().setNumber).isEqualTo(1)
    }

    @Test
    fun editingAnUnknownExerciseChangesNothing() {
        val routine = routineWithSets()

        assertThat(routine.addSet(99)).isEqualTo(routine)
        assertThat(routine.updateSet(99, ExerciseSet(setNumber = 1))).isEqualTo(routine)
    }

    @Test
    fun completingAllTicksEveryExercise() {
        val completed = routineOf(true, false, false).completeAll()

        assertThat(completed.completionPercentage).isEqualTo(100)
        assertThat(completed.isComplete).isTrue()
    }
}
