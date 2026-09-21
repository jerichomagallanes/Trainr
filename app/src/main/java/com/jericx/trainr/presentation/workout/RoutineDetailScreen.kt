package com.jericx.trainr.presentation.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrFieldError
import com.jericx.trainr.presentation.common.components.core.TrainrOptionRow
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.core.TrainrSlideToConfirm
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.workout.components.ExerciseCard
import com.jericx.trainr.presentation.workout.components.ExerciseTimer
import com.jericx.trainr.presentation.workout.components.HowToSection
import com.jericx.trainr.presentation.workout.components.VideoTutorial
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.presentation.unstuck.AdjustTodaySheet
import com.jericx.trainr.presentation.unstuck.joinAnd
import com.jericx.trainr.presentation.unstuck.labelRes
import com.jericx.trainr.presentation.workout.model.AdjustedBannerUi
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.YouTubeVideo
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter

@Composable
fun RoutineDetailRoute(
    onBackClick: () -> Unit = {},
    onDayCompleted: (dayNumber: Int, weekNumber: Int) -> Unit = { _, _ -> },
    onWeekCompleted: (Int) -> Unit = {},
    onSessionSaved: (SessionSavedEvent) -> Unit = {},
    onAdjust: (DirectReason, Long?) -> Unit = { _, _ -> },
    finishEarlyRequested: Boolean = false,
    onFinishEarlyHandled: () -> Unit = {},
    howToRequested: String? = null,
    onHowToHandled: () -> Unit = {},
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Returning from an adjustment brings back a different day, so the stored
    // one is read again every time this route comes back into composition.
    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(viewModel) {
        viewModel.savedEvents.collect { onSessionSaved(it) }
    }

    LaunchedEffect(finishEarlyRequested) {
        if (!finishEarlyRequested) return@LaunchedEffect
        onFinishEarlyHandled()
        viewModel.askToFinishEarly()
    }

    // Waits for the stored day: the exercise to open is found in it, and the
    // request can arrive before the read that comes back with the route.
    LaunchedEffect(howToRequested, state.isLoaded) {
        val key = howToRequested ?: return@LaunchedEffect
        if (!state.isLoaded) return@LaunchedEffect
        onHowToHandled()
        viewModel.showHowToFor(key)
    }

    RoutineDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onOpenAdjustSheet = viewModel::openAdjustSheet,
        onDismissAdjustSheet = viewModel::dismissAdjustSheet,
        onChooseReason = { reason ->
            viewModel.dismissAdjustSheet()
            onAdjust(reason, null)
        },
        onShowHowTo = viewModel::showHowTo,
        onNeedAlternative = { exerciseId -> onAdjust(DirectReason.EQUIPMENT, exerciseId) },
        onUndoAdjustment = viewModel::undoAdjustment,
        onScrolled = viewModel::scrolled,
        onDayCompleted = onDayCompleted,
        onWeekCompleted = onWeekCompleted,
        onAskToFinishEarly = viewModel::askToFinishEarly,
        onKeepTraining = viewModel::keepTraining,
        onFinishEarly = viewModel::finishEarly,
        onRetryFinishEarly = viewModel::retryFinishEarly,
        onToggleExercise = viewModel::toggleExercise,
        onSetChanged = viewModel::updateSet,
        onAddSet = viewModel::addSet,
        onDeleteSet = viewModel::deleteSet,
        onCompleteRoutine = viewModel::completeRoutine,
        onClearProgress = viewModel::clearProgress,
        onStartTimer = viewModel::startTimer,
        onPauseTimer = viewModel::pauseTimer,
        onResumeTimer = viewModel::resumeTimer,
        onResetTimer = viewModel::resetTimer,
        onStopTimer = viewModel::stopTimer,
        onToggleVideo = viewModel::toggleVideo,
        onToggleHowTo = viewModel::toggleHowTo
    )
}

@Composable
fun RoutineDetailScreen(
    state: RoutineDetailUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onToggleExercise: (Int) -> Unit = {},
    onSetChanged: (Int, ExerciseSet) -> Unit = { _, _ -> },
    onAddSet: (Int) -> Unit = {},
    onDeleteSet: (Int, Int) -> Unit = { _, _ -> },
    onCompleteRoutine: () -> Unit = {},
    onClearProgress: () -> Unit = {},
    onStartTimer: (ExerciseUi) -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    onToggleVideo: (Int) -> Unit = {},
    onToggleHowTo: (Int) -> Unit = {},
    onDayCompleted: (dayNumber: Int, weekNumber: Int) -> Unit = { _, _ -> },
    onWeekCompleted: (Int) -> Unit = {},
    onAskToFinishEarly: () -> Unit = {},
    onKeepTraining: () -> Unit = {},
    onFinishEarly: () -> Unit = {},
    onRetryFinishEarly: () -> Unit = {},
    onOpenAdjustSheet: () -> Unit = {},
    onDismissAdjustSheet: () -> Unit = {},
    onChooseReason: (DirectReason) -> Unit = {},
    onShowHowTo: (Int) -> Unit = {},
    onNeedAlternative: (Long) -> Unit = {},
    onUndoAdjustment: () -> Unit = {},
    onScrolled: () -> Unit = {}
) {
    val locale = LocalLocale.current.platformLocale

    // Only the transition ends the day: null means nothing loaded has been seen
    // yet, so the first loaded state primes and a finished routine is not re-finished.
    var wasComplete by remember { mutableStateOf<Boolean?>(null) }

    var showStartOver by remember { mutableStateOf(false) }

    val routine = state.routine
    val finishedEarly = state.outcome?.finishKind == FinishKind.PARTIAL
    val hasOutcome = state.outcome != null
    val requesters = remember { mutableMapOf<Int, BringIntoViewRequester>() }

    LaunchedEffect(state.scrollToPosition) {
        val position = state.scrollToPosition ?: return@LaunchedEffect
        requesters[position]?.bringIntoView()
        onScrolled()
    }

    LaunchedEffect(routine.isComplete, state.isLoaded) {
        if (!state.isLoaded) return@LaunchedEffect

        val isComplete = routine.isComplete
        val previous = wasComplete
        wasComplete = isComplete

        // A session closed as finished early is not re-celebrated by ticking its last box.
        if (previous != null && isComplete && !previous && !finishedEarly) {
            if (state.completesTheWeek) {
                onWeekCompleted(state.weekNumber)
            } else {
                onDayCompleted(state.dayNumber, state.weekNumber)
            }
        }
    }

    if (state.isConfirmingFinishEarly) {
        BackHandler(onBack = onKeepTraining)
        FinishEarlyContent(
            performedExercises = routine.performedExerciseCount,
            plannedExercises = routine.plannedExerciseCount,
            saveFailed = state.saveFailed,
            onKeepTraining = onKeepTraining,
            onFinishEarly = if (state.saveFailed) onRetryFinishEarly else onFinishEarly,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        TrainrTopBar(onBackClick = onBackClick)

        // A blank moment is honest; a sample routine here is one nobody is doing.
        if (!state.isLoaded) return@Column

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.screen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar_today),
                    contentDescription = null,
                    tint = MaterialTheme.trainrColors.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(Spacing.small))
                Text(
                    text = WorkoutDateFormatter.formatFullDate(state.dateMillis, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.trainrColors.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.card),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = routine.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.trainrColors.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.small)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = MaterialTheme.trainrColors.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.extraSmall))
                    Text(
                        text = pluralStringResource(
                            R.plurals.minutes,
                            state.totalMinutes ?: routine.totalMinutes,
                            state.totalMinutes ?: routine.totalMinutes
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.trainrColors.onSurface
                    )
                }
            }

            TrainrProgress(
                currentStep = routine.completionPercentage,
                totalSteps = 100,
                modifier = Modifier.padding(top = Spacing.screen)
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                        append(stringResource(R.string.equipment_label) + " ")
                    }
                    append(state.equipment.joinToString(", "))
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.trainrColors.onSurface,
                modifier = Modifier.padding(top = Spacing.section)
            )

            if (finishedEarly) {
                Text(
                    text = pluralStringResource(
                        R.plurals.finished_early_summary_format,
                        routine.plannedExerciseCount,
                        routine.performedExerciseCount,
                        routine.plannedExerciseCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainrColors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.section)
                        .background(MaterialTheme.trainrColors.surfaceSunken, MaterialTheme.shapes.small)
                        .padding(horizontal = Spacing.card, vertical = 12.dp)
                )
            }

            state.adjustedBanner?.let { banner ->
                AdjustedBanner(
                    banner = banner,
                    showUndo = !hasOutcome,
                    onUndo = onUndoAdjustment,
                    modifier = Modifier.padding(top = Spacing.section)
                )
            }

            state.undoKeptSets?.let { kept ->
                Text(
                    text = pluralStringResource(R.plurals.undo_kept_logged, kept, kept),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainrColors.onSurfaceMuted,
                    modifier = Modifier.padding(top = Spacing.tight)
                )
            }

            if (!hasOutcome) {
                TrainrOptionRow(
                    title = stringResource(R.string.adjust_today),
                    description = stringResource(R.string.adjust_today_hint),
                    onClick = onOpenAdjustSheet,
                    modifier = Modifier.padding(top = Spacing.section)
                )
            }

            Column(
                modifier = Modifier.padding(top = Spacing.section),
                verticalArrangement = Arrangement.spacedBy(Spacing.section)
            ) {
                routine.exercises.forEach { exercise ->
                    key(exercise.position) {
                        val requester = remember { BringIntoViewRequester() }
                        requesters[exercise.position] = requester
                        ExerciseCard(
                            exercise = exercise,
                            onToggleCompleted = { onToggleExercise(exercise.position) },
                            onSetChanged = { onSetChanged(exercise.position, it) },
                            onAddSet = { onAddSet(exercise.position) },
                            onDeleteSet = { onDeleteSet(exercise.position, it.setNumber) },
                            units = state.unitSystem,
                            modifier = Modifier.bringIntoViewRequester(requester)
                        ) {
                            if (!exercise.isCompleted) {
                                ExerciseTimer(
                                    timer = state.timer?.takeIf { it.position == exercise.position },
                                    onStart = { onStartTimer(exercise) },
                                    onPause = onPauseTimer,
                                    onResume = onResumeTimer,
                                    onReset = onResetTimer,
                                    onStop = onStopTimer
                                )

                                val video: @Composable () -> Unit = {
                                    YouTubeVideo.from(exercise.videoUrl)?.let {
                                        VideoTutorial(
                                            video = it,
                                            isExpanded = state.expandedVideo == exercise.position,
                                            onToggle = { onToggleVideo(exercise.position) }
                                        )
                                    }
                                }
                                if (exercise.steps.isNotEmpty()) {
                                    HowToSection(
                                        steps = exercise.steps,
                                        isExpanded = state.expandedHowTo == exercise.position,
                                        onToggle = { onToggleHowTo(exercise.position) },
                                        video = video
                                    )
                                } else {
                                    video()
                                }

                                if (!hasOutcome && exercise.sets.any { !it.isCompleted }) {
                                    TrainrQuietButton(
                                        text = stringResource(R.string.need_an_alternative),
                                        onClick = { onNeedAlternative(exercise.exerciseId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!finishedEarly && !routine.isComplete) {
                TrainrSlideToConfirm(
                    text = stringResource(R.string.slide_to_complete_routine),
                    onConfirm = onCompleteRoutine,
                    modifier = Modifier.padding(top = Spacing.section + Spacing.tight)
                )
                TrainrQuietButton(
                    text = stringResource(R.string.finish_early),
                    onClick = onAskToFinishEarly,
                    modifier = Modifier.padding(top = Spacing.tight)
                )
            } else if (!finishedEarly && routine.hasProgress) {
                TextButton(
                    onClick = { showStartOver = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.section + Spacing.tight)
                ) {
                    Text(
                        text = stringResource(R.string.start_workout_over),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.trainrColors.onSurface
                    )
                }
            }
        }
    }

    if (state.showAdjustSheet) {
        AdjustTodaySheet(
            dayTitle = routine.title,
            exercises = routine.exercises.map { it.name },
            onChoose = onChooseReason,
            onShowHowTo = onShowHowTo,
            onDismiss = onDismissAdjustSheet
        )
    }

    if (showStartOver) {
        StartWorkoutOverDialog(
            onConfirm = {
                showStartOver = false
                onClearProgress()
            },
            onDismiss = { showStartOver = false }
        )
    }
}

@Composable
private fun FinishEarlyContent(
    performedExercises: Int,
    plannedExercises: Int,
    saveFailed: Boolean,
    onKeepTraining: () -> Unit,
    onFinishEarly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.trainrColors

    Box(modifier = modifier) {
        TrainrScaffold(
            onBackClick = onKeepTraining,
            bottomButton = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                    TrainrButton(
                        text = stringResource(
                            if (saveFailed) R.string.try_again else R.string.save_workout
                        ),
                        onClick = onFinishEarly
                    )
                    TrainrQuietButton(
                        text = stringResource(R.string.keep_training),
                        onClick = onKeepTraining
                    )
                }
            }
        ) { padding ->
            TrainrScreenContent(modifier = Modifier.padding(padding)) {
                Text(
                    text = stringResource(R.string.finish_early_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    ),
                    color = colors.onSurface
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.medium)
                        .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
                        .padding(Spacing.card),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(
                        text = stringResource(R.string.finish_early_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.exercises_completed_of_format,
                            plannedExercises,
                            performedExercises,
                            plannedExercises
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                    Text(
                        text = stringResource(R.string.finish_early_card_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted
                    )
                }

                Text(
                    text = stringResource(R.string.finish_early_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier.padding(top = Spacing.medium)
                )

                if (saveFailed) {
                    TrainrFieldError(message = stringResource(R.string.finish_early_failed))
                }
            }
        }
    }
}

@Composable
private fun AdjustedBanner(
    banner: AdjustedBannerUi,
    showUndo: Boolean,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.trainrColors
    val regions = banner.regions.map { stringResource(it.labelRes) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.card, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.adjusted_for_today),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
        Text(
            text = when (banner.messageRes) {
                R.string.adjusted_replaced_banner_format ->
                    stringResource(banner.messageRes, banner.fromName, banner.toName)

                R.string.adjusted_time_banner_format ->
                    stringResource(banner.messageRes, joinAnd(regions))

                else -> stringResource(banner.messageRes)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            // Applying or undoing changes this line and nothing else moves, so
            // a screen reader would otherwise never hear that the day changed.
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        if (showUndo) {
            Text(
                text = stringResource(R.string.undo_adjustment),
                style = MaterialTheme.typography.titleMedium,
                color = colors.brandStrong,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .heightIn(min = ComponentHeight.Medium)
                    .clickable(role = Role.Button, onClick = onUndo)
            )
        }
    }
}

@Composable
private fun StartWorkoutOverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.start_workout_over_title)) },
        text = { Text(text = stringResource(R.string.start_workout_over_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.start_over), color = MaterialTheme.trainrColors.dangerInk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.trainrColors.onSurface)
            }
        }
    )
}

@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun RoutineDetailScreenPreview() {
    TrainrTheme {
        RoutineDetailScreen(state = RoutineDetailViewModel.sampleState())
    }
}
