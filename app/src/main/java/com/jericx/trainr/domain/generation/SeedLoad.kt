package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.catalog.isLoadable
import com.jericx.trainr.domain.catalog.isLowerBody
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import kotlin.math.max
import kotlin.math.min

// A first guess at a starting weight. No validated equation predicts one from
// bodyweight, age and sex (Reynolds 2006); the validated route is a performed
// rep maximum, which is what week one then is. So this table is an engineering
// heuristic that errs light, the plan labels it an estimate, and the first
// logged session corrects it.
object SeedLoad {

    const val MOBILITY_SECONDS = 60
    const val WARM_UP_SECONDS = 300

    // A ten-rep maximum for the whole load, calibrated at an intermediate man.
    fun tenRepMaxKg(user: UserProfile, exercise: CatalogExercise): Float? {
        if (!exercise.isLoadable) return null
        val bodyweight = user.weight.takeIf { it > 0f } ?: FALLBACK_BODYWEIGHT_KG
        return bodyweight * coefficient(exercise) * muscleFactor(exercise) *
            sexFactor(user, exercise) * ageFactor(user) * experienceFactor(user)
    }

    // Epley, both ways, clamped where it stops being trustworthy. Without it a
    // strength week one is far too light and an endurance one too heavy.
    fun atReps(tenRepMaxKg: Float, reps: Int): Float {
        val oneRepMax = tenRepMaxKg * (1 + TEN_REPS / EPLEY_DIVISOR)
        return oneRepMax / (1 + min(reps, EPLEY_CEILING_REPS) / EPLEY_DIVISOR)
    }

    // What goes in one hand, unsnapped. The table is total load; weightKg is
    // one bell, so a movement done with a pair is half the total per bell.
    fun loadKg(user: UserProfile, exercise: CatalogExercise, reps: Int): Float? {
        val total = tenRepMaxKg(user, exercise)?.let { atReps(it, reps) } ?: return null
        return if (exercise.equipment == Equipment.DUMBBELL && !exercise.oneHanded) total / 2 else total
    }

    fun holdSeconds(user: UserProfile): Int = when (user.experienceLevel) {
        ExperienceLevel.BEGINNER -> 30
        ExperienceLevel.INTERMEDIATE -> 40
        ExperienceLevel.ADVANCED -> 45
    }

    fun conditioningSeconds(user: UserProfile): Int = when (user.fitnessGoal) {
        FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> 900
        FitnessGoal.GENERAL_FITNESS -> 600
        else -> 480
    }

    private fun coefficient(exercise: CatalogExercise): Float {
        val row = when (exercise.equipment) {
            Equipment.BARBELL -> Barbell
            Equipment.DUMBBELL -> Dumbbell
            Equipment.MACHINE -> Machine
            Equipment.KETTLEBELL -> Kettlebell
            Equipment.PLATE -> Plate
            else -> Bodyweight
        }
        return row[exercise.pattern] ?: row.getValue(null)
    }

    // (equipment, pattern) is three to five times wrong for a few families: a
    // calf press is not a lateral raise.
    private fun muscleFactor(exercise: CatalogExercise): Float {
        val isolation = exercise.pattern == MovementPattern.ISOLATION
        return when {
            exercise.primary == MuscleGroup.CALVES -> 2.5f
            exercise.primary == MuscleGroup.SHOULDERS && isolation -> 0.35f
            exercise.primary == MuscleGroup.HAMSTRINGS && isolation -> 1.5f
            exercise.primary == MuscleGroup.QUADRICEPS && isolation -> 2.0f
            exercise.primary in DirectArm -> 0.7f
            exercise.primary in LoadedCore -> 0.6f
            else -> 1f
        }
    }

    // Erring light is corrected within a session; erring heavy is an injury.
    private fun sexFactor(user: UserProfile, exercise: CatalogExercise): Float = when {
        user.gender == Gender.MALE -> 1f
        exercise.isLowerBody -> 0.70f
        else -> 0.55f
    }

    private fun ageFactor(user: UserProfile): Float = when {
        user.age in 1 until MINOR_AGE -> 0.80f
        else -> max(0.60f, 1f - 0.01f * max(0, user.age - 40))
    }

    private fun experienceFactor(user: UserProfile): Float = when (user.experienceLevel) {
        ExperienceLevel.BEGINNER -> 0.65f
        ExperienceLevel.INTERMEDIATE -> 1f
        ExperienceLevel.ADVANCED -> 1.3f
    }

    private fun row(
        squat: Float, hinge: Float, lunge: Float, horizontalPush: Float, verticalPush: Float,
        horizontalPull: Float, verticalPull: Float, isolation: Float, core: Float, other: Float
    ): Map<MovementPattern?, Float> = mapOf(
        MovementPattern.SQUAT to squat, MovementPattern.HINGE to hinge,
        MovementPattern.LUNGE to lunge, MovementPattern.HORIZONTAL_PUSH to horizontalPush,
        MovementPattern.VERTICAL_PUSH to verticalPush,
        MovementPattern.HORIZONTAL_PULL to horizontalPull,
        MovementPattern.VERTICAL_PULL to verticalPull, MovementPattern.ISOLATION to isolation,
        MovementPattern.CORE to core, null to other
    )

    private val Barbell = row(0.90f, 1.10f, 0.40f, 0.75f, 0.45f, 0.60f, 0.50f, 0.30f, 0.25f, 0.50f)
    private val Dumbbell = row(0.50f, 0.60f, 0.36f, 0.60f, 0.36f, 0.56f, 0.44f, 0.24f, 0.24f, 0.40f)
    private val Machine = row(1.40f, 0.70f, 0.35f, 0.70f, 0.45f, 0.70f, 0.65f, 0.25f, 0.30f, 0.50f)
    private val Kettlebell = row(0.25f, 0.22f, 0.16f, 0.18f, 0.16f, 0.25f, 0.20f, 0.12f, 0.12f, 0.18f)
    private val Plate = row(0.25f, 0.25f, 0.15f, 0.15f, 0.12f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f)
    private val Bodyweight = row(0.15f, 0.15f, 0.10f, 0.15f, 0.10f, 0.10f, 0.10f, 0.10f, 0.12f, 0.10f)

    private val DirectArm = setOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS)
    private val LoadedCore = setOf(MuscleGroup.ABDOMINALS, MuscleGroup.LOWER_BACK)

    private const val FALLBACK_BODYWEIGHT_KG = 70f
    private const val MINOR_AGE = 18
    private const val TEN_REPS = 10f
    private const val EPLEY_DIVISOR = 30f
    private const val EPLEY_CEILING_REPS = 12
}
