package com.jericx.trainr.domain.catalog

import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure

// One movement, described well enough that the app can decide whether a client
// can perform it and what it trains, rather than asking a model to assert both.
data class CatalogExercise(
    val key: String,
    val name: String,
    val nameJa: String,
    val muscle: MuscleGroup,
    val requires: Set<Equipment>,
    val measure: ExerciseMeasure,
    val pattern: MovementPattern,
    val staple: Boolean
) {
    fun displayName(languageCode: String): String =
        if (languageCode == JAPANESE) nameJa.ifBlank { name } else name

    // Bodyweight needs nothing, so it is available to everyone; everything else
    // needs every item it lists, not any one of them.
    fun isAvailableWith(owned: Set<Equipment>): Boolean =
        requires == BODYWEIGHT || owned.containsAll(requires)

    private companion object {
        const val JAPANESE = "ja"
        val BODYWEIGHT = setOf(Equipment.NONE)
    }
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
