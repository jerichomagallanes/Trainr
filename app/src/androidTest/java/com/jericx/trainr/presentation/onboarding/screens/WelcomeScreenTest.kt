package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
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
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysGetStartedButton() {
        composeTestRule.setContent {
            TrainrTheme {
                WelcomeScreen(onGetStartedClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.get_started), ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun clickingGetStartedFiresCallback() {
        var clicked = false
        composeTestRule.setContent {
            TrainrTheme {
                WelcomeScreen(onGetStartedClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText(string(R.string.get_started), ignoreCase = true).performClick()

        assertThat(clicked).isTrue()
    }
}
