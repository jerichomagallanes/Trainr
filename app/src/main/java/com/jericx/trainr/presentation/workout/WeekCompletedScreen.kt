package com.jericx.trainr.presentation.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun WeekCompletedScreen(
    weekNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onPreviewNextWeekClick: () -> Unit = {}
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
        onPrimaryClick = onPreviewNextWeekClick
    )
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun WeekCompletedScreenPreview() {
    TrainrTheme {
        WeekCompletedScreen(weekNumber = 1)
    }
}
