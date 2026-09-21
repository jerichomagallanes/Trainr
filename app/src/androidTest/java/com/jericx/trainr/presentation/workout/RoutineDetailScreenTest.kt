package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.AdjustedBannerUi
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val state = RoutineDetailViewModel.sampleState()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        onToggleExercise: (Int) -> Unit = {},
        onCompleteRoutine: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(
                    state = state,
                    onToggleExercise = onToggleExercise,
                    onCompleteRoutine = onCompleteRoutine
                )
            }
        }
    }

    @Test
    fun aFinishedWorkoutOffersNothingToFinish() {
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(state = state.copy(routine = state.routine.completeAll()))
            }
        }

        composeTestRule.onNodeWithText(string(R.string.slide_to_complete_routine))
            .assertDoesNotExist()
    }

    private fun slideToConfirm(endX: (Float) -> Float) {
        composeTestRule.onNodeWithText(string(R.string.slide_to_complete_routine))
            .performScrollTo()
            .performTouchInput {
                swipeRight(startX = 25.dp.toPx(), endX = endX(width.toFloat()))
            }
    }

    @Test
    fun showsTheRoutineTitleAndTotalDuration() {
        setScreen()

        composeTestRule.onNodeWithText("CARDIO & CORE").assertIsDisplayed()
        composeTestRule.onNodeWithText("28 mins").assertIsDisplayed()
    }

    @Test
    fun showsTheDateAndEquipment() {
        setScreen()

        val date = WorkoutDateFormatter.formatFullDate(state.dateMillis, Locale.getDefault())
        composeTestRule.onNodeWithText(date).assertIsDisplayed()
        composeTestRule.onNodeWithText(state.equipment.joinToString(", "), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun listsEveryExerciseInTheRoutine() {
        setScreen()

        state.routine.exercises.forEach { exercise ->
            composeTestRule.onNodeWithText(exercise.name).assertExists()
        }
    }

    // The first exercise is already done, so the first unticked box is number two.
    @Test
    fun tickingAnExerciseReportsItsPosition() {
        var toggled: Int? = null
        setScreen(onToggleExercise = { toggled = it })

        composeTestRule.onAllNodesWithContentDescription(string(R.string.mark_exercise_complete))
            .onFirst()
            .performClick()

        assertThat(toggled).isEqualTo(2)
    }

    @Test
    fun onlyTheUnfinishedExercisesOfferATimer() {
        setScreen()

        val starts = composeTestRule
            .onAllNodesWithText(string(R.string.start_timer))
            .fetchSemanticsNodes()

        assertThat(starts).hasSize(state.routine.exercises.count { !it.isCompleted })
    }

    // A movement with a tutorial but no written steps offers the video alone:
    // a How to perform toggle with nothing behind it opened onto an empty list.
    @Test
    fun aMovementWithOnlyAVideoOffersTheVideoAndNotTheSteps() {
        val unfinished = state.routine.exercises.filter { !it.isCompleted }
        assertThat(unfinished.any { it.steps.isEmpty() && it.videoUrl != null }).isTrue()
        setScreen()

        val howTos = composeTestRule
            .onAllNodesWithText(string(R.string.show_how_to_perform))
            .fetchSemanticsNodes()
        val videos = composeTestRule
            .onAllNodesWithText(string(R.string.show_video_tutorial))
            .fetchSemanticsNodes()

        assertThat(howTos).hasSize(unfinished.count { it.steps.isNotEmpty() })
        assertThat(videos).hasSize(unfinished.count { it.steps.isEmpty() && it.videoUrl != null })
    }

    // The stored routine arrives after the sample one is already on screen;
    // swapping in an already-finished day must not read as finishing it.
    @Test
    fun aFinishedRoutineArrivingFromStorageReportsNothing() {
        var reported: Int? = null

        composeTestRule.setContent {
            var current by remember { mutableStateOf(state.copy(isLoaded = false)) }
            TrainrTheme {
                RoutineDetailScreen(state = current, onDayCompleted = { reported = it })
                LaunchedEffect(Unit) {
                    current = state.copy(routine = state.routine.completeAll())
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(reported).isNull()
    }

    // Reopening a day already finished shows the routine, not the celebration again.
    @Test
    fun openingAnAlreadyFinishedRoutineReportsNothing() {
        var reported: Int? = null
        val finished = state.copy(routine = state.routine.completeAll())

        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(state = finished, onDayCompleted = { reported = it })
            }
        }

        composeTestRule.waitForIdle()
        assertThat(reported).isNull()
        composeTestRule.onNodeWithText("CARDIO & CORE").assertIsDisplayed()
    }

    @Test
    fun finishingTheLastExerciseReportsTheDay() {
        var reported: Int? = null

        composeTestRule.setContent {
            var current by remember { mutableStateOf(state) }
            TrainrTheme {
                RoutineDetailScreen(
                    state = current,
                    onToggleExercise = { current = current.copy(routine = current.routine.completeAll()) },
                    onDayCompleted = { reported = it }
                )
            }
        }

        composeTestRule.onAllNodesWithContentDescription(string(R.string.mark_exercise_complete))
            .onFirst()
            .performClick()
        composeTestRule.waitForIdle()

        assertThat(reported).isEqualTo(state.dayNumber)
    }

    @Test
    fun slidingTheThumbToTheEndCompletesTheRoutine() {
        var completed = false
        setScreen(onCompleteRoutine = { completed = true })

        slideToConfirm { width -> width }

        assertThat(completed).isTrue()
    }

    // A drag is unreachable through TalkBack, so the bar carries a click action too.
    @Test
    fun theAccessibilityActionCompletesTheRoutine() {
        var completed = false
        setScreen(onCompleteRoutine = { completed = true })

        composeTestRule.onNodeWithText(string(R.string.slide_to_complete_routine))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(completed).isTrue()
    }

    @Test
    fun aPartialSlideLeavesTheRoutineAlone() {
        var completed = false
        setScreen(onCompleteRoutine = { completed = true })

        slideToConfirm { width -> width / 2 }

        assertThat(completed).isFalse()
    }

    @Test
    fun aFinishedWorkoutOffersTheWayBackInstead() {
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(state = state.copy(routine = state.routine.completeAll()))
            }
        }

        composeTestRule.onNodeWithText(string(R.string.slide_to_complete_routine))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.start_workout_over))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun anUntouchedWorkoutDoesNotOfferToStartOver() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.start_workout_over))
            .assertDoesNotExist()
    }

    @Test
    fun startingOverAsksFirstAndSaysWhatSurvives() {
        var cleared = false
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(
                    state = state.copy(routine = state.routine.completeAll()),
                    onClearProgress = { cleared = true }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.start_workout_over))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(string(R.string.start_workout_over_message))
            .assertIsDisplayed()
        assertThat(cleared).isFalse()

        composeTestRule.onNodeWithText(string(R.string.start_over)).performClick()

        assertThat(cleared).isTrue()
    }

    @Test
    fun cancellingLeavesTheWorkoutFinished() {
        var cleared = false
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(
                    state = state.copy(routine = state.routine.completeAll()),
                    onClearProgress = { cleared = true }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.start_workout_over))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(cleared).isFalse()
        composeTestRule.onNodeWithText(string(R.string.start_workout_over))
            .assertIsDisplayed()
    }

    private val finishedEarly = state.copy(
        outcome = SessionOutcome(
            workoutDayId = 2,
            finishKind = FinishKind.PARTIAL,
            finishedAt = 1L,
            performedSetCount = 1,
            plannedSetCount = 4
        )
    )

    @Test
    fun anUnfinishedWorkoutOffersToFinishEarly() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.finish_early))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aFinishedWorkoutDoesNotOfferToFinishEarly() {
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(state = state.copy(routine = state.routine.completeAll()))
            }
        }

        composeTestRule.onNodeWithText(string(R.string.finish_early)).assertDoesNotExist()
    }

    @Test
    fun finishingEarlyAsksFirstWithTheTruthfulCount() {
        var saved = false
        composeTestRule.setContent {
            var current by remember { mutableStateOf(state) }
            TrainrTheme {
                RoutineDetailScreen(
                    state = current,
                    onAskToFinishEarly = { current = current.copy(isConfirmingFinishEarly = true) },
                    onKeepTraining = { current = current.copy(isConfirmingFinishEarly = false) },
                    onFinishEarly = { saved = true }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.finish_early))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(string(R.string.finish_early_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            string(
                R.string.exercises_completed_of_format,
                state.routine.performedExerciseCount,
                state.routine.plannedExerciseCount
            )
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.finish_early_card_message)).assertIsDisplayed()
        assertThat(saved).isFalse()

        composeTestRule.onNodeWithText(string(R.string.save_workout).uppercase()).performClick()
        assertThat(saved).isTrue()
    }

    @Test
    fun keepTrainingReturnsToTheWorkout() {
        composeTestRule.setContent {
            var current by remember { mutableStateOf(state.copy(isConfirmingFinishEarly = true)) }
            TrainrTheme {
                RoutineDetailScreen(
                    state = current,
                    onKeepTraining = { current = current.copy(isConfirmingFinishEarly = false) }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.finish_early_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.keep_training)).performClick()

        composeTestRule.onNodeWithText(string(R.string.finish_early_title)).assertDoesNotExist()
        composeTestRule.onNodeWithText("CARDIO & CORE").assertIsDisplayed()
    }

    @Test
    fun aFailedSaveSaysSoAndOffersToTryAgain() {
        var retried = false
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(
                    state = state.copy(isConfirmingFinishEarly = true, saveFailed = true),
                    onRetryFinishEarly = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.finish_early_failed)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.try_again).uppercase()).performClick()

        assertThat(retried).isTrue()
    }

    @Test
    fun tickingTheLastExerciseOfAWorkoutFinishedEarlyReportsNothing() {
        var reported: Int? = null

        composeTestRule.setContent {
            var current by remember { mutableStateOf(finishedEarly) }
            TrainrTheme {
                RoutineDetailScreen(
                    state = current,
                    onToggleExercise = { current = current.copy(routine = current.routine.completeAll()) },
                    onDayCompleted = { reported = it },
                    onWeekCompleted = { reported = it }
                )
            }
        }

        composeTestRule.onAllNodesWithContentDescription(string(R.string.mark_exercise_complete))
            .onFirst()
            .performClick()
        composeTestRule.waitForIdle()

        assertThat(reported).isNull()
    }

    @Test
    fun aWorkoutFinishedEarlyShowsTheBannerAndNothingToFinish() {
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(state = finishedEarly)
            }
        }

        composeTestRule.onNodeWithText(
            string(
                R.string.finished_early_summary_format,
                state.routine.performedExerciseCount,
                state.routine.plannedExerciseCount
            )
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.slide_to_complete_routine)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.finish_early)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.start_workout_over)).assertDoesNotExist()
    }

    @Test
    fun aSessionStillRunningOffersToAdjustToday() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.adjust_today)).performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(string(R.string.need_an_alternative)).onFirst()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aFinishedSessionIsNotOfferedAnAdjustment() {
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(
                    state = state.copy(
                        outcome = SessionOutcome(
                            workoutDayId = 1,
                            finishKind = FinishKind.PARTIAL,
                            finishedAt = 1L,
                            performedSetCount = 1,
                            plannedSetCount = 4
                        )
                    )
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.adjust_today)).assertDoesNotExist()
        assertThat(
            composeTestRule.onAllNodesWithText(string(R.string.need_an_alternative))
                .fetchSemanticsNodes()
        ).isEmpty()
    }

    @Test
    fun anAppliedAdjustmentIsAnnouncedAndCanBeUndone() {
        var undone = false
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(
                    state = state.copy(
                        adjustedBanner = AdjustedBannerUi(
                            messageRes = R.string.adjusted_time_banner_format,
                            regions = listOf(MuscleRegion.ARMS)
                        )
                    ),
                    onUndoAdjustment = { undone = true }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.adjusted_for_today)).performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                string(R.string.adjusted_time_banner_format, string(R.string.region_arms))
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.undo_adjustment)).performClick()

        assertThat(undone).isTrue()
    }

    @Test
    fun tappingAdjustTodayOpensTheChooser() {
        var opened = false
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailScreen(state = state, onOpenAdjustSheet = { opened = true })
            }
        }

        composeTestRule.onNodeWithText(string(R.string.adjust_today)).performScrollTo()
            .performClick()

        assertThat(opened).isTrue()
    }
}
