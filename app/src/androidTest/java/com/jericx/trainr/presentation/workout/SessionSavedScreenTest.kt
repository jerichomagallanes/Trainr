package com.jericx.trainr.presentation.workout

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
class SessionSavedScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(onDoneClick: () -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme {
                SessionSavedScreen(
                    performedExercises = 4,
                    plannedExercises = 6,
                    onDoneClick = onDoneClick
                )
            }
        }
    }

    @Test
    fun saysTheWorkoutWasSavedAndHowMuchWasDone() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.workout_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.finished_early_summary_format, 4, 6))
            .assertIsDisplayed()
    }

    @Test
    fun doneIsTheOnlyWayOnward() {
        var done = false
        setScreen(onDoneClick = { done = true })

        composeTestRule.onNodeWithText(string(R.string.view_weekly_progress).uppercase())
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.done).uppercase()).performClick()

        assertThat(done).isTrue()
    }
}
