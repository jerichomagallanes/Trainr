package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet

enum class PrescriptionUnit { REPS, SECONDS, MINUTES }

// What the chip under an exercise says, worked out from the sets rather than
// written by a model that could disagree with them. A value rather than a
// string so the words live with the rest of the copy.
sealed interface Prescription {

    data object None : Prescription

    data class Fixed(
        val setCount: Int,
        val unit: PrescriptionUnit,
        val amount: Int,
        val perSide: Boolean
    ) : Prescription

    data class Spread(
        val setCount: Int,
        val unit: PrescriptionUnit,
        val low: Int,
        val high: Int,
        val perSide: Boolean
    ) : Prescription

    companion object {

        // A minute is easier to read than sixty seconds, and a long piece of
        // conditioning in seconds is unreadable.
        private const val SECONDS_PER_MINUTE = 60
        private const val LONG_HOLD_SECONDS = 120

        fun of(
            sets: List<ExerciseSet>,
            measure: ExerciseMeasure,
            unilateral: Boolean = false
        ): Prescription {
            if (sets.isEmpty()) return None

            val amounts = when (measure) {
                ExerciseMeasure.DURATION -> sets.mapNotNull { it.targetSeconds }
                else -> sets.mapNotNull { it.targetReps }
            }
            if (amounts.isEmpty()) return None

            val low = amounts.min()
            val high = amounts.max()
            val perSide = unilateral && measure != ExerciseMeasure.DURATION

            if (measure == ExerciseMeasure.DURATION) {
                val whole = amounts.all { it % SECONDS_PER_MINUTE == 0 }
                if (low >= LONG_HOLD_SECONDS && whole) {
                    return spreadOrFixed(
                        sets.size, PrescriptionUnit.MINUTES,
                        low / SECONDS_PER_MINUTE, high / SECONDS_PER_MINUTE, perSide
                    )
                }
                return spreadOrFixed(sets.size, PrescriptionUnit.SECONDS, low, high, perSide)
            }
            return spreadOrFixed(sets.size, PrescriptionUnit.REPS, low, high, perSide)
        }

        private fun spreadOrFixed(
            setCount: Int,
            unit: PrescriptionUnit,
            low: Int,
            high: Int,
            perSide: Boolean
        ): Prescription =
            if (low == high) {
                Fixed(setCount, unit, low, perSide)
            } else {
                Spread(setCount, unit, low, high, perSide)
            }
    }
}
