package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicInfoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.tell_us_about_yourself))
            .assertIsDisplayed()
    }

    @Test
    fun nextButtonDisabledWhenFormIsEmpty() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    @Test
    fun nextButtonEnabledOnceAllRequiredFieldsAreFilled() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name))
            .performTextInput("Jericho")
        composeTestRule.onNodeWithText(string(R.string.enter_your_age))
            .performTextInput("30")
        composeTestRule.onNodeWithText(string(R.string.male)).performClick()
        composeTestRule.onNodeWithText(string(R.string.beginner_level)).performClick()

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }

    @Test
    fun nextCallbackReceivesEnteredValues() {
        var capturedFirstName = ""
        var capturedAge = -1
        var capturedGender: Gender? = null
        var capturedExperience: ExperienceLevel? = null

        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(
                    onNextClick = { firstName, age, gender, experience ->
                        capturedFirstName = firstName
                        capturedAge = age
                        capturedGender = gender
                        capturedExperience = experience
                    },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name))
            .performTextInput("Ana")
        composeTestRule.onNodeWithText(string(R.string.enter_your_age))
            .performTextInput("28")
        composeTestRule.onNodeWithText(string(R.string.female)).performClick()
        composeTestRule.onNodeWithText(string(R.string.intermediate_level)).performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(capturedFirstName).isEqualTo("Ana")
        assertThat(capturedAge).isEqualTo(28)
        assertThat(capturedGender).isEqualTo(Gender.FEMALE)
        assertThat(capturedExperience).isEqualTo(ExperienceLevel.INTERMEDIATE)
    }
}
