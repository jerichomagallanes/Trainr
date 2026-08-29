package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.StatusCompleted
import org.junit.Test

class ExerciseStepTest {

    private fun routineOf(vararg completed: Boolean) = RoutineUi(
        title = "Cardio & Core",
        exercises = completed.mapIndexed { index, done ->
            ExerciseUi(
                position = index + 1,
                name = "Exercise ${index + 1}",
                description = "Description",
                minutes = 5,
                detail = "5 minutes",
                isCompleted = done
            )
        }
    )

    @Test
    fun marksWhereYouAreAsTheCurrentStep() {
        val steps = routineOf(true, true, false, false, false).stepsFor(currentPosition = 3)

        assertThat(steps).containsExactly(
            ExerciseStep.COMPLETED,
            ExerciseStep.COMPLETED,
            ExerciseStep.CURRENT,
            ExerciseStep.UPCOMING,
            ExerciseStep.UPCOMING
        ).inOrder()
    }

    // Going back to an exercise you already ticked should still read as "here".
    @Test
    fun theCurrentStepWinsOverBeingCompleted() {
        val steps = routineOf(true, true, false).stepsFor(currentPosition = 1)

        assertThat(steps.first()).isEqualTo(ExerciseStep.CURRENT)
    }

    @Test
    fun anExerciseTickedOutOfOrderStillReadsAsCompleted() {
        val steps = routineOf(false, false, true).stepsFor(currentPosition = 1)

        assertThat(steps).containsExactly(
            ExerciseStep.CURRENT,
            ExerciseStep.UPCOMING,
            ExerciseStep.COMPLETED
        ).inOrder()
    }

    @Test
    fun aPositionThatIsNotInTheRoutineLeavesNoCurrentStep() {
        val steps = routineOf(true, false, false).stepsFor(currentPosition = 99)

        assertThat(steps).doesNotContain(ExerciseStep.CURRENT)
    }

    @Test
    fun anEmptyRoutineHasNoSteps() {
        assertThat(RoutineUi(title = "Empty", exercises = emptyList()).stepsFor(1)).isEmpty()
    }

    @Test
    fun eachStateKeepsItsFigmaColour() {
        assertThat(ExerciseStep.COMPLETED.color).isEqualTo(StatusCompleted)
        assertThat(ExerciseStep.CURRENT.color).isEqualTo(Slate800)
        assertThat(ExerciseStep.UPCOMING.color).isEqualTo(OutlineGray)
    }
}
