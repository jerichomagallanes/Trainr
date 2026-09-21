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
class FeedbackDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setScreen(
        onAnswer: (FeedbackAnswer) -> Unit = {},
        onSaveForLater: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                FeedbackDetailScreen(onAnswer = onAnswer, onSaveForLater = onSaveForLater)
            }
        }
    }

    @Test
    fun eachFollowUpOffersItsOwnNextStep() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.what_still_needs_changing))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feedback_still_too_long_hint))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feedback_exercise_confusing_hint))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feedback_something_else_hint))
            .assertIsDisplayed()
    }

    @Test
    fun everyRowReportsItsAnswer() {
        val answers = mutableListOf<FeedbackAnswer>()
        setScreen(onAnswer = { answers += it })

        composeTestRule.onNodeWithText(string(R.string.feedback_still_too_long)).performClick()
        composeTestRule.onNodeWithText(string(R.string.feedback_exercise_confusing)).performClick()
        composeTestRule.onNodeWithText(string(R.string.feedback_something_else)).performClick()

        assertThat(answers).containsExactly(
            FeedbackAnswer.STILL_TOO_LONG,
            FeedbackAnswer.EXERCISE_CONFUSING,
            FeedbackAnswer.SOMETHING_ELSE
        ).inOrder()
    }

    @Test
    fun savingForLaterAnswersNothing() {
        var answered = false
        var later = false
        setScreen(onAnswer = { answered = true }, onSaveForLater = { later = true })

        composeTestRule.onNodeWithText(string(R.string.save_for_later)).performClick()

        assertThat(later).isTrue()
        assertThat(answered).isFalse()
    }
}
