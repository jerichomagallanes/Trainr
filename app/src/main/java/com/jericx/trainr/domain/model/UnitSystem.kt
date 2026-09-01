package com.jericx.trainr.domain.model

import com.jericx.trainr.common.Constants
import kotlin.math.roundToInt

// Which units the client reads and writes in. Storage is always kilograms, so
// this decides presentation and nothing else: changing it re-renders the app
// rather than rewriting a single logged set.
enum class UnitSystem {
    METRIC,
    IMPERIAL;

    companion object {
        val Default = METRIC
    }
}

// Kit whose weight is written on it. A client with only these has plates to
// read, and is therefore worth asking which units they are marked in; one
// training on a pull-up bar and a mat has nothing to read.
val LoadedEquipment = setOf(
    Equipment.DUMBBELLS,
    Equipment.BARBELL,
    Equipment.KETTLEBELLS,
    Equipment.CABLE_MACHINE
)

// Loads are stored in kilograms because that is what the model prescribes and
// what history is compared in. These convert at the edges.
object WeightUnit {

    // A prescribed load, moved to the nearest weight the client can actually
    // make. A gym in pounds has no 44.1 lb dumbbell, so 20 kg straight off the
    // model names a weight that does not exist; 45 lb does. Snapping the
    // prescription rather than its display keeps the number the client is shown
    // and the number that gets logged the same one.
    fun loadable(kg: Float, units: UnitSystem): Float = when (units) {
        // Kilograms are prescribed for a gym graduated in kilograms, so there
        // is nothing to move them onto.
        UnitSystem.METRIC -> kg
        UnitSystem.IMPERIAL -> {
            val pounds = kg * Constants.Workout.KG_TO_LBS
            toKilograms((pounds / POUND_STEP).roundToInt() * POUND_STEP.toFloat(), units)
        }
    }

    // Kilograms in the client's own units. Rounded only far enough to shed the
    // noise of converting twice: a logged set is a record of what was lifted and
    // must read back as the number that was typed.
    fun forDisplay(kg: Float, units: UnitSystem): Float {
        val shown = when (units) {
            UnitSystem.METRIC -> kg
            UnitSystem.IMPERIAL -> kg * Constants.Workout.KG_TO_LBS
        }
        return (shown * PRECISION).roundToInt() / PRECISION
    }

    // What the client typed, in kilograms.
    fun toKilograms(entered: Float, units: UnitSystem): Float = when (units) {
        UnitSystem.METRIC -> entered
        UnitSystem.IMPERIAL -> entered / Constants.Workout.KG_TO_LBS
    }

    private const val POUND_STEP = 5
    private const val PRECISION = 100f
}
