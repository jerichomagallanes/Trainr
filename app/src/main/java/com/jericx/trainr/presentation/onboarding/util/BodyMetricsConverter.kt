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

    fun parseImperialHeight(height: String): Float {
        val parts = height.replace("\"", "").split("'")
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
