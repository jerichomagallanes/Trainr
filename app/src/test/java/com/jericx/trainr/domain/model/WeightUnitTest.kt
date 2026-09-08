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

    // A literal 20 kg is 44.09 lb, which is not a plate or a dumbbell anyone owns
    @Test
    fun aPrescriptionMovesOntoAWeightTheGymActuallyHas() {
        assertThat(WeightUnit.forDisplay(WeightUnit.loadable(20f, UnitSystem.IMPERIAL),
            UnitSystem.IMPERIAL)).isEqualTo(45f)
        assertThat(WeightUnit.forDisplay(WeightUnit.loadable(10f, UnitSystem.IMPERIAL),
            UnitSystem.IMPERIAL)).isEqualTo(20f)
    }

    // A 12 kg dumbbell exists, so a metric prescription is never rounded to a rounder number
    @Test
    fun aMetricPrescriptionIsLeftAlone() {
        assertThat(WeightUnit.loadable(12f, UnitSystem.METRIC)).isEqualTo(12f)
        assertThat(WeightUnit.loadable(22.5f, UnitSystem.METRIC)).isEqualTo(22.5f)
    }

    // What the client typed is the record: 22 lb is not a plate, but it survives storage unmoved
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
