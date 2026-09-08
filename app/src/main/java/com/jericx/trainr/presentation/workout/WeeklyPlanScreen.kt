package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.data.preferences.AppearanceMode
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrRadioDot
import androidx.compose.ui.text.style.TextAlign
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
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
    onRepeatWeekClick: () -> Unit = {},
    onRegenerateWeekClick: () -> Unit = {},
    onCreatePlanClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    versionName: String = "",
    appearance: AppearanceMode = AppearanceMode.SYSTEM,
    onAppearanceChange: (AppearanceMode) -> Unit = {},
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
        onRepeatWeekClick = onRepeatWeekClick,
        onRegenerateWeekClick = onRegenerateWeekClick,
        onCreatePlanClick = onCreatePlanClick,
        versionName = versionName,
        appearance = appearance,
        onAppearanceChange = onAppearanceChange,
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
    onRepeatWeekClick: () -> Unit = {},
    onRegenerateWeekClick: () -> Unit = {},
    onCreatePlanClick: () -> Unit = {},
    versionName: String = "",
    appearance: AppearanceMode = AppearanceMode.SYSTEM,
    onAppearanceChange: (AppearanceMode) -> Unit = {},
    onMoveDay: (Int, Int) -> Unit = { _, _ -> },
    // Set only when a week was opened from Weekly Progress.
    onBackClick: (() -> Unit)? = null
) {
    // Such a week is a look at the record rather than the plan being trained,
    // so it gets a way back and drops the actions that belong to the plan
    // standing in as home: regenerating, starting today, and the progress link
    // that leads back where the reader just came from.
    // A week already behind you is a record: it keeps its dates and its order,
    // and offers none of the actions that belong to the week being trained. The
    // newest week is live wherever it was opened from, so home and the list
    // show the same thing rather than two versions of it.
    val isBrowsedWeek = state.hasPlan && !state.isCurrentWeek

    // Two different questions, and they were being answered by one flag. Whether
    // the week is live decides what may be done to its contents. Whether this is
    // home decides what may be done to the plan as a whole: a week opened from
    // the list is a week you went to see, so it does not carry the actions that
    // rebuild the plan, nor a link back to the list you came from. Those read as
    // offers here and did nothing, because the route that opens a week has no
    // plan-level callbacks to give them.
    val isHome = onBackClick == null
    val locale = LocalLocale.current.platformLocale
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showRegenerateDialog) {
        RegenerateWeekDialog(
            loggedWorkouts = state.days.count { it.day.status == WorkoutStatus.COMPLETED },
            totalWorkouts = state.days.size,
            onConfirm = {
                showRegenerateDialog = false
                onRegenerateWeekClick()
            },
            onDismiss = { showRegenerateDialog = false }
        )
    }

    if (showLeaveDialog) {
        LeavePlanDialog(
            onConfirm = {
                showLeaveDialog = false
                onLeavePlanConfirmed()
            },
            onDismiss = { showLeaveDialog = false }
        )
    }

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Home has nowhere to go back to. Plan-level actions live behind the
        // heading's overflow, where the design puts them; who you are and what
        // the app is belong to home's app bar, and stay reachable there even
        // when there is no plan for the overflow to hang off.
        TrainrTopBar(
            onBackClick = onBackClick,
            actions = {
                // Who you are belongs to home, not to a week you opened from
                // somewhere else: a screen with a way back is somewhere you
                // went, and the account is not part of what you went to see.
                if (onBackClick == null) {
                    ProfileMenu(
                        versionName = versionName,
                        onUpdateProfileClick = onUpdateProfileClick,
                        appearance = appearance,
                        onAppearanceChange = onAppearanceChange
                    )
                }
            }
        )

        // Nothing is drawn until the plan has been looked for: a blank moment
        // is honest, where a stand-in week would be read as the real thing.
        if (!state.hasLoaded) return@Column

        if (!state.hasPlan) {
            NoPlanYet(
                onCreatePlanClick = onCreatePlanClick,
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.your_weekly_workout_plan),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.trainrColors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // Repeating is the one action a week can offer about itself:
                // its subject is the week you are looking at, not the plan, so
                // it belongs on whichever week that is. Building the next week
                // and starting over are about the plan's future, and stay on
                // home, which is where they have somewhere to go afterwards.
                if (state.hasPlan && (isHome || state.canAddWeek || !isBrowsedWeek)) {
                    Box {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_horiz),
                            contentDescription = stringResource(R.string.plan_options),
                            tint = MaterialTheme.trainrColors.onSurface,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = MaterialTheme.trainrColors.surfaceRaised,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.trainrColors.raisedEdge
                            )
                        ) {
                            // With the week behind you there are two sound
                            // ways on: progress from what you lifted, or run
                            // the same week again. The second is a coaching
                            // decision, so it is offered rather than assumed.
                            if (isHome && state.canStartNextWeek) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.generate_next_week)) },
                                    onClick = {
                                        showMenu = false
                                        onStartNextWeekClick()
                                    }
                                )
                            }
                            // Any week can be run again, this one or one from
                            // months ago; the copy joins the plan at the end,
                            // which is why it waits for the same moment as a
                            // generated week rather than landing on top of one
                            // still being trained.
                            if (state.canAddWeek) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.repeat_this_week)) },
                                    onClick = {
                                        showMenu = false
                                        onRepeatWeekClick()
                                    }
                                )
                            }
                            // The other half of the same question: still in
                            // this week, so it can be written again; done with
                            // it, and the offer becomes the week that follows.
                            if (!state.canAddWeek) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.regenerate_week)) },
                                    onClick = {
                                        showMenu = false
                                        if (state.days.any {
                                                it.day.status == WorkoutStatus.COMPLETED
                                            }
                                        ) {
                                            showRegenerateDialog = true
                                        } else {
                                            onRegenerateWeekClick()
                                        }
                                    }
                                )
                            }
                            if (isHome) {
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
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar_today),
                    contentDescription = null,
                    tint = MaterialTheme.trainrColors.onSurface,
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
                    color = MaterialTheme.trainrColors.onSurface
                )
            }

            if (isHome) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onTrackProgressClick)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_moving),
                        contentDescription = null,
                        tint = MaterialTheme.trainrColors.brandStrong,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.small))
                    Text(
                        text = stringResource(R.string.track_weekly_progress) + " →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.trainrColors.brandStrong
                    )
                }
            }

            ReorderableDayList(
                days = state.days,
                locale = locale,
                onDayClick = onDayClick,
                // A week being read back is a record; only the plan you are
                // training can be rescheduled.
                canReorder = !isBrowsedWeek,
                onMove = onMoveDay,
                scrollState = scrollState
            )
        }

        // Whatever is actually left: a session to train, or — with the week
        // behind you — the week that follows it. Never a finished session
        // dressed as the next one. Training is offered wherever the live week
        // was opened from; building the next one is home's business.
        val next = state.nextWorkout
        when {
            next != null && !isBrowsedWeek -> TrainrButton(
                text = stringResource(
                    if (state.nextWorkoutIsToday) R.string.start_todays_workout
                    else R.string.start_next_workout
                ),
                onClick = { onStartTodayClick(next.day) },
                modifier = Modifier
                    .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
            )

            isHome && state.canStartNextWeek -> TrainrButton(
                text = stringResource(R.string.generate_next_week),
                onClick = onStartNextWeekClick,
                modifier = Modifier
                    .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
            )
        }
    }
}

// Who you are and what the app is: the two things that are about the client
// rather than about this week's training, kept out of the plan's own overflow.
@Composable
private fun ProfileMenu(
    versionName: String,
    onUpdateProfileClick: () -> Unit,
    appearance: AppearanceMode,
    onAppearanceChange: (AppearanceMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutDialog(versionName = versionName, onDismiss = { showAbout = false })
    }

    if (showAppearance) {
        AppearanceDialog(
            appearance = appearance,
            onSelect = onAppearanceChange,
            onDismiss = { showAppearance = false }
        )
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.profile_and_app),
                tint = MaterialTheme.trainrColors.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.trainrColors.surfaceRaised,
            border = BorderStroke(1.dp, MaterialTheme.trainrColors.raisedEdge)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.update_profile)) },
                onClick = {
                    expanded = false
                    onUpdateProfileClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.appearance)) },
                onClick = {
                    expanded = false
                    showAppearance = true
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.about_the_app)) },
                onClick = {
                    expanded = false
                    showAbout = true
                }
            )
        }
    }
}

// The chosen appearance is applied as it is tapped rather than on closing, so
// the dialog itself is the preview of what was picked.
@Composable
private fun AppearanceDialog(
    appearance: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.appearance)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                AppearanceOption(
                    icon = painterResource(R.drawable.ic_phone_android),
                    text = stringResource(R.string.appearance_system),
                    selected = appearance == AppearanceMode.SYSTEM,
                    onClick = { onSelect(AppearanceMode.SYSTEM) }
                )
                AppearanceOption(
                    icon = painterResource(R.drawable.ic_light_mode),
                    text = stringResource(R.string.appearance_light),
                    selected = appearance == AppearanceMode.LIGHT,
                    onClick = { onSelect(AppearanceMode.LIGHT) }
                )
                AppearanceOption(
                    icon = painterResource(R.drawable.ic_dark_mode),
                    text = stringResource(R.string.appearance_dark),
                    selected = appearance == AppearanceMode.DARK,
                    onClick = { onSelect(AppearanceMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close), color = MaterialTheme.trainrColors.brandStrong)
            }
        }
    )
}

@Composable
private fun AppearanceOption(
    icon: Painter,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.trainrColors

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentHeight.Option),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.surfaceSelected else colors.surfaceCard,
        border = BorderStroke(1.dp, colors.outlineControl)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (selected) colors.onSurfaceSelected else colors.brand
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) colors.onSurfaceSelected else colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            TrainrRadioDot(selected = selected)
        }
    }
}

@Composable
private fun AboutDialog(versionName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.about_the_app)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(text = stringResource(R.string.app_version_format, versionName))
                Text(text = stringResource(R.string.app_about_message))
                // Kept reachable after onboarding: the review shows it once,
                // and a client training months later has nowhere else to find it.
                Text(text = stringResource(R.string.health_disclaimer))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close), color = MaterialTheme.trainrColors.brandStrong)
            }
        }
    )
}

// Deleting every week is allowed, so landing there has to be a place rather
// than a gap: it says what happened and offers the way out of it.
@Composable
private fun NoPlanYet(
    onCreatePlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_plan_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.trainrColors.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(Spacing.small))

        Text(
            text = stringResource(R.string.no_plan_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.trainrColors.onSurfaceMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(Spacing.sectionGap))

        TrainrButton(
            text = stringResource(R.string.create_my_plan),
            onClick = onCreatePlanClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// A week with nothing logged in it is just a week; one with training in it is a
// record, and what a new week costs is named before it is asked for.
@Composable
private fun RegenerateWeekDialog(
    loggedWorkouts: Int,
    totalWorkouts: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.regenerate_week_title)) },
        text = {
            Text(
                text = pluralStringResource(
                    R.plurals.regenerate_week_message_trained,
                    loggedWorkouts,
                    loggedWorkouts,
                    totalWorkouts
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.regenerate_week_confirm),
                    color = MaterialTheme.trainrColors.dangerInk
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.trainrColors.onSurface)
            }
        }
    )
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
                Text(text = stringResource(R.string.leave_plan_confirm), color = MaterialTheme.trainrColors.brandStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.trainrColors.onSurface)
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
