package com.jericx.trainr.presentation.onboarding.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BodyMetricsConverterTest {

    @Test
    fun `parseImperialHeight returns cm for feet and inches`() {
        val input = "5'10\""

        val result = BodyMetricsConverter.parseImperialHeight(input)

        assertThat(result).isWithin(0.01f).of(177.8f)
    }

    @Test
    fun `parseImperialHeight handles feet only with apostrophe`() {
        val input = "6'"

        val result = BodyMetricsConverter.parseImperialHeight(input)

        assertThat(result).isWithin(0.01f).of(182.88f)
    }

    @Test
    fun `parseImperialHeight returns zero when format lacks apostrophe`() {
        val input = "70"

        val result = BodyMetricsConverter.parseImperialHeight(input)

        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `parseImperialHeight returns zero for empty string`() {
        val input = ""

        val result = BodyMetricsConverter.parseImperialHeight(input)

        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `convertHeightToImperial rounds near integer-foot boundary`() {
        // 182.88 cm is exactly 6'0"; float math yields 71.999... inches, must not truncate to 5'11"
        val input = "182.88"

        val result = BodyMetricsConverter.convertHeightToImperial(input)

        assertThat(result).isEqualTo("6'0\"")
    }

    @Test
    fun `convertHeightToImperial rounds typical height`() {
        val input = "170"

        val result = BodyMetricsConverter.convertHeightToImperial(input)

        assertThat(result).isEqualTo("5'7\"")
    }

    @Test
    fun `convertHeightToImperial returns empty string for invalid input`() {
        val input = "abc"

        val result = BodyMetricsConverter.convertHeightToImperial(input)

        assertThat(result).isEmpty()
    }

    @Test
    fun `convertHeightToImperial returns empty string for zero`() {
        val input = "0"

        val result = BodyMetricsConverter.convertHeightToImperial(input)

        assertThat(result).isEmpty()
    }

    @Test
    fun `convertHeightToMetric rounds cm value`() {
        val input = "5'10\""

        val result = BodyMetricsConverter.convertHeightToMetric(input)

        assertThat(result).isEqualTo("178")
    }

    @Test
    fun `convertHeightToMetric returns empty when format lacks apostrophe`() {
        val input = "70"

        val result = BodyMetricsConverter.convertHeightToMetric(input)

        assertThat(result).isEmpty()
    }

    @Test
    fun `convertWeightToImperial rounds kg to lbs`() {
        val input = "70"

        val result = BodyMetricsConverter.convertWeightToImperial(input)

        assertThat(result).isEqualTo("154")
    }

    @Test
    fun `convertWeightToImperial rounds up when fractional part exceeds half`() {
        val input = "80"

        val result = BodyMetricsConverter.convertWeightToImperial(input)

        assertThat(result).isEqualTo("176")
    }

    @Test
    fun `convertWeightToImperial returns empty for invalid input`() {
        val input = "abc"

        val result = BodyMetricsConverter.convertWeightToImperial(input)

        assertThat(result).isEmpty()
    }

    @Test
    fun `convertWeightToMetric rounds lbs to kg`() {
        val input = "154"

        val result = BodyMetricsConverter.convertWeightToMetric(input)

        assertThat(result).isEqualTo("70")
    }

    @Test
    fun `convertWeightToMetric returns empty for invalid input`() {
        val input = ""

        val result = BodyMetricsConverter.convertWeightToMetric(input)

        assertThat(result).isEmpty()
    }

    @Test
    fun `weight metric-imperial-metric round trip preserves value within 1 kg`() {
        val original = "70"

        val imperial = BodyMetricsConverter.convertWeightToImperial(original)
        val backToMetric = BodyMetricsConverter.convertWeightToMetric(imperial)

        val diff = kotlin.math.abs(backToMetric.toInt() - original.toInt())
        assertThat(diff).isAtMost(1)
    }

    @Test
    fun `height metric-imperial-metric round trip preserves value within 2 cm`() {
        val original = "170"

        val imperial = BodyMetricsConverter.convertHeightToImperial(original)
        val backToMetric = BodyMetricsConverter.convertHeightToMetric(imperial)

        // imperial is integer inches, so max round-trip drift is ~1.27 cm
        val diff = kotlin.math.abs(backToMetric.toInt() - original.toInt())
        assertThat(diff).isAtMost(2)
    }

    @Test
    fun `parseMetrics returns raw values when metric`() {
        val height = "170"
        val weight = "70"

        val (h, w) = BodyMetricsConverter.parseMetrics(height, weight, useMetric = true)

        assertThat(h).isEqualTo(170f)
        assertThat(w).isEqualTo(70f)
    }

    @Test
    fun `parseMetrics converts imperial input to cm and kg`() {
        val height = "5'10\""
        val weight = "154"

        val (h, w) = BodyMetricsConverter.parseMetrics(height, weight, useMetric = false)

        assertThat(h).isWithin(0.01f).of(177.8f)
        assertThat(w).isWithin(0.01f).of(154f / 2.20462f)
    }

    @Test
    fun `calculateBMI computes kg per square meter for metric input`() {
        val height = "170"
        val weight = "70"

        val result = BodyMetricsConverter.calculateBMI(height, weight, useMetric = true)

        assertThat(result).isNotNull()
        assertThat(result!!).isWithin(0.1f).of(24.22f)
    }

    @Test
    fun `calculateBMI matches across metric and imperial for equivalent input`() {
        val metricBmi = BodyMetricsConverter.calculateBMI("170", "70", useMetric = true)

        val imperialBmi = BodyMetricsConverter.calculateBMI("5'7\"", "154", useMetric = false)

        assertThat(metricBmi).isNotNull()
        assertThat(imperialBmi).isNotNull()
        assertThat(kotlin.math.abs(metricBmi!! - imperialBmi!!)).isLessThan(0.2f)
    }

    @Test
    fun `calculateBMI returns null for empty input`() {
        val result = BodyMetricsConverter.calculateBMI("", "", useMetric = true)

        assertThat(result).isNull()
    }

    @Test
    fun `calculateBMI returns null when height is zero`() {
        val result = BodyMetricsConverter.calculateBMI("0", "70", useMetric = true)

        assertThat(result).isNull()
    }
}
