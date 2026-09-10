package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet

// `detail` is the prescription chip ("3 sets of 20 reps"), carried here because
// WorkoutExercise has one `duration` field and cannot hold both a total and a
// per-set duration.
data class ExerciseUi(
    val position: Int,
    val name: String,
    val description: String,
    val minutes: Int,
    val detail: String,
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    val sets: List<ExerciseSet> = emptyList(),
    // Sets from the last completed day with this movement, matched on exerciseKey.
    val previousSets: List<ExerciseSet> = emptyList(),
    val videoUrl: String? = null,
    // What the movement trains and how to perform it, both owned by the
    // catalog rather than the model that wrote the week.
    val primaryMuscle: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val isCompleted: Boolean = false
)
