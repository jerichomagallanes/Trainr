package com.jericx.trainr.data.local

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutTime
import com.jericx.trainr.domain.model.WorkoutType
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
        workoutLocation = WorkoutLocation.GYM,
        availableEquipment = listOf(Equipment.DUMBBELLS, Equipment.BARBELL),
        workoutDaysPerWeek = 4,
        workoutDuration = 60,
        preferredWorkoutTime = WorkoutTime.EVENING,
        injuries = listOf("Lower back"),
        workoutType = WorkoutType.STRENGTH,
        createdAt = 1_700_000_000_000L
    )

    @Test
    fun `mapToEntity serializes enums by name and preserves scalar fields`() {
        // Arrange
        val profile = sampleProfile()

        // Act
        val entity = mapper.mapToEntity(profile)

        // Assert
        assertThat(entity.id).isEqualTo(7L)
        assertThat(entity.firstName).isEqualTo("Jericho")
        assertThat(entity.gender).isEqualTo("MALE")
        assertThat(entity.fitnessGoal).isEqualTo("MUSCLE_GAIN")
        assertThat(entity.experienceLevel).isEqualTo("INTERMEDIATE")
        assertThat(entity.workoutLocation).isEqualTo("GYM")
        assertThat(entity.preferredWorkoutTime).isEqualTo("EVENING")
        assertThat(entity.workoutType).isEqualTo("STRENGTH")
        assertThat(entity.availableEquipment).containsExactly("DUMBBELLS", "BARBELL").inOrder()
        assertThat(entity.injuries).containsExactly("Lower back")
        assertThat(entity.createdAt).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun `UserProfile round-trip through entity preserves all fields`() {
        // Arrange
        val original = sampleProfile()

        // Act
        val restored = mapper.mapToDomain(mapper.mapToEntity(original))

        // Assert
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `mapToDomain drops unknown equipment names instead of throwing`() {
        // Arrange: simulate an entity with a stale enum name no longer in the codebase
        val entity = mapper.mapToEntity(sampleProfile()).copy(
            availableEquipment = listOf("DUMBBELLS", "OBSOLETE_GADGET")
        )

        // Act
        val domain = mapper.mapToDomain(entity)

        // Assert
        assertThat(domain.availableEquipment).containsExactly(Equipment.DUMBBELLS)
    }
}
