package com.jericx.trainr.presentation.onboarding.screens

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
class LimitationsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(onNextClick = {}, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.lets_keep_you_safe))
            .assertIsDisplayed()
    }

    @Test
    fun submitFiresWithDefaultsWhenNoSelectionsMade() {
        var capturedInjuries: List<String>? = null

        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(
                    onNextClick = { injuries -> capturedInjuries = injuries },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.submit)).performClick()

        assertThat(capturedInjuries).isEmpty()
    }

    // Workout style moved to the goals screen, where the review card that shows
    // it can actually reach it. It must not drift back here.
    @Test
    fun workoutStyleIsNotAskedOnThisScreen() {
        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(onNextClick = {}, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.preferred_workout_style))
            .assertDoesNotExist()
    }
}
