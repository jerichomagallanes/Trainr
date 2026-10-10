package com.jericx.trainr.domain.unstuck

enum class PreferenceKind { TIME_LIMIT }

data class TrainingPreference(
    val id: Long = 0,
    val userId: Long,
    val kind: PreferenceKind,
    val minutes: Int,
    // java.time.DayOfWeek.value, Monday 1 to Sunday 7
    val weekday: Int,
    val sourceAdjustmentId: Long?,
    val confirmedAt: Long,
    val updatedAt: Long
)
