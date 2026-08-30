package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlin.math.abs
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        onWeekClick = { if (!state.isSampleData) onWeekClick(it) },
        onDeleteWeek = { if (!state.isSampleData) viewModel.deleteWeek(it.weekNumber) }
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
                    week = week,
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
    week: WeekProgressUi,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!week.canDelete) {
        Box(modifier = Modifier.swallowHorizontalDrags()) { content() }
        return
    }

    // Read through rememberUpdatedState: the state below keeps the confirm
    // lambda from its first composition.
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        // The swipe asks rather than deletes, so the card must never settle as
        // dismissed: refusing the change springs it back on its own, and the
        // dialog decides what actually happens. Snapping it back by hand
        // instead loses a race with the swipe's own settle animation and
        // strands the card off-screen. Opening the dialog twice is harmless.
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentOnDelete()
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart &&
                dismissState.progress > 0f && dismissState.progress < 1f
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(RedError),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_week_confirm),
                        tint = Color.White,
                        modifier = Modifier.padding(end = Spacing.medium)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}

// Swiping a week that cannot be deleted would otherwise end as a tap and open
// it, because nothing here consumes the drag. Swallowing it once it passes the
// touch slop keeps a refused swipe from navigating.
private fun Modifier.swallowHorizontalDrags(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var travelled = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: break
            if (change.changedToUpIgnoreConsumed()) break
            travelled += change.positionChangeIgnoreConsumed().x
            if (abs(travelled) > viewConfiguration.touchSlop) change.consume()
        }
    }
}

@Composable
private fun DeleteWeekDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_week_title)) },
        text = { Text(text = stringResource(R.string.delete_week_message)) },
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
