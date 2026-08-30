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
import com.jericx.trainr.presentation.common.theme.TrainrTheme
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

    private fun string(id: Int) = composeTestRule.activity.getString(id)

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

    // Nothing left to finish, so the control has nothing to say — and it is
    // reachable now that the finished session stays behind the congratulations.
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

    // The design gives the finished card no timer row, and it has nothing left to time.
    @Test
    fun onlyTheUnfinishedExercisesOfferATimer() {
        setScreen()

        val starts = composeTestRule
            .onAllNodesWithText(string(R.string.start_timer))
            .fetchSemanticsNodes()

        assertThat(starts).hasSize(state.routine.exercises.count { !it.isCompleted })
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

    // Tapping a day you already finished should show you the routine, not bounce
    // you straight to the celebration screen you saw when you finished it.
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

    // A short drag is how an accidental brush reads; it must not tick the routine off.
    @Test
    fun aPartialSlideLeavesTheRoutineAlone() {
        var completed = false
        setScreen(onCompleteRoutine = { completed = true })

        slideToConfirm { width -> width / 2 }

        assertThat(completed).isFalse()
    }
}
