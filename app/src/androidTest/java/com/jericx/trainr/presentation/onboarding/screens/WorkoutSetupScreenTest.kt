package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.testing.notEllipsized
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.EquipmentChoices
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile

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
                    onNextClick = { _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.set_up_your_workout))
            .assertIsDisplayed()
    }

    // The laid-out chip text must never visually overflow. Asserted without
    // scrolling to it: the screen is one scrolling Column, so every chip is
    // composed and measured, and scrolling past nine wrapping chips is what
    // this never settles behind.
    @Test
    fun everyDurationChipShowsItsWholeLabel() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        listOf(30, 45, 60, 90).forEach { minutes ->
            composeTestRule.onNodeWithText(
                composeTestRule.activity.resources
                    .getQuantityString(R.plurals.minutes, minutes, minutes)
            ).assert(notEllipsized())
        }
    }

    // Equipment is the question now: every category the catalog stocks is on
    // the screen, with no answer about where someone stands in front of it.
    @Test
    fun displaysEveryEquipmentCategory() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        EquipmentChoices.forEach { kit ->
            composeTestRule.onNodeWithText(kit.label()).assertExists()
        }
    }

    // "Bodyweight only" is one of the choices, so an empty set means the
    // question is unanswered rather than that there is nothing available.
    @Test
    fun equipmentHasToBeAnsweredRatherThanAssumed() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    // A length the week cannot fill is said on the screen that asks for it,
    // rather than left to be found later as a day two thirds the size.
    @Test
    fun aSessionLengthTheWeekCannotFillSaysSoWhereItIsChosen() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    initial = answered,
                    longestSessionMinutes = { _, _, _ -> SHORT_SESSION },
                    onNextClick = { _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.resources
                .getQuantityString(R.plurals.sessions_fall_short_message, SHORT_SESSION, SHORT_SESSION)
        ).assertExists()
    }

    @Test
    fun aSessionLengthTheWeekCanFillIsLeftToStand() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    initial = answered,
                    longestSessionMinutes = { _, _, duration -> duration },
                    onNextClick = { _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.resources
                .getQuantityString(R.plurals.sessions_fall_short_message, SHORT_SESSION, SHORT_SESSION)
        ).assertDoesNotExist()
    }

    // The question appears above the fold once a weighted chip is picked, so an
    // unanswered one would leave NEXT disabled with nothing on screen to say so.
    @Test
    fun pickingWeightedKitAnswersTheUnitQuestionWithTheUnitsAlreadyChosen() {
        var captured: UnitSystem? = null
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    initial = answered,
                    bodyUnits = UnitSystem.IMPERIAL,
                    onNextClick = { _, liftingUnits, _, _ -> captured = liftingUnits },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.equipment_barbell))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(string(R.string.weights_marked_in)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()

        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(captured).isEqualTo(UnitSystem.IMPERIAL)
    }

    // Changing the answer is still the client's to make.
    @Test
    fun theUnitQuestionCanStillBeAnsweredDifferently() {
        var captured: UnitSystem? = null
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    initial = answered,
                    bodyUnits = UnitSystem.IMPERIAL,
                    onNextClick = { _, liftingUnits, _, _ -> captured = liftingUnits },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.equipment_barbell))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.weight_column))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(captured).isEqualTo(UnitSystem.METRIC)
    }

    private val answered = UserProfile(
        availableEquipment = listOf(Equipment.NONE),
        workoutDaysPerWeek = 3,
        workoutDuration = ASKED_SESSION
    )

    private fun Equipment.label(): String = when (this) {
        Equipment.NONE -> string(R.string.bodyweight_only)
        Equipment.BARBELL -> string(R.string.equipment_barbell)
        Equipment.DUMBBELL -> string(R.string.equipment_dumbbell)
        Equipment.KETTLEBELL -> string(R.string.equipment_kettlebell)
        Equipment.MACHINE -> string(R.string.equipment_machine)
        Equipment.PLATE -> string(R.string.equipment_plate)
        Equipment.RESISTANCE_BAND -> string(R.string.equipment_resistance_band)
        Equipment.SUSPENSION_BAND -> string(R.string.equipment_suspension_band)
        Equipment.OTHER -> string(R.string.equipment_other)
    }

    private companion object {
        const val ASKED_SESSION = 90
        const val SHORT_SESSION = 41
    }
}
