package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.generation.Prescription
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.Injury

data class ExerciseUi(
    val position: Int,
    val name: String,
    val description: String,
    val minutes: Int,
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
    // Counted per side, so the chip says so.
    val unilateral: Boolean = false,
    // The injury the client declared that this movement asks care with.
    val caution: Injury? = null,
    val isCompleted: Boolean = false
) {
    // Read off the sets rather than stored beside them, so a set added or
    // taken away on the day re-reads.
    val prescription: Prescription get() = Prescription.of(sets, measure, unilateral)

    // A weight never lifted before is the app's guess from the profile, and
    // the card says so.
    val isEstimated: Boolean
        get() = measure == ExerciseMeasure.WEIGHT_AND_REPS && previousSets.isEmpty() &&
            sets.any { it.targetWeightKg != null }
}
