package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSetTableUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setTable(previousSets: List<ExerciseSet>) {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseSetTable(
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    sets = listOf(
                        ExerciseSet(setNumber = 1, targetReps = 12, targetWeightKg = 20f),
                        ExerciseSet(setNumber = 2, targetReps = 12, targetWeightKg = 20f)
                    ),
                    onSetChanged = {},
                    onAddSet = {},
                    previousSets = previousSets
                )
            }
        }
    }

    @Test
    fun historyPutsAPreviousColumnOnTheTable() {
        setTable(
            previousSets = listOf(
                ExerciseSet(setNumber = 1, actualReps = 12, actualWeightKg = 20f)
            )
        )

        composeTestRule.onNodeWithText(string(R.string.previous_column)).assertIsDisplayed()
        composeTestRule.onNodeWithText("20kg × 12").assertIsDisplayed()
    }

    // Set 2 existed in the plan but was never done last time: dash, not target.
    @Test
    fun aRowBeyondTheHistoryShowsADash() {
        setTable(
            previousSets = listOf(
                ExerciseSet(setNumber = 1, actualReps = 12, actualWeightKg = 20f)
            )
        )

        composeTestRule.onNodeWithText("—").assertIsDisplayed()
    }

    // Week one has no history anywhere, and its card must look exactly like
    // the design, which has no PREVIOUS column.
    @Test
    fun noHistoryMeansNoColumn() {
        setTable(previousSets = emptyList())

        composeTestRule.onNodeWithText(string(R.string.previous_column)).assertDoesNotExist()
    }

    // Deleting every set used to take the Add set button with it, because the
    // card only drew the table when there were rows to put in it. That left an
    // exercise you could empty and never refill.
    @Test
    fun addSetSurvivesAnEmptiedTable() {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseSetTable(
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    sets = emptyList(),
                    onSetChanged = {},
                    onAddSet = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.add_set)).assertIsDisplayed()
    }

    // Headings over nothing are noise.
    @Test
    fun anEmptiedTableDropsItsColumnHeadings() {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseSetTable(
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    sets = emptyList(),
                    onSetChanged = {},
                    onAddSet = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.set_column)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.reps_column)).assertDoesNotExist()
    }
}
