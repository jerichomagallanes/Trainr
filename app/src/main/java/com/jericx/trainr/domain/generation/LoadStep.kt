package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.isLoadable
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import kotlin.math.max
import kotlin.math.roundToInt

enum class Snap { NEAREST, DOWN, UP }

// What a movement can actually be loaded in. A barbell climbs in plate pairs
// from an empty bar, a dumbbell rack skips the numbers it does not stock, and
// a kettlebell only exists in the sizes it is cast in. Rounding in kilograms
// puts 22.5 kg on a barbell, which no symmetric pair of plates can make, so
// every rung is counted in the unit the gym marks its kit in.
object LoadStep {

    fun snap(
        kg: Float,
        exercise: CatalogExercise,
        units: UnitSystem,
        how: Snap = Snap.NEAREST
    ): Float {
        if (!exercise.isLoadable) return kg
        rungsFor(exercise.equipment, units)?.let { return snapToRungs(kg, it, units, how) }

        val (base, step) = baseAndStep(exercise.equipment, units)
        val shown = WeightUnit.forDisplay(kg, units)
        val above = (shown - base) / step
        val rungs = max(
            0f,
            when (how) {
                Snap.NEAREST -> above.roundToInt().toFloat()
                Snap.DOWN -> kotlin.math.floor(above + TOLERANCE)
                Snap.UP -> kotlin.math.ceil(above - TOLERANCE)
            }
        )
        return capped(WeightUnit.toKilograms(base + rungs * step, units), exercise.equipment)
    }

    // Strictly heavier than what was asked for, so a week of "one increment at
    // minimum" can never land back on the same plate.
    fun nextUp(kg: Float, exercise: CatalogExercise, units: UnitSystem): Float {
        val here = snap(kg, exercise, units, Snap.NEAREST)
        var candidate = snap(here + smallestStepKg(exercise, units), exercise, units, Snap.UP)
        if (candidate <= here) {
            candidate = snap(here + smallestStepKg(exercise, units) * 2, exercise, units, Snap.UP)
        }
        return capped(max(candidate, here), exercise.equipment)
    }

    fun nextDown(kg: Float, exercise: CatalogExercise, units: UnitSystem): Float {
        val here = snap(kg, exercise, units, Snap.NEAREST)
        val candidate = snap(here - smallestStepKg(exercise, units), exercise, units, Snap.DOWN)
        return max(candidate, lightest(exercise, units))
    }

    // How coarse this movement's smallest change is against the load itself.
    // A 5 kg jump on a 10 kg cable is half again as heavy; the same jump on a
    // 100 kg leg press is nothing.
    fun stepFractionOf(kg: Float, exercise: CatalogExercise, units: UnitSystem): Float {
        if (!exercise.isLoadable || kg <= 0f) return 0f
        return smallestStepKg(exercise, units) / kg
    }

    fun lightest(exercise: CatalogExercise, units: UnitSystem): Float {
        if (!exercise.isLoadable) return 0f
        rungsFor(exercise.equipment, units)?.let {
            return capped(WeightUnit.toKilograms(it.first(), units), exercise.equipment)
        }
        val (base, _) = baseAndStep(exercise.equipment, units)
        return capped(WeightUnit.toKilograms(base, units), exercise.equipment)
    }

    fun ceilingKg(equipment: Equipment): Float = when (equipment) {
        Equipment.BARBELL -> 250f
        Equipment.DUMBBELL -> 50f
        Equipment.KETTLEBELL -> 48f
        Equipment.MACHINE -> 200f
        Equipment.PLATE -> 25f
        else -> 40f
    }

    private fun snapToRungs(kg: Float, rungs: List<Float>, units: UnitSystem, how: Snap): Float {
        val shown = WeightUnit.forDisplay(kg, units)
        val pick = when (how) {
            Snap.DOWN -> rungs.lastOrNull { it <= shown + TOLERANCE } ?: rungs.first()
            Snap.UP -> rungs.firstOrNull { it >= shown - TOLERANCE } ?: rungs.last()
            Snap.NEAREST -> rungs.minBy { kotlin.math.abs(it - shown) }
        }
        return WeightUnit.toKilograms(pick, units)
    }

    private fun smallestStepKg(exercise: CatalogExercise, units: UnitSystem): Float {
        rungsFor(exercise.equipment, units)?.let { rungs ->
            val gap = rungs.zipWithNext().minOf { (a, b) -> b - a }
            return WeightUnit.toKilograms(gap, units) - WeightUnit.toKilograms(0f, units)
        }
        val (_, step) = baseAndStep(exercise.equipment, units)
        return WeightUnit.toKilograms(step, units)
    }

    private fun baseAndStep(equipment: Equipment, units: UnitSystem): Pair<Float, Float> =
        when (units) {
            UnitSystem.METRIC -> when (equipment) {
                Equipment.BARBELL -> 20f to 2.5f
                Equipment.DUMBBELL -> 2.5f to 2.5f
                Equipment.MACHINE -> 5f to 5f
                else -> 1.25f to 1.25f
            }

            UnitSystem.IMPERIAL -> when (equipment) {
                Equipment.BARBELL -> 45f to 5f
                Equipment.DUMBBELL -> 5f to 5f
                Equipment.MACHINE -> 10f to 10f
                else -> 2.5f to 2.5f
            }
        }

    // Bells are cast, not assembled, so their sizes are a list rather than a
    // step. The imperial rungs are the sizes American racks are stocked in,
    // not the metric ones converted.
    private fun rungsFor(equipment: Equipment, units: UnitSystem): List<Float>? =
        if (equipment != Equipment.KETTLEBELL) {
            null
        } else {
            when (units) {
                UnitSystem.METRIC -> MetricBells
                UnitSystem.IMPERIAL -> ImperialBells
            }
        }

    private fun capped(kg: Float, equipment: Equipment): Float =
        kg.coerceIn(MIN_WEIGHT_KG, ceilingKg(equipment))

    private val MetricBells =
        listOf(4f, 6f, 8f, 10f, 12f, 16f, 20f, 24f, 28f, 32f, 36f, 40f, 48f)
    private val ImperialBells =
        listOf(10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f, 60f, 70f, 80f, 90f, 105f)

    private const val MIN_WEIGHT_KG = 0.5f
    private const val TOLERANCE = 0.001f
}
