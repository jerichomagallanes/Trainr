package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.unstuck.feedback.FeedbackPromptViewModel

@Composable
fun DayCompletedRoute(
    dayNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onBackToRoutineClick: () -> Unit = {},
    onFeedback: (Long) -> Unit = {},
    viewModel: FeedbackPromptViewModel = hiltViewModel()
) {
    val pending by viewModel.pendingAdjustmentId.collectAsStateWithLifecycle()

    DayCompletedScreen(
        dayNumber = dayNumber,
        modifier = modifier,
        onBackClick = onBackClick,
        onViewProgressClick = onViewProgressClick,
        onBackToRoutineClick = onBackToRoutineClick,
        extra = {
            pending?.let { adjustmentId ->
                TrainrTextAction(
                    text = stringResource(R.string.tell_us_how_it_went),
                    onClick = { onFeedback(adjustmentId) },
                    modifier = Modifier.padding(top = Spacing.card)
                )
            }
        }
    )
}

@Composable
fun DayCompletedScreen(
    dayNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onBackToRoutineClick: () -> Unit = {},
    extra: @Composable () -> Unit = {}
) {
    CompletionScreen(
        iconRes = R.drawable.ic_award_star,
        iconSize = 80.dp,
        title = stringResource(R.string.day_completed_format, dayNumber),
        message = stringResource(R.string.day_completed_message),
        secondaryLabel = stringResource(R.string.view_weekly_progress),
        primaryLabel = stringResource(R.string.back_to_workout_plan),
        modifier = modifier,
        onBackClick = onBackClick,
        onSecondaryClick = onViewProgressClick,
        onPrimaryClick = onBackToRoutineClick,
        extra = extra
    )
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun DayCompletedScreenPreview() {
    TrainrTheme {
        DayCompletedScreen(dayNumber = 2)
    }
}
