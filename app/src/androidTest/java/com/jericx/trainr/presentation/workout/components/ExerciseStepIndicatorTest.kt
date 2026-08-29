package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.stepsFor
import com.jericx.trainr.presentation.workout.sample.SampleRoutine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseStepIndicatorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setIndicator(currentPosition: Int) {
        composeTestRule.setContent {
            TrainrTheme {
                ExerciseStepIndicator(
                    steps = SampleRoutine.cardioAndCore.stepsFor(currentPosition)
                )
            }
        }
    }

    @Test
    fun numbersEveryExerciseInTheRoutine() {
        setIndicator(currentPosition = 3)

        (1..SampleRoutine.cardioAndCore.exercises.size).forEach { number ->
            composeTestRule.onNodeWithText(number.toString(), useUnmergedTree = true)
                .assertExists()
        }
    }

    // The dots carry no text of their own, so the progress has to be spoken.
    @Test
    fun readsOutWhereYouAreInTheRoutine() {
        setIndicator(currentPosition = 3)

        composeTestRule.onNodeWithContentDescription(
            string(R.string.exercise_step_progress, 3, 5)
        ).assertIsDisplayed()
    }

    @Test
    fun readsOutTheFirstAndLastStepsToo() {
        setIndicator(currentPosition = 1)

        composeTestRule.onNodeWithContentDescription(
            string(R.string.exercise_step_progress, 1, 5)
        ).assertIsDisplayed()
    }
}
