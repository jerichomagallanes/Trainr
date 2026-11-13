package com.jericx.trainr.presentation.workout.model

data class ExpandedTutorialUi(
    val position: Int,
    val exerciseId: Long
)

fun ExpandedTutorialUi?.isOn(exercise: ExerciseUi): Boolean =
    this != null && position == exercise.position && exerciseId == exercise.exerciseId
