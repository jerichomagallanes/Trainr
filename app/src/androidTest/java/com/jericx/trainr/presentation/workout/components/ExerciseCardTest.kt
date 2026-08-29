package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.sample.SampleRoutine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setCard(exercise: ExerciseUi, onToggle: () -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme { ExerciseCard(exercise = exercise, onToggleCompleted = onToggle) }
        }
    }

    @Test
    fun showsThePrescriptionAndDuration() {
        setCard(SampleRoutine.cardioAndCore.exercises[1])

        composeTestRule.onNodeWithText("High-Intensity Intervals").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 sets of 1 minute").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 mins").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun anUnfinishedExerciseOffersToBeCompleted() {
        setCard(SampleRoutine.cardioAndCore.exercises[1])

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_complete))
            .assertIsDisplayed()
    }

    @Test
    fun aFinishedExerciseOffersToBeUndone() {
        setCard(SampleRoutine.cardioAndCore.exercises[0])

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_incomplete))
            .assertIsDisplayed()
    }

    @Test
    fun tappingTheCheckboxReportsTheToggle() {
        var toggled = false
        setCard(SampleRoutine.cardioAndCore.exercises[1], onToggle = { toggled = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_complete))
            .performClick()

        assertThat(toggled).isTrue()
    }
}
