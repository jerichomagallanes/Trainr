package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import org.junit.Test

class SeedLoadTest {

    private val squat = CatalogExercise(
        key = "barbell_squat", name = "Squat", primary = MuscleGroup.QUADRICEPS,
        secondary = emptyList(), equipment = Equipment.BARBELL,
        measure = ExerciseMeasure.WEIGHT_AND_REPS, pattern = MovementPattern.SQUAT, staple = true
    )
    private val man = UserProfile(
        age = 30, gender = Gender.MALE, weight = 80f, experienceLevel = ExperienceLevel.INTERMEDIATE
    )

    private fun seed(user: UserProfile, exercise: CatalogExercise = squat) =
        SeedLoad.tenRepMaxKg(user, exercise)!!

    // Erring light is corrected inside a session; erring heavy is an injury.
    @Test
    fun theGuessErrsLightForEveryoneItKnowsLessAbout() {
        assertThat(seed(man.copy(gender = Gender.FEMALE))).isLessThan(seed(man))
        assertThat(seed(man.copy(gender = Gender.PREFER_NOT_TO_SAY))).isLessThan(seed(man))
        assertThat(seed(man.copy(age = 70))).isLessThan(seed(man))
        assertThat(seed(man.copy(age = 15))).isLessThan(seed(man))
        assertThat(seed(man.copy(experienceLevel = ExperienceLevel.BEGINNER))).isLessThan(seed(man))
    }

    @Test
    fun anAdvancedLifterIsGuessedHeavierThanAnIntermediate() {
        assertThat(seed(man.copy(experienceLevel = ExperienceLevel.ADVANCED))).isGreaterThan(seed(man))
    }

    // Ten reps is the anchor, so a lower rep target is heavier and a higher
    // one lighter.
    @Test
    fun fewerRepsAreHeavierAndMoreAreLighter() {
        assertThat(SeedLoad.atReps(100f, 10)).isWithin(0.01f).of(100f)
        assertThat(SeedLoad.atReps(100f, 5)).isGreaterThan(100f)
        assertThat(SeedLoad.atReps(100f, 15)).isLessThan(100f)
    }

    // Past twelve reps Epley stops meaning anything, so it stops moving.
    @Test
    fun theConversionStopsAtTwelveReps() {
        assertThat(SeedLoad.atReps(100f, 20)).isEqualTo(SeedLoad.atReps(100f, 12))
    }

    @Test
    fun aMovementWithNoWeightHasNoSeed() {
        val pushUp = squat.copy(key = "push_up", equipment = Equipment.NONE, measure = ExerciseMeasure.REPS)

        assertThat(SeedLoad.tenRepMaxKg(man, pushUp)).isNull()
    }

    // A calf press is not a lateral raise.
    @Test
    fun calvesAreSeededHeavierThanShoulderIsolationOnTheSameKit() {
        val calf = squat.copy(key = "calf", primary = MuscleGroup.CALVES, pattern = MovementPattern.ISOLATION)
        val raise = squat.copy(key = "raise", primary = MuscleGroup.SHOULDERS, pattern = MovementPattern.ISOLATION)

        assertThat(seed(man, calf)).isGreaterThan(seed(man, raise) * 3)
    }

    @Test
    fun aClientWhoLeftTheirWeightBlankIsStillGivenAGuess() {
        assertThat(seed(man.copy(weight = 0f))).isGreaterThan(0f)
    }
}
