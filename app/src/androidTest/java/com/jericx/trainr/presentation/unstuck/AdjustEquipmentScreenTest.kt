package com.jericx.trainr.presentation.unstuck

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
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustEquipmentScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setScreen(
        state: AdjustmentUiState = SampleAdjustmentStates.equipment,
        onToggleEquipment: (Equipment) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustEquipmentScreen(state = state, onToggleEquipment = onToggleEquipment)
            }
        }
    }

    // Coming in from the chooser leaves the exercise unanswered, and the
    // profile's broad kit is not an answer either.
    @Test
    fun theExerciseIsAskedForWhenNoneWasSupplied() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.adjust_equipment_exercise))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.show_recommendation).uppercase())
            .assertIsNotEnabled()
    }

    @Test
    fun anExerciseAndSomeKitEnableTheRecommendation() {
        val chosen = SampleAdjustmentStates.equipment.exerciseChoices.first()
        setScreen(state = SampleAdjustmentStates.equipment.copy(selectedExerciseId = chosen.id))

        composeTestRule.onNodeWithText(chosen.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.show_recommendation).uppercase())
            .assertIsEnabled()
    }

    @Test
    fun comingFromAnExerciseCardSkipsTheChooser() {
        setScreen(
            state = SampleAdjustmentStates.equipment.copy(
                selectedExerciseId = SampleAdjustmentStates.equipment.exerciseChoices.first().id,
                enteredWithExercise = true
            )
        )

        composeTestRule.onNodeWithText(string(R.string.adjust_equipment_exercise))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.show_recommendation).uppercase())
            .assertIsEnabled()
    }

    @Test
    fun togglingEquipmentReportsIt() {
        var toggled: Equipment? = null
        setScreen(onToggleEquipment = { toggled = it })

        composeTestRule.onNodeWithText(string(R.string.equipment_kettlebell)).performClick()

        assertThat(toggled).isEqualTo(Equipment.KETTLEBELL)
    }
}
