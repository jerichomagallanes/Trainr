package com.jericx.trainr.presentation.unstuck.feedback

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackOutcomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        state: AdjustmentFeedbackUiState,
        onDone: () -> Unit = {},
        onOpenGuidance: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                FeedbackOutcomeScreen(
                    state = state,
                    onDone = onDone,
                    onOpenGuidance = onOpenGuidance
                )
            }
        }
    }

    @Test
    fun everyAnswerHasItsOwnSentence() {
        val sentences = mapOf(
            FeedbackAnswer.HELPED to R.string.outcome_helped,
            FeedbackAnswer.STILL_TOO_LONG to R.string.outcome_still_too_long,
            FeedbackAnswer.EXERCISE_CONFUSING to R.string.outcome_confusing,
            FeedbackAnswer.SOMETHING_ELSE to R.string.outcome_something_else,
            FeedbackAnswer.DISCOMFORT to R.string.outcome_discomfort
        )
        var state by mutableStateOf(SampleFeedbackStates.time)
        composeTestRule.setContent {
            TrainrTheme { FeedbackOutcomeScreen(state = state) }
        }

        sentences.forEach { (answer, sentence) ->
            state = SampleFeedbackStates.time.copy(answer = answer)

            composeTestRule.onNodeWithText(string(R.string.how_it_fit)).assertIsDisplayed()
            composeTestRule.onNodeWithText(string(sentence)).assertIsDisplayed()
        }
    }

    @Test
    fun theTrendIsHonestAboutWhatOneSessionShows() {
        setScreen(state = SampleFeedbackStates.helped)

        composeTestRule.onNodeWithText(string(R.string.progress_toward_goal)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            string(R.string.progress_need_more_format, string(R.string.trend_training_performance))
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.progress_one_session_caveat))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.future_workouts_unchanged))
            .assertIsDisplayed()
    }

    // No score and no improvement percentage: practicality is not a result (C12).
    @Test
    fun nothingOnScreenIsAPercentage() {
        setScreen(state = SampleFeedbackStates.helped)

        val texts = mutableListOf<String>()
        fun collect(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { texts += it.text }
            node.children.forEach { child -> collect(child) }
        }
        collect(composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())

        assertThat(texts).isNotEmpty()
        texts.forEach { assertThat(it).doesNotContainMatch("\\d\\s*%") }
    }

    @Test
    fun theGuidanceIsOfferedOnlyWhenTheExerciseConfused() {
        var opened = false
        setScreen(state = SampleFeedbackStates.helped, onOpenGuidance = { opened = true })

        composeTestRule.onNodeWithText(string(R.string.view_exercise_guidance)).assertDoesNotExist()
        assertThat(opened).isFalse()
    }

    @Test
    fun theConfusedExerciseKeepsItsGuidanceATapAway() {
        var opened = false
        setScreen(state = SampleFeedbackStates.confusing, onOpenGuidance = { opened = true })

        composeTestRule.onNodeWithText(string(R.string.view_exercise_guidance)).performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun doneIsTheWayOnward() {
        var done = false
        setScreen(state = SampleFeedbackStates.helped, onDone = { done = true })

        composeTestRule.onNodeWithText(string(R.string.done).uppercase()).performClick()

        assertThat(done).isTrue()
    }
}
