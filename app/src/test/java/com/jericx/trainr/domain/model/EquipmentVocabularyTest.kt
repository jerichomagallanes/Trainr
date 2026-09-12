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

    // "I have no equipment" is one of the nine answers, not a special case of
    // where someone stands: a gym member can still be given a push-up.
    @Test
    fun everyCategoryIncludingBodyweightIsOffered() {
        val offered = equipmentFor()

        assertThat(offered).containsNoDuplicates()
        assertThat(offered).containsExactlyElementsIn(EquipmentChoices).inOrder()
        assertThat(offered).contains(Equipment.NONE)
    }

    // A category the catalog cannot serve is a chip that leads nowhere.
    @Test
    fun aCategoryNothingIsStockedForIsNotOffered() {
        val stocked = setOf(Equipment.NONE, Equipment.DUMBBELL)

        assertThat(equipmentFor(stocked))
            .containsExactly(Equipment.NONE, Equipment.DUMBBELL).inOrder()
    }
}
