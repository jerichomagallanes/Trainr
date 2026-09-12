package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import androidx.compose.ui.test.performClick
import com.jericx.trainr.domain.generation.PlanGenerationResult
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
    // The one free generation is spent in onDone, so a failure reaching it would
    // charge someone their free week for a plan they never got — which is
    // exactly what happened when the spend sat on the screen appearing instead.
    @Test
    fun aGenerationThatFailedNeverFinishes() {
        var done = false
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(
                    isReady = false,
                    onStart = {},
                    onDone = { done = true },
                    failure = PlanGenerationResult.Failed
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(10_000)

        assertThat(done).isFalse()
    }

    @Test
    fun aFailureSaysWhatWentWrong() {
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(
                    isReady = false,
                    onStart = {},
                    onDone = {},
                    failure = PlanGenerationResult.Failed
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.generation_failed_message))
            .assertIsDisplayed()
    }

    @Test
    fun theClientCanAskForThePlanAgain() {
        var retried = false
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(
                    isReady = false,
                    onStart = {},
                    onDone = {},
                    failure = PlanGenerationResult.Failed,
                    onRetry = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(retried).isTrue()
    }

    @Test
    fun theClientCanGoBackToTheirProfile() {
        var wentBack = false
        composeTestRule.setContent {
            TrainrTheme {
                GeneratingScreen(
                    isReady = false,
                    onStart = {},
                    onDone = {},
                    failure = PlanGenerationResult.Failed,
                    onGiveUp = { wentBack = true },
                    giveUpLabel = R.string.back_to_profile
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.back_to_profile)).performClick()

        assertThat(wentBack).isTrue()
    }
}
