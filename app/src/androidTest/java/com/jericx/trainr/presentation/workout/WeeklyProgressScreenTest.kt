package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.WeekProgressUi
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeeklyProgressScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(onWeekClick: (WeekProgressUi) -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyProgressScreen(
                    weeks = SampleWeeklyProgress.weeks,
                    onWeekClick = onWeekClick
                )
            }
        }
    }

    @Test
    fun showsTheScreenHeading() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.weekly_progress)).assertIsDisplayed()
    }

    @Test
    fun showsEveryWeekWithItsCompletionCount() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.week_number_format, 1), substring = true)
            .assertIsDisplayed()

        // Weeks 1, 5 and 7 are all fully completed, so this row repeats.
        val fullyCompleted = composeTestRule
            .onAllNodesWithText(string(R.string.days_completed_format, 3, 3, 100))
            .fetchSemanticsNodes()
        assertThat(fullyCompleted).hasSize(3)

        composeTestRule.onNodeWithText(string(R.string.days_completed_format, 0, 3, 0))
            .assertIsDisplayed()
    }

    // Four statuses across seven weeks; every one should be on screen somewhere.
    @Test
    fun showsAllFourWeekStatuses() {
        setScreen()

        listOf(R.string.completed, R.string.in_progress, R.string.not_completed, R.string.skipped)
            .forEach { status ->
                assertThat(
                    composeTestRule.onAllNodesWithText(string(status)).fetchSemanticsNodes()
                ).isNotEmpty()
            }
    }

    @Test
    fun tappingAWeekReportsIt() {
        var tapped: WeekProgressUi? = null
        setScreen(onWeekClick = { tapped = it })

        composeTestRule.onNodeWithText(string(R.string.days_completed_format, 0, 3, 0)).performClick()

        assertThat(tapped?.weekNumber).isEqualTo(3)
    }
}
