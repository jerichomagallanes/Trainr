package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.ExerciseTimerUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseTimerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setTimer(
        timer: ExerciseTimerUi?,
        onStart: () -> Unit = {},
        onPause: () -> Unit = {},
        onResume: () -> Unit = {},
        onStop: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseTimer(
                    timer = timer,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop
                )
            }
        }
    }

    private fun running(seconds: Int = 58) =
        ExerciseTimerUi(position = 2, remainingSeconds = seconds, isRunning = true)

    private fun paused(seconds: Int = 58) =
        ExerciseTimerUi(position = 2, remainingSeconds = seconds, isRunning = false)

    @Test
    fun anUnstartedExerciseOffersOnlyToStart() {
        setTimer(timer = null)

        composeTestRule.onNodeWithText(string(R.string.start_timer)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.stop_timer)).assertDoesNotExist()
    }

    @Test
    fun aRunningTimerShowsTheCountdownAndOffersPauseAndStop() {
        setTimer(timer = running())

        composeTestRule.onNodeWithText("0:58").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.exercise_in_progress)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pause_timer)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.stop_timer)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.resume_timer)).assertDoesNotExist()
    }

    @Test
    fun aPausedTimerSaysSoAndOffersResume() {
        setTimer(timer = paused())

        composeTestRule.onNodeWithText(string(R.string.timer_paused)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.resume_timer)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pause_timer)).assertDoesNotExist()
    }

    @Test
    fun eachControlReportsItsAction() {
        var started = false
        setTimer(timer = null, onStart = { started = true })
        composeTestRule.onNodeWithText(string(R.string.start_timer)).performClick()
        assertThat(started).isTrue()
    }

    @Test
    fun pauseAndStopReportTheirActions() {
        var paused = false
        var stopped = false
        setTimer(timer = running(), onPause = { paused = true }, onStop = { stopped = true })

        composeTestRule.onNodeWithText(string(R.string.pause_timer)).performClick()
        composeTestRule.onNodeWithText(string(R.string.stop_timer)).performClick()

        assertThat(paused).isTrue()
        assertThat(stopped).isTrue()
    }

    @Test
    fun resumeReportsItsAction() {
        var resumed = false
        setTimer(timer = paused(), onResume = { resumed = true })

        composeTestRule.onNodeWithText(string(R.string.resume_timer)).performClick()

        assertThat(resumed).isTrue()
    }
}
