package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.testing.notEllipsized
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.EquipmentChoices

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
}
