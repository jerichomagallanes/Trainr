package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitnessGoalScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.your_fitness_goals))
            .assertIsDisplayed()
    }

    @Test
    fun nextDisabledUntilBothAnswersAreGiven() {
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()

        composeTestRule.onNodeWithText(string(R.string.build_muscle)).performClick()
        composeTestRule.onNodeWithText(string(R.string.hiit)).performScrollTo().performClick()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }

    @Test
    fun nextEmitsSelectedGoal() {
        var captured: FitnessGoal? = null
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = { goal, _ -> captured = goal }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.build_muscle)).performClick()
        composeTestRule.onNodeWithText(string(R.string.hiit)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(captured).isEqualTo(FitnessGoal.MUSCLE_GAIN)
    }

    // The review shows workout style on the same card as the main goal, so the
    // Edit beside it has to be able to change it. It used to open this screen
    // while the field itself lived on the limitations step.
    @Test
    fun nextEmitsTheChosenWorkoutStyleBesideTheGoal() {
        var capturedStyle: WorkoutType? = null
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(
                    onNextClick = { _, style -> capturedStyle = style },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.get_stronger)).performClick()
        composeTestRule.onNodeWithText(string(R.string.hiit)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(capturedStyle).isEqualTo(WorkoutType.HIIT)
    }

    // Mixed used to arrive pre-selected, so a client could reach the end of
    // onboarding having agreed to a training style they were never asked about.
    @Test
    fun noStyleIsChosenUntilTheClientChoosesOne() {
        var emitted = false
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = { _, _ -> emitted = true }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.lose_weight)).performClick()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()
        assertThat(emitted).isFalse()
    }

    @Test
    fun bothAnswersTogetherAreEnough() {
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = { _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.lose_weight)).performClick()
        composeTestRule.onNodeWithText(string(R.string.mixed_balanced)).performScrollTo().performClick()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }
}
