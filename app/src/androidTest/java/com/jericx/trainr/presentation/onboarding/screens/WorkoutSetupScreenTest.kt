package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.testing.notEllipsized
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.set_up_your_workout))
            .assertIsDisplayed()
    }

    // "90 mins" once rendered as "90..." inside its fixed-width chip: the
    // laid-out text must never visually overflow.
    @Test
    fun everyDurationChipShowsItsWholeLabel() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        listOf(30, 45, 60, 90).forEach { minutes ->
            composeTestRule.onNodeWithText(
                composeTestRule.activity.resources
                    .getQuantityString(R.plurals.minutes, minutes, minutes)
            ).performScrollTo().assert(notEllipsized())
        }
    }

    @Test
    fun displaysLocationOptions() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.home)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.gym)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.both)).assertIsDisplayed()
    }

    // Days, duration and preferred time used to open on three, forty five and
    // the morning. A client could walk past all three and have the plan built
    // around answers they never gave.
    @Test
    fun choosingOnlyALocationIsNotEnoughToContinue() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(onNextClick = { _, _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.home)).performClick()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    // "Bodyweight only" is one of the choices, so an empty set means the
    // question is unanswered rather than that there is nothing available. It
    // used to be sent on as bodyweight regardless.
    @Test
    fun equipmentHasToBeAnsweredRatherThanAssumed() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(onNextClick = { _, _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.home)).performClick()
        composeTestRule.onNodeWithText(string(R.string.select_days_placeholder))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }
}
