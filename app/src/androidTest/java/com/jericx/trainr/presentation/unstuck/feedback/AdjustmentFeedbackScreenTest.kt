package com.jericx.trainr.presentation.unstuck.feedback

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class AdjustmentFeedbackScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        state: AdjustmentFeedbackUiState = SampleFeedbackStates.time,
        onAnswer: (FeedbackAnswer) -> Unit = {},
        onNotQuite: () -> Unit = {},
        onDiscomfort: () -> Unit = {},
        onNotNow: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustmentFeedbackScreen(
                    state = state,
                    onAnswer = onAnswer,
                    onNotQuite = onNotQuite,
                    onDiscomfort = onDiscomfort,
                    onNotNow = onNotNow
                )
            }
        }
    }

    @Test
    fun aShorterSessionIsAskedAboutItsTime() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.your_workout_is_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.did_the_adjustment_help)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feedback_time_body)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feedback_yes_time)).assertIsDisplayed()
    }

    @Test
    fun aReplacementNamesBothExercises() {
        setScreen(state = SampleFeedbackStates.equipment)

        composeTestRule.onNodeWithText(
            string(
                R.string.feedback_equipment_body_format,
                SampleFeedbackStates.equipment.substituteName,
                SampleFeedbackStates.equipment.originalName
            )
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feedback_yes_equipment)).assertIsDisplayed()
    }

    @Test
    fun theThreeRowsReportWhatWasChosen() {
        var answered: FeedbackAnswer? = null
        var notQuite = false
        var discomfort = false
        setScreen(
            onAnswer = { answered = it },
            onNotQuite = { notQuite = true },
            onDiscomfort = { discomfort = true }
        )

        composeTestRule.onNodeWithText(string(R.string.feedback_not_quite)).performClick()
        composeTestRule.onNodeWithText(string(R.string.feedback_discomfort)).performClick()
        composeTestRule.onNodeWithText(string(R.string.feedback_yes_time)).performClick()

        assertThat(notQuite).isTrue()
        assertThat(discomfort).isTrue()
        assertThat(answered).isEqualTo(FeedbackAnswer.HELPED)
    }

    @Test
    fun notNowLeavesWithoutAnAnswer() {
        var answered: FeedbackAnswer? = null
        var notNow = false
        setScreen(onAnswer = { answered = it }, onNotNow = { notNow = true })

        composeTestRule.onNodeWithText(string(R.string.not_now)).performClick()

        assertThat(notNow).isTrue()
        assertThat(answered).isNull()
    }
}
