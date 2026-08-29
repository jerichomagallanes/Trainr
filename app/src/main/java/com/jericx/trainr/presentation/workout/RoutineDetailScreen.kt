package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.core.TrainrSlideToConfirm
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.components.ExerciseCard
import com.jericx.trainr.presentation.workout.components.ExerciseTimer
import com.jericx.trainr.presentation.workout.components.VideoTutorial
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.YouTubeVideo
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter

@Composable
fun RoutineDetailRoute(
    onBackClick: () -> Unit = {},
    onDayCompleted: (Int) -> Unit = {},
    onWeekCompleted: (Int) -> Unit = {},
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RoutineDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onDayCompleted = onDayCompleted,
        onWeekCompleted = onWeekCompleted,
        onToggleExercise = viewModel::toggleExercise,
        onSetChanged = viewModel::updateSet,
        onAddSet = viewModel::addSet,
        onCompleteRoutine = viewModel::completeRoutine,
        onStartTimer = viewModel::startTimer,
        onPauseTimer = viewModel::pauseTimer,
        onResumeTimer = viewModel::resumeTimer,
        onResetTimer = viewModel::resetTimer,
        onStopTimer = viewModel::stopTimer,
        onToggleVideo = viewModel::toggleVideo,
        onPlayVideo = viewModel::playVideo
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
    onCompleteRoutine: () -> Unit = {},
    onStartTimer: (ExerciseUi) -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    onToggleVideo: (Int) -> Unit = {},
    onPlayVideo: (Int) -> Unit = {},
    onDayCompleted: (Int) -> Unit = {},
    onWeekCompleted: (Int) -> Unit = {}
) {
    val locale = LocalLocale.current.platformLocale

    // However the last exercise gets ticked — the slider or its own checkbox —
    // finishing the routine is what ends the day. Opening an already-finished
    // routine is not finishing it, so only the transition counts.
    var wasComplete by remember { mutableStateOf(state.routine.isComplete) }

    LaunchedEffect(state.routine.isComplete) {
        val isComplete = state.routine.isComplete

        if (isComplete && !wasComplete) {
            if (state.completesTheWeek) {
                onWeekCompleted(state.weekNumber)
            } else {
                onDayCompleted(state.dayNumber)
            }
        }
        wasComplete = isComplete
    }
    val routine = state.routine

    Column(modifier = modifier.fillMaxSize()) {
        TrainrTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.screen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_calendar_today),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(Spacing.small))
                Text(
                    text = WorkoutDateFormatter.formatFullDate(state.dateMillis, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate800
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
                    color = Slate800
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.extraSmall))
                    Text(
                        text = pluralStringResource(
                            R.plurals.minutes,
                            routine.totalMinutes,
                            routine.totalMinutes
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = Slate800
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
                color = Slate800,
                modifier = Modifier.padding(top = Spacing.section)
            )

            Column(
                modifier = Modifier.padding(top = Spacing.section),
                verticalArrangement = Arrangement.spacedBy(Spacing.section)
            ) {
                routine.exercises.forEach { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onToggleCompleted = { onToggleExercise(exercise.position) },
                        onSetChanged = { onSetChanged(exercise.position, it) },
                        onAddSet = { onAddSet(exercise.position) }
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

                            YouTubeVideo.from(exercise.videoUrl)?.let { video ->
                                VideoTutorial(
                                    video = video,
                                    isExpanded = exercise.position in state.expandedVideos,
                                    isPlaying = state.playingVideo == exercise.position,
                                    onToggle = { onToggleVideo(exercise.position) },
                                    onPlay = { onPlayVideo(exercise.position) }
                                )
                            }
                        }
                    }
                }
            }

            TrainrSlideToConfirm(
                text = stringResource(R.string.slide_to_complete_routine),
                onConfirm = onCompleteRoutine,
                modifier = Modifier.padding(top = Spacing.section + Spacing.tight)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun RoutineDetailScreenPreview() {
    TrainrTheme {
        RoutineDetailScreen(state = RoutineDetailViewModel.sampleState())
    }
}
