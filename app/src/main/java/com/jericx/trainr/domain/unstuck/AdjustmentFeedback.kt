package com.jericx.trainr.domain.unstuck

enum class FeedbackAnswer { HELPED, NOT_QUITE, STILL_TOO_LONG, EXERCISE_CONFUSING, SOMETHING_ELSE, DISCOMFORT }

data class AdjustmentFeedback(
    val id: Long = 0,
    val adjustmentId: Long,
    val answer: FeedbackAnswer?,
    val answeredAt: Long?,
    val dismissedAt: Long?
)
