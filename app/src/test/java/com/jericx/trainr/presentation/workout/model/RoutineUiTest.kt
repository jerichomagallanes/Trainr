package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.unstuck.ActualOrigin
import org.junit.Test

class RoutineUiTest {

    private fun exercise(position: Int, minutes: Int = 5, isCompleted: Boolean = false) = ExerciseUi(
        position = position,
        name = "Exercise $position",
        description = "Description $position",
        minutes = minutes,
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

    private fun RoutineUi.tickEverySetOf(position: Int): RoutineUi =
        exercises.first { it.position == position }.sets
            .fold(this) { routine, set -> routine.updateSet(position, set.copy(isCompleted = true)) }

    @Test
    fun tickingEverySetFinishesTheExercise() {
        val routine = routineWithSets()

        val partly = routine.updateSet(1, routine.exercises.first().sets.first().copy(isCompleted = true))
        assertThat(partly.exercises.first().isCompleted).isFalse()

        assertThat(routine.tickEverySetOf(1).exercises.first().isCompleted).isTrue()
    }

    @Test
    fun clearingASetReopensTheExercise() {
        val done = routineWithSets().tickEverySetOf(1)

        val reopened = done.updateSet(1, done.exercises.first().sets.last().copy(isCompleted = false))

        assertThat(reopened.exercises.first().isCompleted).isFalse()
    }

    @Test
    fun addingASetReopensAFinishedExercise() {
        val done = routineWithSets().tickEverySetOf(1)

        assertThat(done.addSet(1).exercises.first().isCompleted).isFalse()
    }

    @Test
    fun removingTheOnlySetLeftOutstandingFinishesTheExercise() {
        val routine = routineWithSets().tickEverySetOf(1).addSet(1)

        val trimmed = routine.removeSet(1, routine.exercises.first().sets.size)

        assertThat(trimmed.exercises.first().isCompleted).isTrue()
    }

    @Test
    fun unTickingAnExerciseClearsItsMarksAndKeepsItsNumbers() {
        val logged = routineWithSets()
            .updateSet(1, ExerciseSet(setNumber = 1, targetReps = 12, targetWeightKg = 20f, actualReps = 9))
            .markCompleted(1)

        val reopened = logged.toggleCompleted(1).exercises.first()

        assertThat(reopened.isCompleted).isFalse()
        assertThat(reopened.sets.none { it.isCompleted }).isTrue()
        assertThat(reopened.sets.first().actualReps).isEqualTo(9)
    }

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
    fun deletingASetRenumbersTheRest() {
        val routine = routineWithSets()
            .updateSet(1, ExerciseSet(setNumber = 3, targetReps = 12, targetWeightKg = 20f, actualReps = 9))
        val victim = routine.exercises.first().sets[1]

        val sets = routine.removeSet(1, victim.setNumber).exercises.first().sets

        assertThat(sets.map { it.setNumber }).containsExactly(1, 2).inOrder()
        assertThat(sets.last().actualReps).isEqualTo(9)
    }

    // Deletion is keyed by set number, so a reload that replaced every set instance cannot strand a swipe
    @Test
    fun deletingAMissingNumberRemovesNothing() {
        val routine = routineWithSets()
        val once = routine.removeSet(1, 3)

        assertThat(once.removeSet(1, 3)).isEqualTo(once)
        assertThat(once.exercises.first().sets).hasSize(2)
    }

    @Test
    fun theLastSetCanBeDeletedToo() {
        val routine = routineWithSets()
        val only = routine.exercises[1].sets.single()

        val emptied = routine.removeSet(2, only.setNumber)

        assertThat(emptied.exercises[1].sets).isEmpty()
    }

    @Test
    fun completingAllTicksEveryExercise() {
        val completed = routineOf(true, false, false).completeAll()

        assertThat(completed.completionPercentage).isEqualTo(100)
        assertThat(completed.isComplete).isTrue()
    }
    // Completing says the prescription was done; blank sets would reach next week's prompt as skipped
    @Test
    fun completingRecordsThePrescriptionOnBlankSets() {
        val completed = routineWithSets().completeAll()

        val sets = completed.exercises.first().sets
        assertThat(sets.map { it.actualReps }).isEqualTo(sets.map { it.targetReps })
        assertThat(sets.map { it.actualWeightKg }).isEqualTo(sets.map { it.targetWeightKg })
        assertThat(sets.all { it.isCompleted }).isTrue()
    }

    @Test
    fun completingKeepsTheNumbersThatWereLogged() {
        val routine = routineWithSets()
        val logged = routine.exercises.first().sets.first()
            .copy(actualReps = 6, actualWeightKg = 30f)
        val completed = routine.updateSet(1, logged).completeAll()

        with(completed.exercises.first().sets.first()) {
            assertThat(actualReps).isEqualTo(6)
            assertThat(actualWeightKg).isEqualTo(30f)
        }
    }

    @Test
    fun unTickingAnExerciseLeavesItsNumbersAlone() {
        val ticked = routineWithSets().toggleCompleted(1)
        val unticked = ticked.toggleCompleted(1)

        assertThat(unticked.exercises.first().isCompleted).isFalse()
        assertThat(unticked.exercises.first().sets.map { it.actualReps })
            .isEqualTo(ticked.exercises.first().sets.map { it.actualReps })
    }


    // Logging only ever writes actuals, so clearing must leave the targets the plan set
    @Test
    fun `clearing progress keeps the prescription and drops the log`() {
        val done = routineWithSets().completeAll()
        val before = done.exercises.first().sets.first()
        assertThat(before.isCompleted).isTrue()
        assertThat(before.actualReps).isNotNull()

        val cleared = done.clearProgress()
        val after = cleared.exercises.first().sets.first()

        assertThat(after.isCompleted).isFalse()
        assertThat(after.actualReps).isNull()
        assertThat(after.actualWeightKg).isNull()
        assertThat(after.actualSeconds).isNull()
        assertThat(after.targetReps).isEqualTo(before.targetReps)
        assertThat(after.targetWeightKg).isEqualTo(before.targetWeightKg)
        assertThat(cleared.exercises.none { it.isCompleted }).isTrue()
        assertThat(cleared.isComplete).isFalse()
    }

    @Test
    fun `an untouched routine has no progress to clear`() {
        assertThat(routineWithSets().hasProgress).isFalse()
        assertThat(routineWithSets().completeAll().hasProgress).isTrue()
        assertThat(routineWithSets().completeAll().clearProgress().hasProgress).isFalse()
    }

    private fun RoutineUi.firstSet() = exercises.first().sets.first()

    private fun RoutineUi.origins() = exercises.first().sets.map { it.actualOrigin }

    @Test
    fun typingANumberMarksTheSetAsTyped() {
        val routine = routineWithSets()

        val typed = routine.updateSet(1, routine.firstSet().copy(actualReps = 9))

        assertThat(typed.origins())
            .containsExactly(ActualOrigin.TYPED, ActualOrigin.NONE, ActualOrigin.NONE)
            .inOrder()
    }

    @Test
    fun theCheckmarkConfirmsTheTargetsOnBlankSets() {
        val ticked = routineWithSets().toggleCompleted(1)

        assertThat(ticked.origins().distinct()).containsExactly(ActualOrigin.CONFIRMED_TARGET)
        assertThat(ticked.exercises.first().sets.map { it.actualReps }).containsExactly(12, 12, 12)
    }

    @Test
    fun aTypedSetKeepsItsOriginThroughTheCheckmark() {
        val routine = routineWithSets()
        val typed = routine.updateSet(1, routine.firstSet().copy(actualReps = 9))

        val ticked = typed.toggleCompleted(1)

        assertThat(ticked.origins())
            .containsExactly(
                ActualOrigin.TYPED,
                ActualOrigin.CONFIRMED_TARGET,
                ActualOrigin.CONFIRMED_TARGET
            )
            .inOrder()
        assertThat(ticked.firstSet().actualReps).isEqualTo(9)
    }

    @Test
    fun tickingWithoutChangingANumberKeepsTheOrigin() {
        val confirmed = routineWithSets().toggleCompleted(1)

        val unticked = confirmed.updateSet(1, confirmed.firstSet().copy(isCompleted = false))

        assertThat(unticked.origins().first()).isEqualTo(ActualOrigin.CONFIRMED_TARGET)
    }

    @Test
    fun blankingEveryNumberLeavesNoOrigin() {
        val routine = routineWithSets()
        val typed = routine.updateSet(1, routine.firstSet().copy(actualReps = 9))

        val blanked = typed.updateSet(1, routine.firstSet())

        assertThat(blanked.origins().first()).isEqualTo(ActualOrigin.NONE)
    }

    @Test
    fun startingOverClearsEveryOrigin() {
        val routine = routineWithSets()
        val logged = routine
            .updateSet(1, routine.firstSet().copy(actualReps = 9))
            .toggleCompleted(1)

        val cleared = logged.clearProgress()

        assertThat(cleared.origins().distinct()).containsExactly(ActualOrigin.NONE)
    }

    @Test
    fun untickingLeavesTheNumbersAndTheirOrigin() {
        val unticked = routineWithSets().toggleCompleted(1).toggleCompleted(1)

        assertThat(unticked.origins().distinct()).containsExactly(ActualOrigin.CONFIRMED_TARGET)
        assertThat(unticked.exercises.first().sets.map { it.actualReps }).containsExactly(12, 12, 12)
    }

    @Test
    fun aTypedNumberSurvivesAnUntickAndRetick() {
        val routine = routineWithSets()
        val cycled = routine
            .updateSet(1, routine.firstSet().copy(actualReps = 9))
            .toggleCompleted(1)
            .toggleCompleted(1)
            .toggleCompleted(1)

        assertThat(cycled.origins())
            .containsExactly(
                ActualOrigin.TYPED,
                ActualOrigin.CONFIRMED_TARGET,
                ActualOrigin.CONFIRMED_TARGET
            )
            .inOrder()
        assertThat(cycled.firstSet().actualReps).isEqualTo(9)
    }

    @Test
    fun exerciseCountsFollowThePlannedSets() {
        val planned = listOf(ExerciseSet(setNumber = 1, targetReps = 12))
        val omitted = planned.map { it.copy(omittedBy = 4L) }
        val routine = RoutineUi(
            title = "Strength",
            exercises = listOf(
                exercise(1).copy(sets = planned),
                exercise(2).copy(sets = omitted),
                exercise(3).copy(sets = planned)
            )
        ).toggleCompleted(1)

        assertThat(routine.plannedExerciseCount).isEqualTo(2)
        assertThat(routine.performedExerciseCount).isEqualTo(1)
    }
}
