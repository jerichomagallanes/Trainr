package com.jericx.trainr.domain.catalog

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.FitnessGoal
import org.junit.Test

class ExerciseShortlistTest {

    private fun exercise(key: String, pattern: MovementPattern) = CatalogExercise(
        key = key,
        name = key,
        primary = MuscleGroup.CHEST,
        secondary = emptyList(),
        equipment = Equipment.NONE,
        measure = ExerciseMeasure.REPS,
        pattern = pattern,
        staple = false
    )

    private val full = listOf(
        exercise("bodyweight_squat", MovementPattern.SQUAT),
        exercise("push_up", MovementPattern.HORIZONTAL_PUSH),
        exercise("pull_up", MovementPattern.VERTICAL_PULL)
    )

    @Test
    fun onlyPatternsTheShortlistCanSatisfyAreRequired() {
        val pushOnly = listOf(exercise("push_up", MovementPattern.HORIZONTAL_PUSH))

        assertThat(ExerciseShortlist.requiredPatterns(pushOnly))
            .containsExactly(PatternRequirement.UPPER_PUSH)
    }

    @Test
    fun aFullShortlistDemandsAllThreePatterns() {
        assertThat(ExerciseShortlist.requiredPatterns(full))
            .containsExactly(
                PatternRequirement.LOWER_PUSH,
                PatternRequirement.UPPER_PUSH,
                PatternRequirement.UPPER_PULL
            )
    }

    // Someone who came for mobility should not have their week rejected for
    // holding no squat.
    @Test
    fun aFlexibilityGoalIsNotHeldToTheSquatPressPullRule() {
        assertThat(ExerciseShortlist.requiredPatterns(full, FitnessGoal.MUSCLE_GAIN)).isNotEmpty()
        assertThat(ExerciseShortlist.requiredPatterns(full, FitnessGoal.FLEXIBILITY)).isEmpty()
    }
}
