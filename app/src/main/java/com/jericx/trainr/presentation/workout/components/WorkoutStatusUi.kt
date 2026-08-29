package com.jericx.trainr.presentation.workout.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.StatusCompleted
import com.jericx.trainr.presentation.common.theme.StatusInProgress
import com.jericx.trainr.presentation.common.theme.StatusNotStarted

@get:StringRes
val WorkoutStatus.labelRes: Int
    get() = when (this) {
        WorkoutStatus.COMPLETED -> R.string.completed
        WorkoutStatus.IN_PROGRESS -> R.string.in_progress
        WorkoutStatus.NOT_STARTED -> R.string.not_started
    }

val WorkoutStatus.chipColor: Color
    get() = when (this) {
        WorkoutStatus.COMPLETED -> StatusCompleted
        WorkoutStatus.IN_PROGRESS -> StatusInProgress
        WorkoutStatus.NOT_STARTED -> StatusNotStarted
    }
