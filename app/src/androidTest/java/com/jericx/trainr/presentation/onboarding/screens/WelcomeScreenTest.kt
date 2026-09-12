package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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

    // The screen used to be laid out from the width alone, so on a short screen
    // there was nothing left for the button by the time the column reached it.
    // It is the height that has to be asserted, not the position: a Column hands
    // its last child whatever remains, so the button stayed in place and simply
    // collapsed to nothing — present, findable, and impossible to tap.
    @Test
    fun theButtonKeepsItsHeightOnAShortScreen() {
        composeTestRule.setContent {
            TrainrTheme {
                Box(modifier = Modifier.size(SHORT_WIDTH, SHORT_HEIGHT)) {
                    WelcomeScreen(onGetStartedClick = {})
                }
            }
        }

        val button = composeTestRule
            .onNodeWithText(string(R.string.get_started), ignoreCase = true)
            .getUnclippedBoundsInRoot()
        val height = button.bottom.value - button.top.value

        assertWithMessage("the button collapsed to ${height}dp on a $SHORT_HEIGHT screen")
            .that(height)
            .isAtLeast(MIN_TAP_TARGET.value)
        assertWithMessage("the button is below the bottom of a $SHORT_HEIGHT screen")
            .that(button.bottom.value)
            .isAtMost(SHORT_HEIGHT.value)
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

    private companion object {
        // A 16:9 phone at xxhdpi — the shortest size still worth supporting and
        // the one the old layout broke on. The height is what is left for
        // content after the system bars, which is what the screen is handed.
        val SHORT_WIDTH = 360.dp
        val SHORT_HEIGHT = 592.dp

        // Below this nothing can reliably be tapped, which is the difference
        // between a cramped button and no button.
        val MIN_TAP_TARGET = 44.dp
    }
}
