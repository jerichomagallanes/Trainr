package com.jericx.trainr.presentation.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun DayCompletedScreen(
    dayNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onBackToRoutineClick: () -> Unit = {}
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
        onPrimaryClick = onBackToRoutineClick
    )
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun DayCompletedScreenPreview() {
    TrainrTheme {
        DayCompletedScreen(dayNumber = 2)
    }
}
