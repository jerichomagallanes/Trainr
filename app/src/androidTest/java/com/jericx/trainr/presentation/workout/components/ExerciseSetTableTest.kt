package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSetTableTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setTable(
        measure: ExerciseMeasure,
        sets: List<ExerciseSet> = listOf(
            ExerciseSet(setNumber = 1, targetReps = 12, targetWeightKg = 20f, targetSeconds = 60)
        ),
        onSetChanged: (ExerciseSet) -> Unit = {},
        onAddSet: () -> Unit = {},
        onDeleteSet: (ExerciseSet) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseSetTable(
                    measure = measure,
                    sets = sets,
                    onSetChanged = onSetChanged,
                    onAddSet = onAddSet,
                    onDeleteSet = onDeleteSet
                )
            }
        }
    }

    // A jog has no weight and a squat has no clock: the columns follow the measure.
    @Test
    fun aWeightExerciseShowsBothWeightAndReps() {
        setTable(ExerciseMeasure.WEIGHT_AND_REPS)

        composeTestRule.onNodeWithText(string(R.string.weight_column)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.reps_column)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.time_column)).assertDoesNotExist()
    }

    @Test
    fun aRepExerciseShowsNoWeightColumn() {
        setTable(ExerciseMeasure.REPS)

        composeTestRule.onNodeWithText(string(R.string.reps_column)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.weight_column)).assertDoesNotExist()
    }

    @Test
    fun aTimedExerciseShowsTimeRatherThanReps() {
        setTable(ExerciseMeasure.DURATION)

        composeTestRule.onNodeWithText(string(R.string.time_column)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.reps_column)).assertDoesNotExist()
    }

    // The target is shown as a hint, so an untouched row does not claim you did it.
    @Test
    fun anUnloggedSetShowsItsTargetWithoutRecordingIt() {
        var logged: ExerciseSet? = null
        setTable(ExerciseMeasure.REPS, onSetChanged = { logged = it })

        composeTestRule.onNodeWithText("12").assertIsDisplayed()
        assertThat(logged).isNull()
    }

    @Test
    fun tickingASetReportsItAsCompleted() {
        var logged: ExerciseSet? = null
        setTable(ExerciseMeasure.REPS, onSetChanged = { logged = it })

        composeTestRule.onAllNodesWithContentDescription(string(R.string.mark_set_complete))
            .onFirst()
            .performClick()

        assertThat(logged?.isCompleted).isTrue()
    }

    // "300" seconds on screen is engineer language; the cell speaks the
    // timer's m:ss and is typed like a microwave: 5-0-0 becomes 5:00.
    @Test
    fun timeIsTypedLikeAMicrowaveAndStoredAsSeconds() {
        var logged: ExerciseSet? = null
        setTable(
            ExerciseMeasure.DURATION,
            sets = listOf(ExerciseSet(setNumber = 1, targetSeconds = 300)),
            onSetChanged = { logged = it }
        )

        composeTestRule.onNodeWithText("5:00").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("500")

        assertThat(logged?.actualSeconds).isEqualTo(300)
    }

    @Test
    fun swipingARowAwayDeletesItsSet() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12) },
            onDeleteSet = { deleted = it }
        )

        composeTestRule.onNodeWithText("2").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertThat(deleted?.setNumber).isEqualTo(2)
    }

    // A real finger usually starts its swipe on the widest thing in the row —
    // the input cell — so the field must not eat the drag.
    @Test
    fun aSwipeStartingOnAnInputCellStillDeletes() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = it * 10) },
            onDeleteSet = { deleted = it }
        )

        composeTestRule.onAllNodes(hasSetTextAction())[1].performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertThat(deleted?.setNumber).isEqualTo(2)
    }

    // With a single set there is nothing sensible left after a delete, so the
    // row does not offer one.
    @Test
    fun theOnlyRowCannotBeSwipedAway() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = listOf(ExerciseSet(setNumber = 1, targetReps = 12)),
            onDeleteSet = { deleted = it }
        )

        composeTestRule.onNodeWithText("1").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertThat(deleted).isNull()
    }

    @Test
    fun addingASetReportsIt() {
        var added = false
        setTable(ExerciseMeasure.REPS, onAddSet = { added = true })

        composeTestRule.onNodeWithText(string(R.string.add_set)).performClick()

        assertThat(added).isTrue()
    }
}
