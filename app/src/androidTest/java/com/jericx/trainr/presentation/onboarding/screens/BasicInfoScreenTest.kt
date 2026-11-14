package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.Density
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

    @Test
    fun firstTimeSetupShowsHowFarThroughItYouAre() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        setupProgress().assertCountEquals(1)
    }

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

    // The chip label must shrink, never truncate.
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
        // The keyboard the age field raises shortens the form, so the answers
        // below it have to be brought back into view before they can be tapped.
        composeTestRule.onNodeWithText(string(R.string.male)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.beginner))
            .performScrollTo()
            .performClick()

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
        composeTestRule.onNodeWithText(string(R.string.female)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.intermediate))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(capturedFirstName).isEqualTo("Ana")
        assertThat(capturedAge).isEqualTo(28)
        assertThat(capturedGender).isEqualTo(Gender.FEMALE)
        assertThat(capturedExperience).isEqualTo(ExperienceLevel.INTERMEDIATE)
    }

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

    // The ask doubles as the field placeholder, so this counts nodes: one while
    // untouched, two once the error joins it.
    @Test
    fun leavingTheNameEmptyAsksForIt() {
        composeTestRule.setContent {
            TrainrTheme {
                BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        val asked = string(R.string.error_enter_name)
        composeTestRule.onAllNodesWithText(asked).assertCountEquals(1)

        // Focus the name, then move to the age field to leave it.
        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name)).performClick()
        composeTestRule.onNodeWithText(string(R.string.enter_your_age)).performClick()

        composeTestRule.onAllNodesWithText(asked).assertCountEquals(2)
    }

    // A chip shorter than the 48dp touch target is grown to it inside the height
    // the row gave it, and the body is drawn a pixel past that height whenever
    // the difference between the two rounds to an odd number of pixels.
    @Test
    fun noGenderChipHangsPastTheRowItIsLaidOutIn() {
        setBasicInfo(density = null)

        assertGenderChipsFitTheirRow()
    }

    // 2.5 is a density where the difference does round odd; the emulator's own
    // does not, so at that density alone the chips look fine either way.
    @Test
    fun noGenderChipHangsPastTheRowAtADensityThatRoundsOdd() {
        setBasicInfo(density = Density(2.5f, 1f))

        assertGenderChipsFitTheirRow()
    }

    private fun setBasicInfo(density: Density?) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides (density ?: LocalDensity.current)
            ) {
                TrainrTheme {
                    BasicInfoScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
                }
            }
        }
    }

    private fun assertGenderChipsFitTheirRow() {
        listOf(R.string.male, R.string.female, R.string.other).forEach { label ->
            composeTestRule.onNodeWithText(string(label)).performScrollTo()
            val chip = composeTestRule.onNodeWithText(string(label))
                .getUnclippedBoundsInRoot()
            val row = composeTestRule.onNodeWithTag("genderChoices").getUnclippedBoundsInRoot()

            assertThat(chip.top.value).isAtLeast(row.top.value)
            assertThat(chip.bottom.value).isAtMost(row.bottom.value)
        }
    }
}
