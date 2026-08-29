package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.components.WeekProgressCard
import com.jericx.trainr.presentation.workout.model.WeekProgressUi
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter

@Composable
fun WeeklyProgressScreen(
    weeks: List<WeekProgressUi>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onWeekClick: (WeekProgressUi) -> Unit = {}
) {
    val locale = LocalLocale.current.platformLocale

    Column(modifier = modifier.fillMaxSize()) {
        TrainrTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.screen)
        ) {
            Text(
                text = stringResource(R.string.weekly_progress),
                style = MaterialTheme.typography.titleLarge,
                color = Slate800
            )

            HorizontalDivider()

            weeks.forEach { week ->
                WeekProgressCard(
                    weekNumber = week.weekNumber,
                    dateRange = WorkoutDateFormatter.formatWeekRange(
                        startMillis = SampleWeeklyProgress.weekStartMillis(week.weekNumber),
                        endMillis = SampleWeeklyProgress.weekEndMillis(week.weekNumber),
                        locale = locale,
                        abbreviated = true
                    ),
                    completedDays = week.completedDays,
                    totalDays = week.totalDays,
                    completionPercentage = week.completionPercentage,
                    status = week.status,
                    onClick = { onWeekClick(week) }
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun WeeklyProgressScreenPreview() {
    TrainrTheme {
        WeeklyProgressScreen(weeks = SampleWeeklyProgress.weeks)
    }
}
