package com.jericx.trainr.presentation.workout.components

import androidx.annotation.StringRes
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.model.StatusTone

@get:StringRes
val WorkoutStatus.labelRes: Int
    get() = when (this) {
        WorkoutStatus.COMPLETED -> R.string.completed
        WorkoutStatus.IN_PROGRESS -> R.string.status_in_progress
        WorkoutStatus.NOT_STARTED -> R.string.not_started
    }

val WorkoutStatus.chipTone: StatusTone
    get() = when (this) {
        WorkoutStatus.COMPLETED -> StatusTone.DONE
        WorkoutStatus.IN_PROGRESS -> StatusTone.ACTIVE
        WorkoutStatus.NOT_STARTED -> StatusTone.IDLE
    }
