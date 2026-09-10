package com.jericx.trainr.domain.catalog

import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure

// One movement, described well enough that the app can decide whether a client
// can perform it and what it trains, rather than asking a model to assert both.
data class CatalogExercise(
    val key: String,
    val name: String,
    val primary: MuscleGroup,
    // A movement can assist several; Around The World names three.
    val secondary: List<MuscleGroup>,
    val equipment: Equipment,
    val measure: ExerciseMeasure,
    val pattern: MovementPattern,
    val staple: Boolean,
    // Reps are performed on one side and repeated on the other, so the set
    // costs twice the time and the chip has to say so. A walking lunge
    // alternates inside the set and is not one of these.
    val unilateral: Boolean = false,
    // One line for the card and the how-to behind a tap, both owned by the
    // catalog so a form cue can never be generated. Empty where the movement
    // is an activity with no technique to describe.
    val summary: String = "",
    val steps: List<String> = emptyList()
) {
    // Bodyweight needs nothing, so it is available to everyone.
    fun isAvailableWith(owned: Set<Equipment>): Boolean =
        equipment == Equipment.NONE || equipment in owned
}

interface ExerciseCatalog {
    val all: List<CatalogExercise>

    operator fun get(key: String): CatalogExercise?

    fun availableWith(owned: Set<Equipment>): List<CatalogExercise> =
        all.filter { it.isAvailableWith(owned) }
}

class InMemoryExerciseCatalog(override val all: List<CatalogExercise>) : ExerciseCatalog {

    private val byKey = all.associateBy { it.key }

    override fun get(key: String): CatalogExercise? = byKey[key]
}

// What a movement is for, which decides its rep window, its rest and where it
// sits in a session. Derived rather than stored, so the catalog has one fewer
// field to keep true.
enum class ExerciseRole { COMPOUND, ISOLATION, TIMED }

// The measure settles it before the pattern gets a say: a clean is tagged
// CONDITIONING and is still a loaded multi-joint lift, and prescribing it in
// seconds would be nonsense.
val CatalogExercise.role: ExerciseRole
    get() = when {
        measure == ExerciseMeasure.DURATION -> ExerciseRole.TIMED
        pattern == MovementPattern.ISOLATION || pattern == MovementPattern.CORE ->
            ExerciseRole.ISOLATION
        else -> ExerciseRole.COMPOUND
    }

// Whether a load can be prescribed at all. It follows the measure, not the
// equipment: an assisted pull-up is on a machine and still has no weight to
// choose.
val CatalogExercise.isLoadable: Boolean
    get() = measure == ExerciseMeasure.WEIGHT_AND_REPS
