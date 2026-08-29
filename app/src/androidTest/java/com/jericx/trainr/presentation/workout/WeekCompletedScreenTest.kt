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
class WeekCompletedScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        weekNumber: Int = 1,
        onViewProgressClick: () -> Unit = {},
        onPreviewNextWeekClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                WeekCompletedScreen(
                    weekNumber = weekNumber,
                    onViewProgressClick = onViewProgressClick,
                    onPreviewNextWeekClick = onPreviewNextWeekClick
                )
            }
        }
    }

    @Test
    fun namesTheWeekThatWasCompleted() {
        setScreen(weekNumber = 3)

        composeTestRule.onNodeWithText(string(R.string.week_completed_format, 3)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.week_completed_message)).assertIsDisplayed()
    }

    @Test
    fun offersBothWaysOnward() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.view_weekly_progress).uppercase())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.preview_next_week).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun eachRouteOnwardReportsItself() {
        var viewedProgress = false
        var previewedNext = false
        setScreen(
            onViewProgressClick = { viewedProgress = true },
            onPreviewNextWeekClick = { previewedNext = true }
        )

        composeTestRule.onNodeWithText(string(R.string.view_weekly_progress).uppercase())
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.preview_next_week).uppercase())
            .performClick()

        assertThat(viewedProgress).isTrue()
        assertThat(previewedNext).isTrue()
    }

    // A plan on its first week must not read "Week 0".
    @Test
    fun theFirstWeekReadsAsWeekOne() {
        setScreen(weekNumber = 1)

        composeTestRule.onNodeWithText(string(R.string.week_completed_format, 1)).assertIsDisplayed()
    }
}
