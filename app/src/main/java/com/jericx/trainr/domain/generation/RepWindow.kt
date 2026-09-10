package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.isLowerBody
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile

// How many reps a set is for. Keyed on the goal AND on what the movement is
// for: a goal-only table is what produces three sets of ten on deadlifts,
// planks and calf raises alike.
object RepWindow {

    fun forExercise(user: UserProfile, exercise: CatalogExercise): IntRange {
        val window = when (goalRowFor(user)) {
            FitnessGoal.STRENGTH -> if (exercise.role == ExerciseRole.COMPOUND) 3..6 else 6..10
            FitnessGoal.MUSCLE_GAIN -> if (exercise.role == ExerciseRole.COMPOUND) 6..10 else 8..15
            FitnessGoal.GENERAL_FITNESS ->
                if (exercise.role == ExerciseRole.COMPOUND) 8..12 else 10..15
            FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE ->
                if (exercise.role == ExerciseRole.COMPOUND) 12..20 else 15..25
            FitnessGoal.FLEXIBILITY -> 10..15
        }
        return window.widenedFor(user)
    }

    fun holdSeconds(user: UserProfile): IntRange = when (user.fitnessGoal) {
        FitnessGoal.FLEXIBILITY -> 45..60
        FitnessGoal.STRENGTH, FitnessGoal.MUSCLE_GAIN -> 30..45
        else -> 30..60
    }

    // How much load a successful week adds. Bigger muscles tolerate a bigger
    // jump than a lateral raise does.
    fun loadStepFraction(user: UserProfile, exercise: CatalogExercise): Float {
        val base = when {
            exercise.role != ExerciseRole.COMPOUND -> 0.025f
            exercise.isLowerBody -> 0.050f
            else -> 0.035f
        }
        return if (user.age >= OLDER_ADULT_AGE) minOf(base, CAUTIOUS_STEP) else base
    }

    // A beginner chasing strength is not put on triples: technique before
    // load, and the general row is where that lives.
    private fun goalRowFor(user: UserProfile): FitnessGoal =
        if (user.fitnessGoal == FitnessGoal.STRENGTH &&
            user.experienceLevel == com.jericx.trainr.domain.model.ExperienceLevel.BEGINNER
        ) {
            FitnessGoal.GENERAL_FITNESS
        } else {
            user.fitnessGoal
        }

    // Older adults and under-18s work further from a maximum, which is more
    // reps of a lighter weight rather than a different exercise.
    private fun IntRange.widenedFor(user: UserProfile): IntRange = when {
        user.age >= OLDER_ADULT_AGE -> (first + 2)..(last + 2)
        user.age in 1 until MINOR_AGE -> maxOf(first, 8)..maxOf(last, 12)
        else -> this
    }

    private const val OLDER_ADULT_AGE = 65
    private const val MINOR_AGE = 18
    private const val CAUTIOUS_STEP = 0.025f
}
