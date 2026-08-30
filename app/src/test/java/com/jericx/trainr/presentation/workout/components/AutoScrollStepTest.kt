package com.jericx.trainr.presentation.workout.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AutoScrollStepTest {

    private val viewport = 2000f
    private val maxStep = 30f

    @Test
    fun aDragInTheMiddleOfTheScreenDoesNotScroll() {
        assertThat(autoScrollStep(1000f, viewport, maxStep)).isEqualTo(0f)
        assertThat(autoScrollStep(301f, viewport, maxStep)).isEqualTo(0f)
    }

    @Test
    fun aDragNearTheTopScrollsBack() {
        assertThat(autoScrollStep(150f, viewport, maxStep)).isLessThan(0f)
    }

    @Test
    fun aDragNearTheBottomScrollsOn() {
        assertThat(autoScrollStep(1900f, viewport, maxStep)).isGreaterThan(0f)
    }

    // The further into the edge the finger sits, the faster it goes.
    @Test
    fun theEdgeBandAcceleratesTowardsTheRim() {
        val shallow = autoScrollStep(1800f, viewport, maxStep)
        val deep = autoScrollStep(1990f, viewport, maxStep)

        assertThat(deep).isGreaterThan(shallow)
        assertThat(deep).isAtMost(maxStep)
    }

    @Test
    fun anUnmeasuredViewportScrollsNothing() {
        assertThat(autoScrollStep(100f, 0f, maxStep)).isEqualTo(0f)
    }
}
