package com.jericx.trainr.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EquipmentVocabularyTest {

    // The three movements worth the time they take are a leg press, an upper
    // push and an upper pull. A gym list with nothing to pull from leaves the
    // model to invent the equipment or skip the pattern.
    @Test
    fun aGymCanPull() {
        assertThat(equipmentFor(WorkoutLocation.GYM))
            .containsAtLeast(Equipment.PULL_UP_BAR, Equipment.CABLE_MACHINE, Equipment.MACHINES)
    }

    // A garage with a barbell is a home gym, and a bench is the most common
    // thing in one after the dumbbells.
    @Test
    fun aHomeCanBeLoaded() {
        assertThat(equipmentFor(WorkoutLocation.HOME))
            .containsAtLeast(Equipment.BENCH, Equipment.BARBELL, Equipment.SQUAT_RACK)
    }

    // Training in both places used to mean being asked only about the gym.
    @Test
    fun bothIsTheUnionAndNotTheGymList() {
        val both = equipmentFor(WorkoutLocation.BOTH)

        assertThat(both).containsAtLeast(Equipment.JUMP_ROPE, Equipment.MACHINES)
        assertThat(both).doesNotContain(Equipment.NONE)
        assertThat(both).containsNoDuplicates()
    }

    @Test
    fun bodyweightOnlyIsOfferedOnlyWhereItIsAnAnswer() {
        assertThat(equipmentFor(WorkoutLocation.HOME)).contains(Equipment.NONE)
        assertThat(equipmentFor(WorkoutLocation.GYM)).doesNotContain(Equipment.NONE)
    }
}
