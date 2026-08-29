package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
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

    @Test
    fun togglingUnitsConvertsAlreadyEnteredValues() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText("5'7\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("154").assertIsDisplayed()
    }

    // Switching units rewrites the field's contents, swaps its keyboard and
    // changes what it will accept, so the field the user was editing is
    // effectively a different field afterwards. Leaving focus behind stranded
    // the caret in a value the user never typed.
    @Test
    fun switchingUnitsClearsFocusFromTheFieldBeingEdited() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText("170").assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText("5'7\"").assertIsNotFocused()
    }

    @Test
    fun switchingUnitsClearsFocusFromTheWeightFieldToo() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")
        composeTestRule.onNodeWithText("70").assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText("154").assertIsNotFocused()
    }

    // Re-tapping the unit that is already active changes nothing, so it must
    // not interrupt whatever the user is typing.
    @Test
    fun reselectingTheCurrentUnitKeepsFocus() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText("170").assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.metric)).performClick()

        composeTestRule.onNodeWithText("170").assertIsFocused()
    }
}
