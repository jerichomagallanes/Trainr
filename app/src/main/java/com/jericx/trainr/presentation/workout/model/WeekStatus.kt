package com.jericx.trainr.presentation.workout.model

import androidx.annotation.StringRes
import com.jericx.trainr.R

enum class WeekStatus {
    COMPLETED,
    IN_PROGRESS,
    NOT_COMPLETED,
    SKIPPED,
    UPCOMING
}

@get:StringRes
val WeekStatus.labelRes: Int
    get() = when (this) {
        WeekStatus.COMPLETED -> R.string.completed
        WeekStatus.IN_PROGRESS -> R.string.in_progress
        WeekStatus.NOT_COMPLETED -> R.string.not_completed
        WeekStatus.SKIPPED -> R.string.skipped
        WeekStatus.UPCOMING -> R.string.upcoming
    }

val WeekStatus.chipTone: StatusTone
    get() = when (this) {
        WeekStatus.COMPLETED -> StatusTone.DONE
        WeekStatus.IN_PROGRESS -> StatusTone.ACTIVE
        WeekStatus.NOT_COMPLETED, WeekStatus.SKIPPED, WeekStatus.UPCOMING -> StatusTone.IDLE
    }
