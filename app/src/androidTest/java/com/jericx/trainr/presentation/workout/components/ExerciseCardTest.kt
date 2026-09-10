package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.toRoutineUi
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private val sampleExercises = SampleWorkoutData
        .dayFor(SampleWorkoutData.DEFAULT_DAY_NUMBER)
        .toRoutineUi(catalog = SampleWorkoutData.catalog).exercises

    private fun setCard(exercise: ExerciseUi, onToggle: () -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme { ExerciseCard(exercise = exercise, onToggleCompleted = onToggle) }
        }
    }

    @Test
    fun showsThePrescriptionAndDuration() {
        setCard(sampleExercises[1])

        composeTestRule.onNodeWithText("High-Intensity Intervals").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 sets of 1 minute").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 mins").assertIsDisplayed()
        // The set rows are numbered too, so the badge is not the only "2" on the card.
        assertThat(
            composeTestRule.onAllNodesWithText("2").fetchSemanticsNodes()
        ).isNotEmpty()
    }

    @Test
    fun anUnfinishedExerciseOffersToBeCompleted() {
        setCard(sampleExercises[1])

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_complete))
            .assertIsDisplayed()
    }

    @Test
    fun aFinishedExerciseOffersToBeUndone() {
        setCard(sampleExercises[0])

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_incomplete))
            .assertIsDisplayed()
    }

    @Test
    fun tappingTheCheckboxReportsTheToggle() {
        var toggled = false
        setCard(sampleExercises[1], onToggle = { toggled = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_complete))
            .performClick()

        assertThat(toggled).isTrue()
    }

    @Test
    fun anExerciseWithNoSetsStillOffersAddSet() {
        setCard(sampleExercises[1].copy(sets = emptyList()))

        composeTestRule.onNodeWithText(string(R.string.add_set)).assertIsDisplayed()
    }

    // Deliberately still tickable: the slide to finish marks it complete anyway.
    @Test
    fun anExerciseWithNoSetsCanStillBeTickedOff() {
        var toggled = false
        setCard(sampleExercises[1].copy(sets = emptyList()), onToggle = { toggled = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.mark_exercise_complete))
            .performClick()

        assertThat(toggled).isTrue()
    }

    // What the movement trains is the catalog's to say, and it reads at a
    // glance rather than as two labelled lines on every card of the day.
    @Test
    fun theMuscleLineNamesWhatTheMovementTrains() {
        val squat = sampleExercises.first { it.primaryMuscle.isNotBlank() }

        setCard(squat)

        composeTestRule.onNodeWithText(squat.primaryMuscle, substring = true)
            .assertIsDisplayed()
        squat.secondaryMuscles.forEach { muscle ->
            composeTestRule.onNodeWithText(muscle, substring = true).assertIsDisplayed()
        }
    }

    // A movement the catalog has nothing to say about must not leave a stray
    // separator sitting under its name.
    @Test
    fun aMovementWithNoMusclesShowsNoMuscleLine() {
        val unknown = sampleExercises[0].copy(primaryMuscle = "", secondaryMuscles = emptyList())

        setCard(unknown)

        composeTestRule.onNodeWithText("\u00b7", substring = true).assertDoesNotExist()
    }
}
