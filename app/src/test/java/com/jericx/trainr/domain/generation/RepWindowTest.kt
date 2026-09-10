package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import org.junit.Test

class RepWindowTest {

    private val squat = CatalogExercise(
        key = "back_squat", name = "Back Squat", primary = MuscleGroup.QUADRICEPS,
        secondary = emptyList(), equipment = Equipment.BARBELL,
        measure = ExerciseMeasure.WEIGHT_AND_REPS, pattern = MovementPattern.SQUAT, staple = true
    )

    private val curl = CatalogExercise(
        key = "biceps_curl", name = "Biceps Curl", primary = MuscleGroup.BICEPS,
        secondary = emptyList(), equipment = Equipment.DUMBBELL,
        measure = ExerciseMeasure.WEIGHT_AND_REPS, pattern = MovementPattern.ISOLATION,
        staple = true
    )

    private fun profile(
        goal: FitnessGoal,
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
        age: Int = 30
    ) = UserProfile(age = age, fitnessGoal = goal, experienceLevel = experience)

    // A goal-only table is what puts three sets of ten on a deadlift and a
    // calf raise alike.
    @Test
    fun aCompoundAndAnIsolationAreNotGivenTheSameWindow() {
        val user = profile(FitnessGoal.MUSCLE_GAIN)

        assertThat(RepWindow.forExercise(user, squat)).isEqualTo(6..10)
        assertThat(RepWindow.forExercise(user, curl)).isEqualTo(8..15)
    }

    @Test
    fun aStrengthGoalIsGivenHeavySetsOnCompounds() {
        assertThat(RepWindow.forExercise(profile(FitnessGoal.STRENGTH), squat)).isEqualTo(3..6)
    }

    // Technique comes before load: a beginner is not put on triples however
    // strong they want to be.
    @Test
    fun aBeginnerWithAStrengthGoalIsNotPrescribedThreeRepSets() {
        val beginner = profile(FitnessGoal.STRENGTH, ExperienceLevel.BEGINNER)

        assertThat(RepWindow.forExercise(beginner, squat).first).isAtLeast(8)
    }

    @Test
    fun anEnduranceGoalWorksInLongSets() {
        assertThat(RepWindow.forExercise(profile(FitnessGoal.ENDURANCE), squat))
            .isEqualTo(12..20)
    }

    // Further from a maximum means more reps of less weight, not a different
    // movement.
    @Test
    fun anOlderClientWorksFurtherFromAMaximum() {
        val older = profile(FitnessGoal.MUSCLE_GAIN, age = 70)

        assertThat(RepWindow.forExercise(older, squat)).isEqualTo(8..12)
        assertThat(RepWindow.loadStepFraction(older, squat)).isAtMost(0.025f)
    }

    @Test
    fun aMinorIsNeverPrescribedALowRepMaximum() {
        val young = profile(FitnessGoal.STRENGTH, ExperienceLevel.ADVANCED, age = 15)

        assertThat(RepWindow.forExercise(young, squat).first).isAtLeast(8)
    }

    // Legs tolerate a bigger jump than arms.
    @Test
    fun aLowerBodyCompoundClimbsFasterThanAnIsolation() {
        val user = profile(FitnessGoal.MUSCLE_GAIN)

        assertThat(RepWindow.loadStepFraction(user, squat))
            .isGreaterThan(RepWindow.loadStepFraction(user, curl))
    }

    // The rest the skeleton pays for has to be the rest the budget priced.
    @Test
    fun theCompoundRestMatchesWhatTheSessionBudgetPaidFor() {
        FitnessGoal.entries.forEach { goal ->
            assertThat(SessionBudget.restSeconds(goal, ExerciseRole.COMPOUND))
                .isEqualTo(SessionBudget.restSeconds(goal))
            assertThat(SessionBudget.restSeconds(goal, ExerciseRole.ISOLATION))
                .isAtMost(SessionBudget.restSeconds(goal))
            assertThat(SessionBudget.restSeconds(goal, ExerciseRole.TIMED)).isEqualTo(30)
        }
    }

    // Every movement in the catalog has to land somewhere sane, or a plan can
    // be built asking for zero reps.
    @Test
    fun everyGoalAndRoleGivesAWindowAClientCouldPerform() {
        FitnessGoal.entries.forEach { goal ->
            listOf(squat, curl).forEach { movement ->
                val window = RepWindow.forExercise(profile(goal), movement)

                assertThat(window.first).isAtLeast(1)
                assertThat(window.last).isAtMost(30)
                assertThat(window.first).isAtMost(window.last)
            }
        }
        assertThat(curl.role).isEqualTo(ExerciseRole.ISOLATION)
    }
}
