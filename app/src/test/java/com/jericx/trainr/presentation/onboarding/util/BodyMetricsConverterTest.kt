package com.jericx.trainr.presentation.onboarding.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BodyMetricsConverterTest {

    // region parseImperialHeight

    @Test
    fun `parseImperialHeight returns cm for feet and inches`() {
        // Arrange
        val input = "5'10\""

        // Act
        val result = BodyMetricsConverter.parseImperialHeight(input)

        // Assert: 5*12 + 10 = 70 inches; 70 * 2.54 = 177.8 cm
        assertThat(result).isWithin(0.01f).of(177.8f)
    }

    @Test
    fun `parseImperialHeight handles feet only with apostrophe`() {
        // Arrange
        val input = "6'"

        // Act
        val result = BodyMetricsConverter.parseImperialHeight(input)

        // Assert: 6*12 = 72 inches; 72 * 2.54 = 182.88 cm
        assertThat(result).isWithin(0.01f).of(182.88f)
    }

    @Test
    fun `parseImperialHeight returns zero when format lacks apostrophe`() {
        // Arrange
        val input = "70"

        // Act
        val result = BodyMetricsConverter.parseImperialHeight(input)

        // Assert
        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `parseImperialHeight returns zero for empty string`() {
        // Arrange
        val input = ""

        // Act
        val result = BodyMetricsConverter.parseImperialHeight(input)

        // Assert
        assertThat(result).isEqualTo(0f)
    }

    // endregion

    // region convertHeightToImperial

    @Test
    fun `convertHeightToImperial rounds near integer-foot boundary`() {
        // Arrange: 182.88 cm is exactly 6'0"; float math yields 71.999... inches
        val input = "182.88"

        // Act
        val result = BodyMetricsConverter.convertHeightToImperial(input)

        // Assert: must round up to 6'0", not truncate to 5'11"
        assertThat(result).isEqualTo("6'0\"")
    }

    @Test
    fun `convertHeightToImperial rounds typical height`() {
        // Arrange: 170 cm = 66.929 inches ~ 5'7" (rounded)
        val input = "170"

        // Act
        val result = BodyMetricsConverter.convertHeightToImperial(input)

        // Assert
        assertThat(result).isEqualTo("5'7\"")
    }

    @Test
    fun `convertHeightToImperial returns empty string for invalid input`() {
        // Arrange
        val input = "abc"

        // Act
        val result = BodyMetricsConverter.convertHeightToImperial(input)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `convertHeightToImperial returns empty string for zero`() {
        // Arrange
        val input = "0"

        // Act
        val result = BodyMetricsConverter.convertHeightToImperial(input)

        // Assert
        assertThat(result).isEmpty()
    }

    // endregion

    // region convertHeightToMetric

    @Test
    fun `convertHeightToMetric rounds cm value`() {
        // Arrange: 5'10" = 177.8 cm -> rounds to 178
        val input = "5'10\""

        // Act
        val result = BodyMetricsConverter.convertHeightToMetric(input)

        // Assert
        assertThat(result).isEqualTo("178")
    }

    @Test
    fun `convertHeightToMetric returns empty when format lacks apostrophe`() {
        // Arrange
        val input = "70"

        // Act
        val result = BodyMetricsConverter.convertHeightToMetric(input)

        // Assert
        assertThat(result).isEmpty()
    }

    // endregion

    // region convertWeightToImperial

    @Test
    fun `convertWeightToImperial rounds kg to lbs`() {
        // Arrange: 70 kg * 2.20462 = 154.32 lbs -> rounds to 154
        val input = "70"

        // Act
        val result = BodyMetricsConverter.convertWeightToImperial(input)

        // Assert
        assertThat(result).isEqualTo("154")
    }

    @Test
    fun `convertWeightToImperial rounds up when fractional part exceeds half`() {
        // Arrange: 80 kg * 2.20462 = 176.37 lbs -> rounds to 176
        val input = "80"

        // Act
        val result = BodyMetricsConverter.convertWeightToImperial(input)

        // Assert
        assertThat(result).isEqualTo("176")
    }

    @Test
    fun `convertWeightToImperial returns empty for invalid input`() {
        // Arrange
        val input = "abc"

        // Act
        val result = BodyMetricsConverter.convertWeightToImperial(input)

        // Assert
        assertThat(result).isEmpty()
    }

    // endregion

    // region convertWeightToMetric

    @Test
    fun `convertWeightToMetric rounds lbs to kg`() {
        // Arrange: 154 lbs / 2.20462 = 69.85 kg -> rounds to 70
        val input = "154"

        // Act
        val result = BodyMetricsConverter.convertWeightToMetric(input)

        // Assert
        assertThat(result).isEqualTo("70")
    }

    @Test
    fun `convertWeightToMetric returns empty for invalid input`() {
        // Arrange
        val input = ""

        // Act
        val result = BodyMetricsConverter.convertWeightToMetric(input)

        // Assert
        assertThat(result).isEmpty()
    }

    // endregion

    // region round-trip

    @Test
    fun `weight metric-imperial-metric round trip preserves value within 1 kg`() {
        // Arrange
        val original = "70"

        // Act
        val imperial = BodyMetricsConverter.convertWeightToImperial(original)
        val backToMetric = BodyMetricsConverter.convertWeightToMetric(imperial)

        // Assert: rounding error should be at most 1 kg
        val diff = kotlin.math.abs(backToMetric.toInt() - original.toInt())
        assertThat(diff).isAtMost(1)
    }

    @Test
    fun `height metric-imperial-metric round trip preserves value within 2 cm`() {
        // Arrange
        val original = "170"

        // Act
        val imperial = BodyMetricsConverter.convertHeightToImperial(original)
        val backToMetric = BodyMetricsConverter.convertHeightToMetric(imperial)

        // Assert: imperial is integer inches, so max round-trip drift is ~1.27 cm
        val diff = kotlin.math.abs(backToMetric.toInt() - original.toInt())
        assertThat(diff).isAtMost(2)
    }

    // endregion

    // region parseMetrics

    @Test
    fun `parseMetrics returns raw values when metric`() {
        // Arrange
        val height = "170"
        val weight = "70"

        // Act
        val (h, w) = BodyMetricsConverter.parseMetrics(height, weight, useMetric = true)

        // Assert
        assertThat(h).isEqualTo(170f)
        assertThat(w).isEqualTo(70f)
    }

    @Test
    fun `parseMetrics converts imperial input to cm and kg`() {
        // Arrange
        val height = "5'10\""
        val weight = "154"

        // Act
        val (h, w) = BodyMetricsConverter.parseMetrics(height, weight, useMetric = false)

        // Assert
        assertThat(h).isWithin(0.01f).of(177.8f)
        assertThat(w).isWithin(0.01f).of(154f / 2.20462f)
    }

    // endregion

    // region calculateBMI

    @Test
    fun `calculateBMI computes kg per square meter for metric input`() {
        // Arrange: 70 kg / 1.7m^2 = 24.22
        val height = "170"
        val weight = "70"

        // Act
        val result = BodyMetricsConverter.calculateBMI(height, weight, useMetric = true)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!).isWithin(0.1f).of(24.22f)
    }

    @Test
    fun `calculateBMI matches across metric and imperial for equivalent input`() {
        // Arrange: 70 kg ~= 154 lbs; 170 cm ~= 5'7"
        val metricBmi = BodyMetricsConverter.calculateBMI("170", "70", useMetric = true)

        // Act
        val imperialBmi = BodyMetricsConverter.calculateBMI("5'7\"", "154", useMetric = false)

        // Assert: conversions should yield near-identical BMI
        assertThat(metricBmi).isNotNull()
        assertThat(imperialBmi).isNotNull()
        assertThat(kotlin.math.abs(metricBmi!! - imperialBmi!!)).isLessThan(0.2f)
    }

    @Test
    fun `calculateBMI returns null for empty input`() {
        // Arrange / Act
        val result = BodyMetricsConverter.calculateBMI("", "", useMetric = true)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `calculateBMI returns null when height is zero`() {
        // Arrange / Act
        val result = BodyMetricsConverter.calculateBMI("0", "70", useMetric = true)

        // Assert
        assertThat(result).isNull()
    }

    // endregion
}
