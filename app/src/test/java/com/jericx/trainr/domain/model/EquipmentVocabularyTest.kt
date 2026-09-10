package com.jericx.trainr.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EquipmentVocabularyTest {

    // The setup screen asks in the catalog's own vocabulary, so a chip and a
    // movement's category are the same word or the filter silently misses.
    @Test
    fun theChipsAreTheCatalogsOwnNineCategories() {
        assertThat(EquipmentChoices).containsExactly(
            Equipment.NONE,
            Equipment.BARBELL,
            Equipment.DUMBBELL,
            Equipment.KETTLEBELL,
            Equipment.MACHINE,
            Equipment.PLATE,
            Equipment.RESISTANCE_BAND,
            Equipment.SUSPENSION_BAND,
            Equipment.OTHER
        ).inOrder()
    }

    // "I have no equipment" is an answer at home and nowhere else.
    @Test
    fun bodyweightOnlyIsOfferedOnlyWhereItIsAnAnswer() {
        assertThat(equipmentFor(WorkoutLocation.HOME)).contains(Equipment.NONE)
        assertThat(equipmentFor(WorkoutLocation.GYM)).doesNotContain(Equipment.NONE)
        assertThat(equipmentFor(WorkoutLocation.BOTH)).doesNotContain(Equipment.NONE)
    }

    @Test
    fun everyLocationCanReachEveryKindOfKit() {
        WorkoutLocation.entries.forEach { location ->
            val offered = equipmentFor(location)
            assertThat(offered).containsNoDuplicates()
            assertThat(offered).containsAtLeast(
                Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE, Equipment.OTHER
            )
        }
    }
}
