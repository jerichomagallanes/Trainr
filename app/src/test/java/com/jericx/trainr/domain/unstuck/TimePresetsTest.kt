package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimePresetsTest {

    @Test
    fun presetsEndAtThePlannedLengthTenMinutesApart() {
        assertThat(TimePresets.forPlanned(45)).containsExactly(25, 35, 45).inOrder()
        assertThat(TimePresets.forPlanned(30)).containsExactly(10, 20, 30).inOrder()
        assertThat(TimePresets.forPlanned(60)).containsExactly(40, 50, 60).inOrder()
    }

    @Test
    fun aShortPlanIsOfferedFewerChoicesRatherThanOneBelowTenMinutes() {
        assertThat(TimePresets.forPlanned(25)).containsExactly(15, 25).inOrder()
        assertThat(TimePresets.forPlanned(15)).containsExactly(15)
    }

    @Test
    fun aPlanShorterThanTheFloorStillOffersTheSessionAsPlanned() {
        assertThat(TimePresets.forPlanned(9)).containsExactly(9)
        assertThat(TimePresets.forPlanned(5)).containsExactly(5)
    }

    @Test
    fun theSupportedRangeIsNarrowerThanTheSchemaBound() {
        assertThat(TimePresets.isSupported(5)).isTrue()
        assertThat(TimePresets.isSupported(180)).isTrue()
        assertThat(TimePresets.isSupported(4)).isFalse()
        assertThat(TimePresets.isSupported(181)).isFalse()
    }
}
