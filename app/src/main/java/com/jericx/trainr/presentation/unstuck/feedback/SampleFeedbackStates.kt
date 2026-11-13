package com.jericx.trainr.presentation.unstuck.feedback

import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.FeedbackAnswer

object SampleFeedbackStates {

    val time = AdjustmentFeedbackUiState(
        isLoaded = true,
        trendLabelRes = R.string.trend_training_performance
    )

    val equipment = AdjustmentFeedbackUiState(
        isLoaded = true,
        isReplacement = true,
        substituteName = "Lateral Raise (Dumbbell)",
        originalName = "Lateral Raise (Cable)",
        trendLabelRes = R.string.trend_strength,
        guidanceKey = "dumbbell_lateral_raise"
    )

    val helped = time.copy(answer = FeedbackAnswer.HELPED)

    val confusing = equipment.copy(answer = FeedbackAnswer.EXERCISE_CONFUSING)
}
