package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.jericx.trainr.presentation.common.theme.RedError
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrSwipeToDelete
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
        onWeekClick = onWeekClick,
        onDeleteWeek = { viewModel.deleteWeek(it.weekNumber) }
    )
}

@Composable
fun WeeklyProgressScreen(
    weeks: List<WeekProgressUi>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onWeekClick: (WeekProgressUi) -> Unit = {},
    onDeleteWeek: (WeekProgressUi) -> Unit = {}
) {
    val locale = LocalLocale.current.platformLocale
    var weekToDelete by remember { mutableStateOf<WeekProgressUi?>(null) }

    weekToDelete?.let { week ->
        DeleteWeekDialog(
            week = week,
            onConfirm = {
                weekToDelete = null
                onDeleteWeek(week)
            },
            onDismiss = { weekToDelete = null }
        )
    }

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
                DeletableWeek(
                    canDelete = true,
                    onDelete = { weekToDelete = week }
                ) {
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
}

// Only an unstarted week can be swiped away, and the swipe asks before it
// deletes: a week is a good deal more than a set. The delete fires once per
// completed dismissal and the state snaps back, so a card that survives the
// question is not left half open.
@Composable
private fun DeletableWeek(
    canDelete: Boolean,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    TrainrSwipeToDelete(
        onDelete = onDelete,
        contentDescription = stringResource(R.string.delete_week_confirm),
        // Every week slides aside; the dialog is where the weight of it lands.
        enabled = canDelete
    ) {
        content()
    }
}

@Composable
private fun DeleteWeekDialog(
    week: WeekProgressUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.delete_week_title, week.weekNumber))
        },
        text = {
            // Training that was actually done is named before it goes, so the
            // choice is made knowing what it costs.
            Text(
                text = if (week.hasTraining) {
                    stringResource(
                        R.string.delete_week_message_trained,
                        week.completedDays,
                        week.totalDays
                    )
                } else {
                    stringResource(R.string.delete_week_message)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete_week_confirm), color = RedError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = Slate800)
            }
        }
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun WeeklyProgressScreenPreview() {
    TrainrTheme {
        WeeklyProgressScreen(weeks = SampleWeeklyProgress.weeks)
    }
}
