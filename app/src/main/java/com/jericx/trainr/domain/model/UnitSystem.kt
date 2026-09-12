package com.jericx.trainr.domain.model

import com.jericx.trainr.common.Constants
import kotlin.math.roundToInt

// Presentation only: changing this re-renders the app rather than rewriting a
// single logged set.
enum class UnitSystem {
    METRIC,
    IMPERIAL;

    companion object {
        val Default = METRIC
    }
}

// Kit with its weight written on it, and so worth asking which units the
// client's gym marks it in.
val LoadedEquipment = setOf(
    Equipment.BARBELL,
    Equipment.DUMBBELL,
    Equipment.KETTLEBELL,
    Equipment.MACHINE,
    Equipment.PLATE
)

// Loads are stored in kilograms, which is what the engine prescribes and what
// history is compared in. These convert at the edges.
object WeightUnit {

    // A prescribed load snapped onto a weight the client's gym actually has.
    // Snapping the prescription, not its display, keeps shown and logged equal.
    fun loadable(kg: Float, units: UnitSystem): Float = when (units) {
        UnitSystem.METRIC -> kg
        UnitSystem.IMPERIAL -> {
            val pounds = kg * Constants.Workout.KG_TO_LBS
            toKilograms((pounds / POUND_STEP).roundToInt() * POUND_STEP.toFloat(), units)
        }
    }

    // Rounded only far enough to shed the noise of converting twice: a logged
    // set must read back as the number that was typed.
    fun forDisplay(kg: Float, units: UnitSystem): Float {
        val shown = when (units) {
            UnitSystem.METRIC -> kg
            UnitSystem.IMPERIAL -> kg * Constants.Workout.KG_TO_LBS
        }
        return (shown * PRECISION).roundToInt() / PRECISION
    }

    fun toKilograms(entered: Float, units: UnitSystem): Float = when (units) {
        UnitSystem.METRIC -> entered
        UnitSystem.IMPERIAL -> entered / Constants.Workout.KG_TO_LBS
    }

    private const val POUND_STEP = 5
    private const val PRECISION = 100f
}
