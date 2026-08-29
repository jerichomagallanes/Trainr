package com.jericx.trainr.presentation.workout.model

import androidx.compose.ui.graphics.Color
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.StatusCompleted

enum class ExerciseStep {
    COMPLETED,
    CURRENT,
    UPCOMING
}

// Where you are wins over whether it is ticked: revisiting a finished exercise
// still reads as the current step.
fun RoutineUi.stepsFor(currentPosition: Int): List<ExerciseStep> = exercises.map {
    when {
        it.position == currentPosition -> ExerciseStep.CURRENT
        it.isCompleted -> ExerciseStep.COMPLETED
        else -> ExerciseStep.UPCOMING
    }
}

val ExerciseStep.color: Color
    get() = when (this) {
        ExerciseStep.COMPLETED -> StatusCompleted
        ExerciseStep.CURRENT -> Slate800
        ExerciseStep.UPCOMING -> OutlineGray
    }
