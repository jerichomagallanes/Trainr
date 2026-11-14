package com.jericx.trainr.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceEligibilityTest {

    private fun gibibytes(value: Double): Long = (value * (1L shl 30)).toLong()

    private fun eligibility(
        totalMemory: Long,
        isLowRamDevice: Boolean = false,
        primaryAbi: String? = "arm64-v8a"
    ) = DeviceEligibility(totalMemory, isLowRamDevice, primaryAbi)

    @Test
    fun aThreeGigabytePhoneIsEligibleWhateverTheKernelReserved() {
        assertThat(eligibility(gibibytes(2.7)).isSupported).isTrue()
        assertThat(eligibility(gibibytes(2.8)).isSupported).isTrue()
        assertThat(eligibility(gibibytes(2.9)).isSupported).isTrue()
        assertThat(eligibility(gibibytes(3.0)).isSupported).isTrue()
    }

    @Test
    fun aTwoGigabytePhoneIsNotEligible() {
        assertThat(eligibility(gibibytes(1.8)).isSupported).isFalse()
    }

    @Test
    fun aFourGigabytePhoneIsEligible() {
        assertThat(eligibility(gibibytes(3.7)).isSupported).isTrue()
    }

    @Test
    fun aLowRamDeviceIsNeverEligible() {
        assertThat(eligibility(gibibytes(8.0), isLowRamDevice = true).isSupported).isFalse()
    }

    @Test
    fun onlySixtyFourBitArchitecturesAreEligible() {
        assertThat(eligibility(gibibytes(8.0), primaryAbi = "armeabi-v7a").isSupported).isFalse()
        assertThat(eligibility(gibibytes(8.0), primaryAbi = null).isSupported).isFalse()
        assertThat(eligibility(gibibytes(8.0), primaryAbi = "x86_64").isSupported).isTrue()
    }
}
