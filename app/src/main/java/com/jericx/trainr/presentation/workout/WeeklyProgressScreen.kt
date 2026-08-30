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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
fun WeeklyProgressRoute(
    onBackClick: () -> Unit = {},
    onWeekClick: (WeekProgressUi) -> Unit = {},
    viewModel: WeeklyProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Coming back from a routine re-reads the plans, so a day completed there
    // is reflected here.
    LaunchedEffect(Unit) { viewModel.refresh() }

    WeeklyProgressScreen(
        weeks = state.weeks,
        onBackClick = onBackClick,
        // Sample weeks stand for nothing stored, so they lead nowhere.
        onWeekClick = { if (!state.isSampleData) onWeekClick(it) }
    )
}

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
                        startMillis = week.startDateMillis,
                        endMillis = week.endDateMillis,
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
