package com.jericx.trainr.data.catalog

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CatalogFile(
    val version: Int = 1,
    // Read loosely on purpose: a row missing a field is one movement lost, not
    // a catalog, and the integrity test is what keeps the file honest.
    val exercises: List<CatalogEntry> = emptyList()
)

@Serializable
internal data class CatalogEntry(
    val key: String = "",
    val name: String = "",
    val primary: String = "",
    val secondary: List<String> = emptyList(),
    val equipment: String = "",
    val measure: String = "",
    val pattern: String = "",
    val staple: Boolean = false,
    val unilateral: Boolean = false,
    val oneHanded: Boolean = false,
    val summary: String = "",
    val steps: List<String> = emptyList()
)

// An entry the code does not understand is dropped, not fatal: the data file
// is allowed to run ahead of the app that reads it.
object ExerciseCatalogReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(source: String): ExerciseCatalog =
        InMemoryExerciseCatalog(
            json.decodeFromString<CatalogFile>(source).exercises.mapNotNull { it.toDomain() }
        )

    private fun CatalogEntry.toDomain(): CatalogExercise? {
        val prime = enumOrNull<MuscleGroup>(primary) ?: return null
        val measure = enumOrNull<ExerciseMeasure>(measure) ?: return null
        val pattern = enumOrNull<MovementPattern>(pattern) ?: return null
        val kit = enumOrNull<Equipment>(equipment) ?: return null
        if (key.isBlank() || name.isBlank()) return null
        return CatalogExercise(
            key = key,
            name = name,
            primary = prime,
            secondary = secondary.mapNotNull { enumOrNull<MuscleGroup>(it) },
            equipment = kit,
            measure = measure,
            pattern = pattern,
            staple = staple,
            unilateral = unilateral,
            oneHanded = oneHanded,
            summary = summary,
            steps = steps
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value) }.getOrNull()
}
