package com.jericx.trainr.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightUnitTest {

    @Test
    fun metricShowsWhatWasStored() {
        assertThat(WeightUnit.forDisplay(20f, UnitSystem.METRIC)).isEqualTo(20f)
        assertThat(WeightUnit.forDisplay(22.5f, UnitSystem.METRIC)).isEqualTo(22.5f)
    }

    @Test
    fun imperialReadsBackInPounds() {
        assertThat(WeightUnit.forDisplay(0f, UnitSystem.IMPERIAL)).isEqualTo(0f)
        assertThat(WeightUnit.forDisplay(WeightUnit.toKilograms(45f, UnitSystem.IMPERIAL),
            UnitSystem.IMPERIAL)).isEqualTo(45f)
    }

    // A literal conversion of 20 kg is 44.09 lb, which is not a plate, a
    // dumbbell, or a number anyone would write in a log.
    @Test
    fun aPrescriptionMovesOntoAWeightTheGymActuallyHas() {
        assertThat(WeightUnit.forDisplay(WeightUnit.loadable(20f, UnitSystem.IMPERIAL),
            UnitSystem.IMPERIAL)).isEqualTo(45f)
        assertThat(WeightUnit.forDisplay(WeightUnit.loadable(10f, UnitSystem.IMPERIAL),
            UnitSystem.IMPERIAL)).isEqualTo(20f)
    }

    // Kilograms are prescribed for a gym graduated in kilograms; a 12 kg
    // dumbbell exists and must not be rounded onto one that is easier to state.
    @Test
    fun aMetricPrescriptionIsLeftAlone() {
        assertThat(WeightUnit.loadable(12f, UnitSystem.METRIC)).isEqualTo(12f)
        assertThat(WeightUnit.loadable(22.5f, UnitSystem.METRIC)).isEqualTo(22.5f)
    }

    // What the client typed is the record, so it survives the trip to storage
    // and back without moving. 22 lb is not a plate, but it is what they lifted.
    @Test
    fun whatTheClientTypedComesBackUnchanged() {
        for (pounds in listOf(22f, 45f, 47.5f, 95f, 135f, 225f)) {
            val stored = WeightUnit.toKilograms(pounds, UnitSystem.IMPERIAL)
            assertThat(WeightUnit.forDisplay(stored, UnitSystem.IMPERIAL)).isEqualTo(pounds)
        }
    }

    @Test
    fun metricEntryIsStoredAsItStands() {
        assertThat(WeightUnit.toKilograms(22.5f, UnitSystem.METRIC)).isEqualTo(22.5f)
    }
}
