package com.jericx.trainr.domain.unstuck

enum class AdjustmentReason { LESS_TIME, EQUIPMENT_UNAVAILABLE }

data class AppliedAdjustment(
    val id: Long = 0,
    val workoutDayId: Long,
    val proposal: AdjustmentProposal,
    val reason: AdjustmentReason,
    val appliedAt: Long,
    val undoneAt: Long? = null
) {
    val isActive: Boolean get() = undoneAt == null
}
