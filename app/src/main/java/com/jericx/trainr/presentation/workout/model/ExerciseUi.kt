package com.jericx.trainr.presentation.workout.model

// `detail` is the prescription chip — "5 minutes", "5 sets of 1 minute",
// "3 sets of 20 reps". WorkoutExercise cannot express it alongside `minutes`:
// it has one `duration` field, but exercise 2 needs both a 10-minute total and
// a 1-minute per-set duration. Carried here until the domain gains a field.
data class ExerciseUi(
    val position: Int,
    val name: String,
    val description: String,
    val minutes: Int,
    val detail: String,
    val isCompleted: Boolean = false
)
