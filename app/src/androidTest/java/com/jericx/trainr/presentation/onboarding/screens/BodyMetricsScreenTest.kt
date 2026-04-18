package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyMetricsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.your_measurements))
            .assertIsDisplayed()
    }

    @Test
    fun nextDisabledInitiallyAndEnabledAfterFillingHeightAndWeight() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }

    @Test
    fun nextEmitsParsedMetricValues() {
        var capturedHeight = 0f
        var capturedWeight = 0f

        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(
                    onNextClick = { h, w -> capturedHeight = h; capturedWeight = w },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(capturedHeight).isEqualTo(170f)
        assertThat(capturedWeight).isEqualTo(70f)
    }

    @Test
    fun togglingToImperialChangesUnitLabels() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText(string(R.string.height_ft_in)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.weight_lbs)).assertIsDisplayed()
    }
}
