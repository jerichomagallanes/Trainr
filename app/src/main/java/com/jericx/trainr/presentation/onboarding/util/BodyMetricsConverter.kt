package com.jericx.trainr.presentation.onboarding.util

import com.jericx.trainr.common.Constants
import kotlin.math.roundToInt

object BodyMetricsConverter {

    // CDC adult categories apply only from age 20. An unknown age is not an adult.
    fun showsAdultBMI(age: Int?): Boolean = age != null && age >= ADULT_BMI_AGE

    const val ADULT_BMI_AGE = 20

    fun parseHeightCm(height: String, useMetric: Boolean): Float =
        if (useMetric) height.toFloatOrNull() ?: 0f else parseImperialHeight(height)

    fun parseWeightKg(weight: String, useMetric: Boolean): Float {
        val typed = weight.toFloatOrNull() ?: 0f
        return if (useMetric) typed else typed / Constants.Workout.KG_TO_LBS
    }

    fun parseMetrics(height: String, weight: String, useMetric: Boolean): Pair<Float, Float> =
        Pair(parseHeightCm(height, useMetric), parseWeightKg(weight, useMetric))

    // The measurements themselves, which a unit switch does not change. A field
    // showing what a conversion made of a measurement is read back as the
    // measurement it was given: a whole inch is coarser than a centimetre, so
    // the fields alone would move the index when only the units moved.
    fun keptMetrics(
        height: String,
        weight: String,
        useMetric: Boolean,
        heightSwap: UnitSwap,
        weightSwap: UnitSwap
    ): Pair<Float, Float> {
        val before = !useMetric
        val cm = heightSwap.measurementBehind(height)
            ?.let { parseHeightCm(it, before) }
            ?: parseHeightCm(height, useMetric)
        val kg = weightSwap.measurementBehind(weight)
            ?.let { parseWeightKg(it, before) }
            ?: parseWeightKg(weight, useMetric)
        return Pair(cm, kg)
    }

    // A pasted measurement often carries the prime marks, and iOS substitutes curly
    // quotes as they are typed. The filter and the parser both speak straight quotes,
    // and both apps accept the same input.
    private fun straightenQuotes(text: String): String {
        var straightened = text
        for (curly in listOf('\u2018', '\u2019', '\u2032')) {
            straightened = straightened.replace(curly, '\'')
        }
        for (curly in listOf('\u201C', '\u201D', '\u2033')) {
            straightened = straightened.replace(curly, '"')
        }
        return straightened
    }

    // The accepted text, or null when the field should keep what it had. Imperial
    // allows a part-typed measurement, so "5" and "5'" pass on the way to "5'10"".
    fun acceptedHeight(text: String, useMetric: Boolean): String? {
        if (useMetric) {
            return if (text.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) text else null
        }
        val straightened = straightenQuotes(text)
        return if (straightened.matches(Regex("^\\d{0,1}'?\\d{0,2}\"?$"))) straightened else null
    }

    fun parseImperialHeight(height: String): Float {
        val parts = straightenQuotes(height).replace("\"", "").split("'")
        return if (parts.size == 2) {
            val feet = parts[0].toIntOrNull() ?: 0
            val inches = parts[1].toIntOrNull() ?: 0
            (feet * Constants.Workout.INCHES_PER_FOOT * Constants.Workout.CM_TO_INCHES) +
                    (inches * Constants.Workout.CM_TO_INCHES)
        } else {
            0f
        }
    }

    fun calculateBMI(height: String, weight: String, useMetric: Boolean): Float? {
        val (h, w) = parseMetrics(height, weight, useMetric)
        return bodyMassIndex(h, w)
    }

    fun bodyMassIndex(heightCm: Float, weightKg: Float): Float? {
        if (!heightCm.isFinite() || !weightKg.isFinite() || heightCm <= 0f || weightKg <= 0f) {
            return null
        }
        val heightMeters = heightCm / CM_PER_METRE
        return weightKg / (heightMeters * heightMeters)
    }

    fun convertHeightToImperial(heightCm: String): String {
        val cm = heightCm.toFloatOrNull() ?: return ""
        val totalInches = (cm / Constants.Workout.CM_TO_INCHES).roundToInt()
        val feet = totalInches / Constants.Workout.INCHES_PER_FOOT.toInt()
        val inches = totalInches % Constants.Workout.INCHES_PER_FOOT.toInt()
        return if (feet > 0 || inches > 0) "$feet'$inches\"" else ""
    }

    fun convertHeightToMetric(heightImperial: String): String {
        val cm = parseImperialHeight(heightImperial)
        return if (cm > 0) cm.roundToInt().toString() else ""
    }

    // The tenth the field accepts, kept in both directions: rounding pounds to
    // a whole number on the way out loses the tenth of a kilogram on the way back.
    fun formatWeight(value: Float): String {
        val rounded = (value * TENTHS).roundToInt() / TENTHS
        return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    }

    fun convertWeightToImperial(weightKg: String): String {
        val kg = weightKg.toFloatOrNull() ?: return ""
        val lbs = kg * Constants.Workout.KG_TO_LBS
        return if (lbs > 0) formatWeight(lbs) else ""
    }

    fun convertWeightToMetric(weightLbs: String): String {
        val lbs = weightLbs.toFloatOrNull() ?: return ""
        val kg = lbs / Constants.Workout.KG_TO_LBS
        return if (kg > 0) formatWeight(kg) else ""
    }

    // What a field shows and the text it was converted from. A whole inch is
    // coarser than a centimetre, so no pair of conversions can be each other's
    // inverse; the text handed over is kept and given back instead.
    data class UnitSwap(val shown: String = "", val typed: String? = null) {
        // Nothing is behind a part-typed measurement that was kept rather than
        // converted: it never left the units the field is showing.
        fun measurementBehind(current: String): String? =
            typed?.takeIf { shown == current && it != shown }
    }

    fun swapUnits(current: String, last: UnitSwap, convert: (String) -> String): UnitSwap {
        if (last.typed != null && last.shown == current) return UnitSwap(last.typed)

        // A part-typed measurement converts to nothing. Emptying the field would
        // throw away what was being written, so it is left exactly as it is and
        // the range message under it says it is not a measurement yet.
        val converted = convert(current)
        if (converted.isEmpty() && current.isNotEmpty()) return UnitSwap(current, current)

        return UnitSwap(converted, current)
    }

    private const val TENTHS = 10f
    private const val CM_PER_METRE = 100f
}
