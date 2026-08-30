package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
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
import com.jericx.trainr.presentation.workout.model.WeekStatus
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

    private fun quantityString(id: Int, quantity: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(id, quantity, *args)

    private fun daysCompleted(completed: Int, total: Int, percentage: Int) =
        composeTestRule.activity.resources.getQuantityString(
            R.plurals.days_completed_format, total, completed, total, percentage
        )

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

        // Weeks 1, 5 and 7 are all fully completed, so this row repeats; the
        // skipped and upcoming weeks share an untouched 0/3.
        val fullyCompleted = composeTestRule
            .onAllNodesWithText(daysCompleted(3, 3, 100))
            .fetchSemanticsNodes()
        assertThat(fullyCompleted).hasSize(3)

        val untouched = composeTestRule
            .onAllNodesWithText(daysCompleted(0, 3, 0))
            .fetchSemanticsNodes()
        assertThat(untouched).hasSize(2)
    }

    // Five statuses across eight weeks; every one should be on screen somewhere.
    @Test
    fun showsEveryWeekStatus() {
        setScreen()

        listOf(
            R.string.completed,
            R.string.in_progress,
            R.string.not_completed,
            R.string.skipped,
            R.string.upcoming
        )
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

        composeTestRule.onNodeWithText(string(R.string.skipped)).performClick()

        assertThat(tapped?.weekNumber).isEqualTo(3)
    }

    // A one-day-per-week plan must not read "0/1 days completed".
    @Test
    fun aSingleScheduledDayUsesTheSingular() {
        val oneDayWeek = WeekProgressUi(
            weekNumber = 1,
            completedDays = 1,
            totalDays = 1,
            status = WeekStatus.COMPLETED,
            startDateMillis = SampleWeeklyProgress.weeks.first().startDateMillis,
            endDateMillis = SampleWeeklyProgress.weeks.first().endDateMillis
        )

        composeTestRule.setContent {
            TrainrTheme { WeeklyProgressScreen(weeks = listOf(oneDayWeek)) }
        }

        composeTestRule.onNodeWithText(daysCompleted(1, 1, 100)).assertIsDisplayed()
        composeTestRule.onNodeWithText("1/1 day completed (100%)").assertIsDisplayed()
    }
    // A week is a good deal more than a set, so the swipe asks first.
    @Test
    fun swipingAnUpcomingWeekAsksBeforeDeleting() {
        var deleted: WeekProgressUi? = null
        val upcoming = SampleWeeklyProgress.weeks.first { !it.hasTraining }
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyProgressScreen(
                    weeks = SampleWeeklyProgress.weeks,
                    onDeleteWeek = { deleted = it }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.week_number_format, upcoming.weekNumber), substring = true)
            .performScrollTo()
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.delete_week_title, upcoming.weekNumber))
            .assertIsDisplayed()
        assertThat(deleted).isNull()

        composeTestRule.onNodeWithText(string(R.string.delete_week_confirm)).performClick()

        assertThat(deleted?.weekNumber).isEqualTo(upcoming.weekNumber)
    }

    // Training already done is still the client's to drop — but the dialog
    // says what it costs before it goes.
    @Test
    fun deletingATrainedWeekNamesWhatItCosts() {
        val trained = SampleWeeklyProgress.weeks.first { it.hasTraining }
        composeTestRule.setContent {
            TrainrTheme { WeeklyProgressScreen(weeks = SampleWeeklyProgress.weeks) }
        }

        composeTestRule.onNodeWithText(
            string(R.string.week_number_format, trained.weekNumber),
            substring = true
        ).performScrollTo().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            quantityString(
                R.plurals.delete_week_message_trained,
                trained.completedDays,
                trained.completedDays,
                trained.totalDays
            )
        ).assertIsDisplayed()
    }

    // A swipe that cannot delete must not fall through as a tap and navigate.
    @Test
    fun aRefusedSwipeDoesNotOpenTheWeek() {
        var opened: WeekProgressUi? = null
        // A lone week cannot be deleted, so its swipe has nothing to do — and
        // must not quietly become a tap that opens it.
        val only = SampleWeeklyProgress.weeks.first()
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyProgressScreen(weeks = listOf(only), onWeekClick = { opened = it })
            }
        }

        composeTestRule.onNodeWithText(
            string(R.string.week_number_format, only.weekNumber),
            substring = true
        ).performScrollTo().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertThat(opened).isNull()
    }

}
