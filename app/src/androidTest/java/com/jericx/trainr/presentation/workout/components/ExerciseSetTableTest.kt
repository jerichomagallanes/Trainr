package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
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

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setTable(
        measure: ExerciseMeasure,
        sets: List<ExerciseSet> = listOf(
            ExerciseSet(setNumber = 1, targetReps = 12, targetWeightKg = 20f, targetSeconds = 60)
        ),
        onSetChanged: (ExerciseSet) -> Unit = {},
        onAddSet: () -> Unit = {},
        onDeleteSet: (ExerciseSet) -> Unit = {},
        isReadOnly: Boolean = false
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseSetTable(
                    measure = measure,
                    sets = sets,
                    onSetChanged = onSetChanged,
                    onAddSet = onAddSet,
                    onDeleteSet = onDeleteSet,
                    isReadOnly = isReadOnly
                )
            }
        }
    }

    @Test
    fun aRecordedSetShowsItsNumbersWithNoWayToChangeThem() {
        setTable(
            ExerciseMeasure.WEIGHT_AND_REPS,
            sets = listOf(
                ExerciseSet(
                    setNumber = 1,
                    targetReps = 12,
                    targetWeightKg = 20f,
                    actualReps = 9,
                    actualWeightKg = 17.5f,
                    isCompleted = true
                )
            ),
            isReadOnly = true
        )

        composeTestRule.onNodeWithText("9").assertIsDisplayed()
        composeTestRule.onNodeWithText("17.5").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.completed)).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.add_set)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(string(R.string.delete_set)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(string(R.string.mark_set_incomplete))
            .assertDoesNotExist()
        assertThat(composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()).isEmpty()
    }

    // The prescription is all a set nobody logged has left to show.
    @Test
    fun aRecordedSetNeverLoggedFallsBackToWhatItAskedFor() {
        setTable(ExerciseMeasure.REPS, isReadOnly = true)

        composeTestRule.onNodeWithText("12").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.not_completed))
            .assertIsDisplayed()
    }

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

    // Typed like a microwave (5-0-0 becomes 5:00) and stored as seconds.
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

    // The screen feeds every change back, so the field shows what is stored.
    private fun loggingTable(
        measure: ExerciseMeasure,
        initial: List<ExerciseSet> = listOf(
            ExerciseSet(setNumber = 1, targetReps = 12, targetWeightKg = 20f, targetSeconds = 60)
        ),
        onLogged: (ExerciseSet) -> Unit = {}
    ) {
        composeTestRule.setContent {
            var sets by remember { mutableStateOf(initial) }
            TrainrTheme {
                ExerciseSetTable(
                    measure = measure,
                    sets = sets,
                    onSetChanged = { changed ->
                        sets = sets.map { if (it.setNumber == changed.setNumber) changed else it }
                        onLogged(changed)
                    },
                    onAddSet = {}
                )
            }
        }
    }

    private fun SemanticsNodeInteraction.shownText(): String =
        fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun weightField() = composeTestRule.onAllNodes(hasSetTextAction()).onFirst()

    private fun onlyField() = composeTestRule.onNode(hasSetTextAction())

    // A slipped dot used to clear the number and start the next keystroke anew.
    @Test
    fun aSecondDecimalPointLeavesTheNumberAndTheRecordAlone() {
        var logged: ExerciseSet? = null
        loggingTable(ExerciseMeasure.WEIGHT_AND_REPS, onLogged = { logged = it })

        weightField().performTextInput("22.5")
        weightField().performTextInput(".")

        assertThat(weightField().shownText()).isEqualTo("22.5")
        assertThat(logged?.actualWeightKg).isEqualTo(22.5f)
    }

    // An external keyboard offers letters whatever the on-screen pad shows.
    @Test
    fun aLetterNeverReachesTheFieldOrTheRecord() {
        var logged: ExerciseSet? = null
        loggingTable(ExerciseMeasure.REPS, onLogged = { logged = it })

        onlyField().performTextInput("abc")

        assertThat(onlyField().shownText()).isEmpty()
        assertThat(logged).isNull()
    }

    @Test
    fun aWeightOverTheMaximumIsRefusedAndSaysWhatTheRangeIs() {
        var logged: ExerciseSet? = null
        loggingTable(ExerciseMeasure.WEIGHT_AND_REPS, onLogged = { logged = it })

        weightField().performTextInput("1500")

        assertThat(weightField().shownText()).isEmpty()
        assertThat(logged).isNull()
        weightRangeMessage().assertIsDisplayed()

        weightField().performTextInput("20")

        weightRangeMessage().assertDoesNotExist()
    }

    // The keystroke is dropped, so the message is the only sign the person gets
    // that anything was refused.
    @Test
    fun aCountOverTheMaximumSaysWhatTheRangeIsUntilADigitIsAccepted() {
        loggingTable(ExerciseMeasure.REPS)

        onlyField().performTextInput("9999")

        repsRangeMessage().assertIsDisplayed()

        onlyField().performTextInput("12")

        assertThat(onlyField().shownText()).isEqualTo("12")
        repsRangeMessage().assertDoesNotExist()
    }

    private fun weightRangeMessage() = composeTestRule.onNodeWithText(
        string(
            R.string.value_range_hint,
            string(R.string.weight_label),
            "0",
            "1000 " + string(R.string.weight_column)
        )
    )

    private fun repsRangeMessage() = composeTestRule.onNodeWithText(
        string(R.string.value_range_hint, string(R.string.reps_column), "0", "999")
    )

    // 6-3-0 used to stall at 0:06, with the last two keystrokes swallowed.
    @Test
    fun everyDigitOfASixMinuteTimeReachesTheCell() {
        var logged: ExerciseSet? = null
        loggingTable(
            ExerciseMeasure.DURATION,
            initial = listOf(ExerciseSet(setNumber = 1)),
            onLogged = { logged = it }
        )

        onlyField().performTextInput("6")
        onlyField().performTextInput("3")
        onlyField().performTextInput("0")

        assertThat(logged?.actualSeconds).isEqualTo(390)
    }

    // 1-6-4-0 used to stall at 0:16, the same stall one digit further along.
    @Test
    fun everyDigitOfASixteenMinuteTimeReachesTheCell() {
        var logged: ExerciseSet? = null
        loggingTable(
            ExerciseMeasure.DURATION,
            initial = listOf(ExerciseSet(setNumber = 1)),
            onLogged = { logged = it }
        )

        listOf("1", "6", "4", "0").forEach { onlyField().performTextInput(it) }

        assertThat(logged?.actualSeconds).isEqualTo(1000)
    }

    @Test
    fun aCountOverTheMaximumIsRefused() {
        var logged: ExerciseSet? = null
        loggingTable(ExerciseMeasure.REPS, onLogged = { logged = it })

        onlyField().performTextInput("999999")

        assertThat(onlyField().shownText()).isEmpty()
        assertThat(logged).isNull()
    }

    // The number pad has no Return key of its own.
    @Test
    fun theNumberPadIsClosedByItsOwnDoneKey() {
        loggingTable(ExerciseMeasure.REPS)

        onlyField().performClick()
        onlyField().assertIsFocused()
        assertThat(onlyField().fetchSemanticsNode().config[SemanticsProperties.ImeAction])
            .isEqualTo(ImeAction.Done)

        onlyField().performImeAction()

        onlyField().assertIsNotFocused()
    }

    // Swipe across the row, not the set-number cell; distance decides the delete.
    private fun swipeRow(label: String, across: Float) {
        val bounds = composeTestRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        composeTestRule.onRoot().performTouchInput {
            val y = bounds.center.y
            swipe(
                start = Offset(right - 1f, y),
                end = Offset(right - (right - left) * across, y),
                durationMillis = 300
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun swipingARowAwayDeletesItsSet() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12) },
            onDeleteSet = { deleted = it }
        )

        swipeRow("2", across = 0.95f)

        assertThat(deleted?.setNumber).isEqualTo(2)
    }

    // A finger usually starts on the widest cell, so the input must not eat the drag.
    @Test
    fun aSwipeStartingOnAnInputCellStillDeletes() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = it * 10) },
            onDeleteSet = { deleted = it }
        )

        swipeRow("2", across = 0.95f)

        assertThat(deleted?.setNumber).isEqualTo(2)
    }

    // The stored routine replaces every set after first composition, so the swipe
    // must delete the current set, not a stale capture.
    @Test
    fun aSwipeAfterTheSetsWereReplacedStillDeletes() {
        var deleted: ExerciseSet? = null
        lateinit var replaceSets: (List<ExerciseSet>) -> Unit
        composeTestRule.setContent {
            var sets by remember {
                mutableStateOf((1..3).map { ExerciseSet(setNumber = it, targetReps = 12) })
            }
            replaceSets = { sets = it }
            TrainrTheme {
                ExerciseSetTable(
                    measure = ExerciseMeasure.REPS,
                    sets = sets,
                    onSetChanged = {},
                    onAddSet = {},
                    onDeleteSet = { deleted = it }
                )
            }
        }

        val replaced = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12) }
        composeTestRule.runOnIdle { replaceSets(replaced) }

        swipeRow("2", across = 0.95f)

        assertThat(deleted?.setNumber).isEqualTo(2)
    }

    // Each report renumbers the rows, so one gesture must report exactly once.
    @Test
    fun oneSwipeDeletesExactlyOneSet() {
        val deletions = mutableListOf<Int>()
        composeTestRule.setContent {
            var sets by remember {
                mutableStateOf((1..3).map { ExerciseSet(setNumber = it, targetReps = 12) })
            }
            TrainrTheme {
                ExerciseSetTable(
                    measure = ExerciseMeasure.REPS,
                    sets = sets,
                    onSetChanged = {},
                    onAddSet = {},
                    onDeleteSet = { victim ->
                        deletions += victim.setNumber
                        sets = sets
                            .filter { it.setNumber != victim.setNumber }
                            .mapIndexed { index, kept -> kept.copy(setNumber = index + 1) }
                    }
                )
            }
        }

        swipeRow("2", across = 0.95f)

        assertThat(deletions).containsExactly(2)
    }

    @Test
    fun halfASwipeRevealsTheDeleteWithoutDoingIt() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12) },
            onDeleteSet = { deleted = it }
        )

        swipeRow("2", across = 0.5f)

        assertThat(deleted).isNull()
    }

    // The delete stays visible while the row is held aside; do not gate it on swipe progress.
    @Test
    fun holdingARowAsideShowsTheDeleteBehindIt() {
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12) }
        )

        composeTestRule.onNodeWithText("2").performTouchInput {
            down(centerRight)
            moveTo(center)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(string(R.string.delete_set))
            .assertIsDisplayed()
    }

    // At rest there is nothing behind the row, or the red shows through it.
    @Test
    fun aRowAtRestHidesTheDelete() {
        setTable(
            ExerciseMeasure.REPS,
            sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 12) }
        )

        composeTestRule.onNodeWithContentDescription(string(R.string.delete_set))
            .assertDoesNotExist()
    }

    @Test
    fun theOnlyRowCanBeSwipedAwayToo() {
        var deleted: ExerciseSet? = null
        setTable(
            ExerciseMeasure.REPS,
            sets = listOf(ExerciseSet(setNumber = 1, targetReps = 12)),
            onDeleteSet = { deleted = it }
        )

        swipeRow("1", across = 0.95f)

        assertThat(deleted?.setNumber).isEqualTo(1)
    }

    @Test
    fun addingASetReportsIt() {
        var added = false
        setTable(ExerciseMeasure.REPS, onAddSet = { added = true })

        composeTestRule.onNodeWithText(string(R.string.add_set)).performClick()

        assertThat(added).isTrue()
    }

    // A fixed 24dp box clipped the tick at the largest accessibility size.
    @Test
    fun theSetTickGrowsWithTheTextSetting() {
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale = 2f)
            ) {
                TrainrTheme {
                    ExerciseSetTable(
                        measure = ExerciseMeasure.REPS,
                        sets = listOf(ExerciseSet(setNumber = 1, targetReps = 12)),
                        onSetChanged = {},
                        onAddSet = {}
                    )
                }
            }
        }

        val tick = composeTestRule.onNodeWithContentDescription(
            string(R.string.mark_set_complete)
        ).getUnclippedBoundsInRoot()

        assertThat((tick.right - tick.left).value).isAtLeast(40f)
    }
}
