package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustTimeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        state: AdjustmentUiState = SampleAdjustmentStates.time,
        onSelectMinutes: (Int) -> Unit = {},
        onShowRecommendation: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustTimeScreen(
                    state = state,
                    onSelectMinutes = onSelectMinutes,
                    onShowRecommendation = onShowRecommendation
                )
            }
        }
    }

    @Test
    fun thePresetsComeFromTheState() {
        setScreen()

        SampleAdjustmentStates.time.presets.forEach {
            composeTestRule
                .onNodeWithText(string(R.string.minutes_short_format, it))
                .assertIsDisplayed()
        }
    }

    @Test
    fun choosingAPresetReportsIt() {
        var minutes: Int? = null
        setScreen(onSelectMinutes = { minutes = it })

        composeTestRule.onNodeWithText(string(R.string.minutes_short_format, 25)).performClick()

        assertThat(minutes).isEqualTo(25)
    }

    @Test
    fun anUnsupportedValueExplainsTheRange() {
        setScreen(state = SampleAdjustmentStates.timeWithError)

        composeTestRule.onNodeWithText(string(R.string.adjust_time_range_error)).assertIsDisplayed()
    }

    @Test
    fun theRecommendationWaitsForAnAnswer() {
        setScreen(state = SampleAdjustmentStates.timeWithError)

        composeTestRule
            .onNodeWithText(string(R.string.show_recommendation).uppercase())
            .assertIsNotEnabled()
    }

    @Test
    fun aChosenBudgetEnablesTheRecommendation() {
        setScreen()

        composeTestRule
            .onNodeWithText(string(R.string.show_recommendation).uppercase())
            .assertIsEnabled()
    }

    @Test
    fun theScopeLineNamesTheWholeSession() {
        setScreen()

        composeTestRule
            .onNodeWithText(string(R.string.adjust_time_whole_session))
            .assertIsDisplayed()
    }
}
