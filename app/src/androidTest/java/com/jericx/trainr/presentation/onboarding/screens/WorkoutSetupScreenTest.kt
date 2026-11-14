package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.testing.notEllipsized
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.jericx.trainr.common.Constants
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.EquipmentChoices
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile

@RunWith(AndroidJUnit4::class)
class WorkoutSetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun minutesLabel(minutes: Int) = composeTestRule.activity.resources
        .getQuantityString(R.plurals.minutes, minutes, minutes)

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

    // The laid-out chip text must never visually overflow.
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

        Constants.Workout.DURATION_OPTIONS.forEach { minutes ->
            composeTestRule.onNodeWithText(minutesLabel(minutes)).assert(notEllipsized())
        }
    }

    @Test
    fun noDurationChipHangsPastTheForm() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(onNextClick = { _, _, _, _ -> }, onBackClick = {})
            }
        }

        assertDurationChipsFitTheForm()
    }

    // The form is taller at the largest text setting and the chips are not, so
    // the pixel a chip ends on is a different one.
    @Test
    fun noDurationChipHangsPastTheFormAtTheLargestTextSize() {
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale = LARGEST_TEXT)
            ) {
                TrainrTheme {
                    WorkoutSetupScreen(
                        onNextClick = { _, _, _, _ -> },
                        onBackClick = {}
                    )
                }
            }
        }

        assertDurationChipsFitTheForm()
    }

    // The form is the first of the two scrolling areas; the second is the bar
    // the button sits in.
    private fun assertDurationChipsFitTheForm() {
        Constants.Workout.DURATION_OPTIONS.forEach { minutes ->
            composeTestRule.onNodeWithText(minutesLabel(minutes)).performScrollTo()
            val chip = composeTestRule.onNodeWithText(minutesLabel(minutes))
                .getUnclippedBoundsInRoot()
            val form = composeTestRule.onAllNodes(hasScrollAction())[0]
                .getUnclippedBoundsInRoot()

            assertThat(chip.top.value).isAtLeast(form.top.value)
            assertThat(chip.bottom.value).isAtMost(form.bottom.value)
        }
    }

    // The step has to settle: a chip reached by scrolling is the only chip a
    // client ever taps, and a scroll onto one that hangs past the form by a
    // pixel is a scroll that can never arrive.
    @Test
    fun everyDurationChipCanBeScrolledToAndTapped() {
        var captured: Int? = null
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    initial = answered,
                    onNextClick = { _, _, _, duration -> captured = duration },
                    onBackClick = {}
                )
            }
        }

        // Tapped back to front: the step opens on the last option, so ending on
        // it would assert the answer the screen already carried.
        Constants.Workout.DURATION_OPTIONS.reversed().forEach { minutes ->
            composeTestRule.onNodeWithText(minutesLabel(minutes))
                .performScrollTo()
                .performClick()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(captured).isEqualTo(Constants.Workout.DURATION_OPTIONS.first())
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

    // "None" is one of the choices, so an empty set means the
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
        Equipment.NONE -> string(R.string.equipment_none)
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
        const val LARGEST_TEXT = 2f
        const val ASKED_SESSION = 90
        const val SHORT_SESSION = 41
    }
}
