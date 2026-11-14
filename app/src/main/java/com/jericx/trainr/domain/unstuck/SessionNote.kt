package com.jericx.trainr.domain.unstuck

data class SessionNote(
    val id: Long = 0,
    val userId: Long,
    val workoutDayId: Long?,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)
