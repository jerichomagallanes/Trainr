package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratingScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysGeneratingMessage() {
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(isReady = false, onStart = {}, onDone = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.generating_your_workout_routine))
            .assertIsDisplayed()
    }

    // The screen used to sit on a timer and only then ask for a plan, so every
    // generation cost that wait on top of the real one.
    @Test
    fun generatingBeginsWithTheScreen() {
        var started = false
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(isReady = false, onStart = { started = true }, onDone = {})
            }
        }

        composeTestRule.waitForIdle()

        assertThat(started).isTrue()
    }

    @Test
    fun theScreenWaitsForThePlanBeforeMovingOn() {
        var done = false
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(isReady = false, onStart = {}, onDone = { done = true })
            }
        }

        composeTestRule.mainClock.advanceTimeBy(10_000)

        assertThat(done).isFalse()
    }

    // A plan that answers at once still gets read: the screen holds briefly
    // rather than flashing past.
    @Test
    fun aPlanThatIsReadyAtOnceStillShowsTheScreen() {
        var done = false
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(isReady = true, onStart = {}, onDone = { done = true })
            }
        }

        composeTestRule.mainClock.advanceTimeBy(500)
        assertThat(done).isFalse()

        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()
        assertThat(done).isTrue()
    }
}
