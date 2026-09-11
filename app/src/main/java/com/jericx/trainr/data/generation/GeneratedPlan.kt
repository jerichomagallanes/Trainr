package com.jericx.trainr.data.generation

import kotlinx.serialization.Serializable

// The generator writes only these fields; everything else on the domain model
// is app state or derived. docs/generation-contract.md annotates them.
@Serializable
data class GeneratedPlan(
    val title: String,
    val days: List<GeneratedDay>
)

@Serializable
data class GeneratedDay(
    val dayNumber: Int,
    val title: String,
    val exercises: List<GeneratedExercise>
)

// The chip and the copy default to blank: the app now works both out itself,
// and only the remote model still writes them.
@Serializable
data class GeneratedExercise(
    val exerciseKey: String,
    val prescription: String = "",
    val instructions: String = "",
    val restSeconds: Int? = null,
    val sets: List<GeneratedSet>
)

@Serializable
data class GeneratedSet(
    val reps: Int? = null,
    val weightKg: Float? = null,
    val seconds: Int? = null
)
