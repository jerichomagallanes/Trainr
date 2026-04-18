package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.FitnessGoal
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
                FitnessGoalScreen(onNextClick = {}, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.main_fitness_goal))
            .assertIsDisplayed()
    }

    @Test
    fun nextDisabledUntilGoalSelected() {
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = {}, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()

        composeTestRule.onNodeWithText(string(R.string.build_muscle)).performClick()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }

    @Test
    fun nextEmitsSelectedGoal() {
        var captured: FitnessGoal? = null
        composeTestRule.setContent {
            TrainrTheme {
                FitnessGoalScreen(onNextClick = { captured = it }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.build_muscle)).performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(captured).isEqualTo(FitnessGoal.MUSCLE_GAIN)
    }
}
