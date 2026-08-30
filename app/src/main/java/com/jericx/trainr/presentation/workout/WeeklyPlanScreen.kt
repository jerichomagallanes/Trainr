package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.theme.Orange500
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.components.ReorderableDayList
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter

@Composable
fun WeeklyPlanRoute(
    onDayClick: (WorkoutDay) -> Unit = {},
    onTrackProgressClick: () -> Unit = {},
    onStartTodayClick: (WorkoutDay) -> Unit = {},
    onLeavePlanConfirmed: () -> Unit = {},
    onUpdateProfileClick: () -> Unit = {},
    onStartNextWeekClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    viewModel: WeeklyPlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Coming back from a routine re-reads the plan, so a day completed there
    // is reflected here.
    LaunchedEffect(Unit) { viewModel.refresh() }

    WeeklyPlanScreen(
        state = state,
        onDayClick = onDayClick,
        onTrackProgressClick = onTrackProgressClick,
        onStartTodayClick = onStartTodayClick,
        onLeavePlanConfirmed = onLeavePlanConfirmed,
        onUpdateProfileClick = onUpdateProfileClick,
        onStartNextWeekClick = onStartNextWeekClick,
        onMoveDay = viewModel::moveDay,
        onBackClick = onBackClick
    )
}

@Composable
fun WeeklyPlanScreen(
    state: WeeklyPlanUiState,
    modifier: Modifier = Modifier,
    onDayClick: (WorkoutDay) -> Unit = {},
    onTrackProgressClick: () -> Unit = {},
    onStartTodayClick: (WorkoutDay) -> Unit = {},
    onLeavePlanConfirmed: () -> Unit = {},
    onUpdateProfileClick: () -> Unit = {},
    onStartNextWeekClick: () -> Unit = {},
    onMoveDay: (Int, Int) -> Unit = { _, _ -> },
    // Set only when a week was opened from Weekly Progress.
    onBackClick: (() -> Unit)? = null
) {
    // Such a week is a look at the record rather than the plan being trained,
    // so it gets a way back and drops the actions that belong to the plan
    // standing in as home: regenerating, starting today, and the progress link
    // that leads back where the reader just came from.
    val isBrowsedWeek = onBackClick != null
    val locale = LocalLocale.current.platformLocale
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showLeaveDialog) {
        LeavePlanDialog(
            onConfirm = {
                showLeaveDialog = false
                onLeavePlanConfirmed()
            },
            onDismiss = { showLeaveDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Home has nowhere to go back to. Plan-level actions live behind the
        // heading's overflow, where the design puts them.
        TrainrTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.your_weekly_workout_plan),
                    style = MaterialTheme.typography.titleLarge,
                    color = Slate800,
                    modifier = Modifier.weight(1f)
                )
                if (!isBrowsedWeek) {
                    Box {
                        Image(
                            painter = painterResource(R.drawable.ic_more_horiz),
                            contentDescription = stringResource(R.string.plan_options),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Editing the profile keeps the plan and its
                            // history; regenerating is the deliberate restart.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.update_profile)) },
                                onClick = {
                                    showMenu = false
                                    onUpdateProfileClick()
                                }
                            )
                            if (state.canStartNextWeek) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.start_next_week)) },
                                    onClick = {
                                        showMenu = false
                                        onStartNextWeekClick()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.regenerate_plan)) },
                                onClick = {
                                    showMenu = false
                                    showLeaveDialog = true
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_calendar_today),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(Spacing.small))
                Text(
                    text = stringResource(
                        R.string.week_range_format,
                        state.plan.weekNumber,
                        WorkoutDateFormatter.formatWeekRange(
                            startMillis = state.weekStartMillis,
                            endMillis = state.weekEndMillis,
                            locale = locale
                        )
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate800
                )
            }

            if (!isBrowsedWeek) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onTrackProgressClick)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_moving),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.small))
                    Text(
                        text = stringResource(R.string.track_weekly_progress) + " →",
                        style = MaterialTheme.typography.labelLarge,
                        color = Orange500
                    )
                }
            }

            ReorderableDayList(
                days = state.days,
                locale = locale,
                onDayClick = onDayClick,
                // A week being read back is a record; only the plan you are
                // training can be rescheduled.
                onMove = if (isBrowsedWeek) { _, _ -> } else onMoveDay
            )
        }

        if (!isBrowsedWeek) {
            TrainrButton(
                text = stringResource(R.string.start_todays_workout),
                onClick = { state.todaysDay?.let(onStartTodayClick) },
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.medium)
            )
        }
    }
}

@Composable
private fun LeavePlanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.leave_plan_title)) },
        text = { Text(text = stringResource(R.string.leave_plan_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.leave_plan_confirm), color = Orange500)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = Slate800)
            }
        }
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyPlanScreenPreview() {
    TrainrTheme {
        WeeklyPlanScreen(
            state = WeeklyPlanViewModel.stateFor(
                plan = SampleWorkoutData.weekOne,
                isSample = true
            )
        )
    }
}
