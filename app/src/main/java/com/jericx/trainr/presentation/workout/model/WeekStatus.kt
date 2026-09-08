package com.jericx.trainr.presentation.workout.model

import androidx.annotation.StringRes
import com.jericx.trainr.R

// A week is coarser than a workout: it can also be missed entirely (SKIPPED),
// ended part-done (NOT_COMPLETED), or generated ahead of its start date
// (UPCOMING) — none of which WorkoutStatus expresses.
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

// Five labels, three tones: the design shows missed and part-done weeks alike.
val WeekStatus.chipTone: StatusTone
    get() = when (this) {
        WeekStatus.COMPLETED -> StatusTone.DONE
        WeekStatus.IN_PROGRESS -> StatusTone.ACTIVE
        WeekStatus.NOT_COMPLETED, WeekStatus.SKIPPED, WeekStatus.UPCOMING -> StatusTone.IDLE
    }
