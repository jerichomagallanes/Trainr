package com.jericx.trainr.data.generation

import kotlinx.serialization.Serializable

// The shape a generated weekly plan arrives in. The generator writes only
// these fields; everything else on the domain model is app state or derived.
// docs/generation-contract.md is the annotated version of this file.
@Serializable
data class GeneratedPlan(
    val title: String,
    val days: List<GeneratedDay>
)

@Serializable
data class GeneratedDay(
    val dayNumber: Int,
    val title: String,
    val equipment: List<String> = emptyList(),
    val exercises: List<GeneratedExercise>
)

@Serializable
data class GeneratedExercise(
    val exerciseKey: String,
    val name: String,
    val measure: String,
    val durationMinutes: Int,
    val prescription: String,
    val instructions: String,
    val restSeconds: Int? = null,
    val sets: List<GeneratedSet>
)

@Serializable
data class GeneratedSet(
    val reps: Int? = null,
    val weightKg: Float? = null,
    val seconds: Int? = null
)
