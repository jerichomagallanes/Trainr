package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
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
}
