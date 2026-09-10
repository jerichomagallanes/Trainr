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
    // How to perform it, written once and the same for everyone. Empty where
    // the movement is an activity with no technique to describe.
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
