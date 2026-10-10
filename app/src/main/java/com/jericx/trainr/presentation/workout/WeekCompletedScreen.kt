package com.jericx.trainr.presentation.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.unstuck.feedback.FeedbackPromptViewModel
import com.jericx.trainr.presentation.workout.components.AnythingToChangeCard

@Composable
fun WeekCompletedRoute(
    weekNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onPreviewNextWeekClick: () -> Unit = {},
    onFeedback: (Long) -> Unit = {},
    onLeaveNote: () -> Unit = {},
    viewModel: FeedbackPromptViewModel = hiltViewModel()
) {
    val pending by viewModel.pendingAdjustmentId.collectAsStateWithLifecycle()

    WeekCompletedScreen(
        weekNumber = weekNumber,
        modifier = modifier,
        onBackClick = onBackClick,
        onViewProgressClick = onViewProgressClick,
        onPreviewNextWeekClick = onPreviewNextWeekClick,
        extra = {
            AnythingToChangeCard(
                onLeaveNote = onLeaveNote,
                onFeedback = pending?.let { adjustmentId -> { onFeedback(adjustmentId) } }
            )
        }
    )
}

@Composable
fun WeekCompletedScreen(
    weekNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onPreviewNextWeekClick: () -> Unit = {},
    extra: @Composable () -> Unit = {}
) {
    CompletionScreen(
        iconRes = R.drawable.ic_trophy,
        iconSize = 100.dp,
        title = stringResource(R.string.week_completed_format, weekNumber),
        message = stringResource(R.string.week_completed_message),
        secondaryLabel = stringResource(R.string.view_weekly_progress),
        primaryLabel = stringResource(R.string.generate_next_week),
        modifier = modifier,
        onBackClick = onBackClick,
        onSecondaryClick = onViewProgressClick,
        onPrimaryClick = onPreviewNextWeekClick,
        extra = extra
    )
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun WeekCompletedScreenPreview() {
    TrainrTheme {
        WeekCompletedScreen(weekNumber = 1)
    }
}
