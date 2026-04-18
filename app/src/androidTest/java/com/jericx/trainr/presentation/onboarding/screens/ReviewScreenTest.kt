package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private val sampleProfile = UserProfile(
        firstName = "Jericho",
        age = 30,
        height = 175f,
        weight = 72f
    )

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.your_fitness_profile))
            .assertIsDisplayed()
    }

    @Test
    fun rendersProvidedFirstName() {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Jericho").assertIsDisplayed()
    }

    @Test
    fun confirmButtonFiresCallback() {
        var confirmed = false
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = { confirmed = true },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.generate_my_workout_plan))
            .performClick()

        assertThat(confirmed).isTrue()
    }
}
