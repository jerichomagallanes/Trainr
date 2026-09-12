package com.jericx.trainr.data.generation

// The generator writes only these fields; everything else on the domain model
// is app state or derived. docs/generation-contract.md annotates them.
data class GeneratedPlan(
    val title: String,
    val days: List<GeneratedDay>
)

data class GeneratedDay(
    val dayNumber: Int,
    val title: String,
    val exercises: List<GeneratedExercise>
)

data class GeneratedExercise(
    val exerciseKey: String,
    val restSeconds: Int? = null,
    val sets: List<GeneratedSet>
)

data class GeneratedSet(
    val reps: Int? = null,
    val weightKg: Float? = null,
    val seconds: Int? = null
)
