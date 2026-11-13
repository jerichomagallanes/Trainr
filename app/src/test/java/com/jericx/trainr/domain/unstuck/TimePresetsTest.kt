package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimePresetsTest {

    @Test
    fun presetsStopBelowThePlannedLengthTenMinutesApart() {
        assertThat(TimePresets.forPlanned(45)).containsExactly(15, 25, 35).inOrder()
        assertThat(TimePresets.forPlanned(39)).containsExactly(19, 29).inOrder()
        assertThat(TimePresets.forPlanned(60)).containsExactly(30, 40, 50).inOrder()
    }

    @Test
    fun aShortPlanIsOfferedFewerChoicesRatherThanOneBelowTenMinutes() {
        assertThat(TimePresets.forPlanned(30)).containsExactly(10, 20).inOrder()
        assertThat(TimePresets.forPlanned(25)).containsExactly(15)
    }

    // The planned length itself can only be answered with "already fits".
    @Test
    fun aPlanWithNothingBelowItOffersNoPreset() {
        assertThat(TimePresets.forPlanned(19)).isEmpty()
        assertThat(TimePresets.forPlanned(9)).isEmpty()
        assertThat(TimePresets.forPlanned(5)).isEmpty()
    }

    @Test
    fun theSupportedRangeIsNarrowerThanTheSchemaBound() {
        assertThat(TimePresets.isSupported(5)).isTrue()
        assertThat(TimePresets.isSupported(180)).isTrue()
        assertThat(TimePresets.isSupported(4)).isFalse()
        assertThat(TimePresets.isSupported(181)).isFalse()
    }
}
