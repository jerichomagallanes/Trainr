package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import org.junit.Test

class SessionMinutesTest {

    // Three sets of ten at three seconds a rep is 90 seconds of work, and two
    // rests of a minute between them.
    @Test
    fun anExerciseIsItsWorkPlusTheRestsBetweenItsSets() {
        val minutes = SessionMinutes.forExercise(
            measure = ExerciseMeasure.WEIGHT_AND_REPS,
            perSet = listOf(10, 10, 10),
            restSeconds = 60
        )

        assertThat(minutes).isEqualTo(4)
    }

    // Reps on one side are done again on the other, so the set costs twice the
    // time even though the number of reps written down does not change.
    @Test
    fun aMovementWorkedOneSideAtATimeCostsTwiceTheRepTime() {
        val perSet = listOf(10, 10, 10)

        val bothSides = SessionMinutes.forExercise(
            ExerciseMeasure.WEIGHT_AND_REPS, perSet, restSeconds = 60, unilateral = true
        )
        val oneSide = SessionMinutes.forExercise(
            ExerciseMeasure.WEIGHT_AND_REPS, perSet, restSeconds = 60, unilateral = false
        )

        assertThat(bothSides).isGreaterThan(oneSide)
    }

    // A hold is already the whole set: a side plank does not become a
    // two-minute plank because it is per side.
    @Test
    fun aTimedHoldIsNotDoubledForBeingPerSide() {
        val held = SessionMinutes.forExercise(
            ExerciseMeasure.DURATION, listOf(60, 60), restSeconds = 30, unilateral = true
        )
        val plain = SessionMinutes.forExercise(
            ExerciseMeasure.DURATION, listOf(60, 60), restSeconds = 30, unilateral = false
        )

        assertThat(held).isEqualTo(plain)
    }

    // Walking to the next station is a minute the client spends whether the
    // plan counts it or not.
    @Test
    fun aDayChargesTheWalkBetweenItsExercises() {
        val day = SessionMinutes.forDay(listOf(5, 5, 5))

        assertThat(day).isEqualTo(5 + 5 + 5 + 2)
    }

    @Test
    fun aDayOfOneExerciseChargesNoTransition() {
        assertThat(SessionMinutes.forDay(listOf(7))).isEqualTo(7)
        assertThat(SessionMinutes.forDay(emptyList())).isEqualTo(0)
    }

    // Even a single short set is a minute of the client's time.
    @Test
    fun nothingIsEverFreeEvenWhenTheArithmeticRoundsToZero() {
        val minutes = SessionMinutes.forExercise(
            ExerciseMeasure.REPS, listOf(1), restSeconds = 0
        )

        assertThat(minutes).isEqualTo(1)
    }
}
