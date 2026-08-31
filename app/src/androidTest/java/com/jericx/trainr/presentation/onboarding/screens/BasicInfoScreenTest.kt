package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.common.Constants
import com.jericx.trainr.testing.notEllipsized
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assert
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

    private fun setupProgress() = composeTestRule.onAllNodes(
        hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 1f, range = 0f..7f))
    )

    // The bar counts the way through first-time setup.
    @Test
    fun firstTimeSetupShowsHowFarThroughItYouAre() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        setupProgress().assertCountEquals(1)
    }

    // Coming back to change one answer is not a seventh of anything.
    @Test
    fun changingOneAnswerLaterCountsNothing() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(
                    isEditing = true,
                    onNextClick = { _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        setupProgress().assertCountEquals(0)
    }

    // "Female" rendered as "Fema..." on narrower phones; the chip label must
    // shrink, never truncate.
    @Test
    fun everyGenderChipShowsItsWholeLabel() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        listOf(R.string.male, R.string.female, R.string.other).forEach { label ->
            composeTestRule.onNodeWithText(string(label))
                .performScrollTo()
                .assert(notEllipsized())
        }
    }

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

    // The range was already enforced, silently: an age of 5 left the button
    // dead with nothing on screen to explain it.
    @Test
    fun anAgeOutsideTheRangeSaysWhatTheRangeIs() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.enter_your_age)).performTextInput("5")

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                R.string.value_range_hint,
                composeTestRule.activity.getString(R.string.age_label),
                Constants.Workout.MIN_AGE.toString(),
                Constants.Workout.MAX_AGE.toString()
            )
        ).assertIsDisplayed()
    }

    // A field nobody has touched must not open already complaining.
    @Test
    fun anUntouchedAgeFieldSaysNothing() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                R.string.value_range_hint,
                composeTestRule.activity.getString(R.string.age_label),
                Constants.Workout.MIN_AGE.toString(),
                Constants.Workout.MAX_AGE.toString()
            )
        ).assertDoesNotExist()
    }

    // A name is whatever its owner says it is, so the only rule is that there
    // is one, and it is only asked for once the field has been left empty.
    @Test
    fun leavingTheNameEmptySaysItIsRequired() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        val required = composeTestRule.activity.getString(
            R.string.field_required,
            composeTestRule.activity.getString(R.string.name_label)
        )
        composeTestRule.onNodeWithText(required).assertDoesNotExist()

        // Focus the name, then move to the age field to leave it.
        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name)).performClick()
        composeTestRule.onNodeWithText(string(R.string.enter_your_age)).performClick()

        composeTestRule.onNodeWithText(required).assertIsDisplayed()
    }
}
