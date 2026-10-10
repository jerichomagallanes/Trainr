package com.jericx.trainr.domain.unstuck

enum class FinishKind { FULL, PARTIAL }

data class SessionOutcome(
    val id: Long = 0,
    val workoutDayId: Long,
    val finishKind: FinishKind,
    val finishedAt: Long,
    val performedSetCount: Int,
    val plannedSetCount: Int
)
