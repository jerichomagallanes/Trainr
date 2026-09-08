package com.jericx.trainr.presentation.onboarding.util

import com.jericx.trainr.common.Constants
import kotlin.math.roundToInt

object BodyMetricsConverter {

    fun parseMetrics(height: String, weight: String, useMetric: Boolean): Pair<Float, Float> {
        return if (useMetric) {
            val h = height.toFloatOrNull() ?: 0f
            val w = weight.toFloatOrNull() ?: 0f
            Pair(h, w)
        } else {
            val h = parseImperialHeight(height)
            val w = (weight.toFloatOrNull() ?: 0f) / Constants.Workout.KG_TO_LBS
            Pair(h, w)
        }
    }

    // A pasted measurement often carries the prime marks, and iOS substitutes curly
    // quotes as they are typed. The filter and the parser both speak straight quotes,
    // and both apps accept the same input.
    fun straightenQuotes(text: String): String {
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
        return try {
            val (h, w) = parseMetrics(height, weight, useMetric)
            if (h > 0 && w > 0) {
                val heightMeters = h / 100f
                w / (heightMeters * heightMeters)
            } else null
        } catch (e: Exception) {
            null
        }
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

    // Trims the float noise a pounds round trip leaves in a typed field.
    fun formatKilograms(kg: Float): String =
        if (kg % 1f == 0f) kg.toInt().toString() else ((kg * 10).roundToInt() / 10f).toString()

    fun convertWeightToImperial(weightKg: String): String {
        val kg = weightKg.toFloatOrNull() ?: return ""
        val lbs = kg * Constants.Workout.KG_TO_LBS
        return if (lbs > 0) lbs.roundToInt().toString() else ""
    }

    fun convertWeightToMetric(weightLbs: String): String {
        val lbs = weightLbs.toFloatOrNull() ?: return ""
        val kg = lbs / Constants.Workout.KG_TO_LBS
        return if (kg > 0) kg.roundToInt().toString() else ""
    }
}
