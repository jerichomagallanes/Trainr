package com.jericx.trainr.data.local

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.unstuck.ActualOrigin
import org.junit.Test

class UserMapperTest {

    private val mapper = UserMapper()

    private fun sampleProfile() = UserProfile(
        id = 7L,
        firstName = "Jericho",
        age = 30,
        gender = Gender.MALE,
        height = 175f,
        weight = 72f,
        fitnessGoal = FitnessGoal.MUSCLE_GAIN,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        availableEquipment = listOf(Equipment.DUMBBELL, Equipment.BARBELL),
        workoutDaysPerWeek = 4,
        workoutDuration = 60,
        injuries = listOf(Injury.LOWER_BACK),
        createdAt = 1_700_000_000_000L
    )

    @Test
    fun `mapToEntity serializes enums by name and preserves scalar fields`() {
        val profile = sampleProfile()

        val entity = mapper.mapToEntity(profile)

        assertThat(entity.id).isEqualTo(7L)
        assertThat(entity.firstName).isEqualTo("Jericho")
        assertThat(entity.gender).isEqualTo("MALE")
        assertThat(entity.fitnessGoal).isEqualTo("MUSCLE_GAIN")
        assertThat(entity.experienceLevel).isEqualTo("INTERMEDIATE")
        assertThat(entity.availableEquipment).containsExactly("DUMBBELL", "BARBELL").inOrder()
        assertThat(entity.injuries).containsExactly("LOWER_BACK")
        assertThat(entity.createdAt).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun `UserProfile round-trip through entity preserves all fields`() {
        val original = sampleProfile()

        val restored = mapper.mapToDomain(mapper.mapToEntity(original))

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `mapToDomain drops unknown equipment names instead of throwing`() {
        val entity = mapper.mapToEntity(sampleProfile()).copy(
            availableEquipment = listOf("DUMBBELL", "OBSOLETE_GADGET")
        )

        val domain = mapper.mapToDomain(entity)

        assertThat(domain.availableEquipment).containsExactly(Equipment.DUMBBELL)
    }

    // Nine categories replaced a longer list, and a profile saved under the
    // old names must not come back with no equipment at all.
    @Test
    fun `equipment saved under the old names still reads back`() {
        val entity = mapper.mapToEntity(sampleProfile()).copy(
            availableEquipment = listOf(
                "DUMBBELLS", "CABLE_MACHINE", "PULL_UP_BAR", "SQUAT_RACK", "ANTIGRAVITY_BOOTS"
            )
        )

        assertThat(mapper.mapToDomain(entity).availableEquipment)
            .containsExactly(Equipment.DUMBBELL, Equipment.MACHINE, Equipment.BARBELL)
    }

    // No build writes these names. They pin what a later release that renames or
    // drops a constant would read back, where today the read throws on launch.
    @Test
    fun `an unknown gender, goal or level falls back instead of throwing`() {
        val entity = mapper.mapToEntity(sampleProfile()).copy(
            gender = "ANDROGYNE",
            fitnessGoal = "MOBILITY",
            experienceLevel = "ELITE"
        )

        val domain = mapper.mapToDomain(entity)

        assertThat(domain.gender).isEqualTo(Gender.PREFER_NOT_TO_SAY)
        assertThat(domain.fitnessGoal).isEqualTo(FitnessGoal.GENERAL_FITNESS)
        assertThat(domain.experienceLevel).isEqualTo(ExperienceLevel.BEGINNER)
    }

    @Test
    fun `an unknown injury is dropped and the known ones survive`() {
        val entity = mapper.mapToEntity(sampleProfile()).copy(
            injuries = listOf("LOWER_BACK", "ELBOW")
        )

        assertThat(mapper.mapToDomain(entity).injuries).containsExactly(Injury.LOWER_BACK)
    }

    @Test
    fun `a day whose status is unreadable is not started`() {
        val day = WorkoutDay(
            dayNumber = 1,
            title = "Push",
            status = WorkoutStatus.IN_PROGRESS,
            duration = 45,
            exerciseCount = 5,
            equipment = listOf("dumbbell")
        )
        val entity = mapper.mapToEntity(day, 1L).copy(status = "ABANDONED_HALFWAY")

        assertThat(mapper.mapToDomain(entity).status).isEqualTo(WorkoutStatus.NOT_STARTED)
    }

    @Test
    fun `a set with actuals but no origin is stored as legacy unknown`() {
        val set = ExerciseSet(setNumber = 1, targetReps = 8, actualReps = 8, isCompleted = true)

        assertThat(mapper.mapToEntity(set, 1L).actualOrigin).isEqualTo("LEGACY_UNKNOWN")
    }

    @Test
    fun `a set with no actuals keeps origin none and a typed set keeps typed`() {
        val blank = ExerciseSet(setNumber = 1, targetReps = 8)
        val typed = blank.copy(actualReps = 6, actualOrigin = ActualOrigin.TYPED)

        assertThat(mapper.mapToEntity(blank, 1L).actualOrigin).isEqualTo("NONE")
        assertThat(mapper.mapToEntity(typed, 1L).actualOrigin).isEqualTo("TYPED")
    }
}
