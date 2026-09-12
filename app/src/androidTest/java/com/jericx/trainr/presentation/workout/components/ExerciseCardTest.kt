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
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExerciseMeasure
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
    fun showsTheNameAndDuration() {
        setCard(sampleExercises[1])

        composeTestRule.onNodeWithText("High-Intensity Intervals").assertIsDisplayed()
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

    @Test
    fun theMuscleLineLabelsWhatTheMovementTrainsAndWhatAssists() {
        val press = sampleExercises[0].copy(
            primaryMuscle = "Chest",
            secondaryMuscles = listOf("Shoulders", "Triceps")
        )

        setCard(press)

        composeTestRule.onNodeWithText(
            "${string(R.string.muscle_primary_label)}: Chest  \u00b7  " +
                "${string(R.string.muscle_secondary_label)}: Shoulders, Triceps"
        ).assertIsDisplayed()
    }

    @Test
    fun aMovementWithNoSecondariesLabelsOnlyThePrimary() {
        val solo = sampleExercises[0].copy(primaryMuscle = "Chest", secondaryMuscles = emptyList())

        setCard(solo)

        composeTestRule.onNodeWithText("${string(R.string.muscle_primary_label)}: Chest").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.muscle_secondary_label), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun aMovementWithNoMusclesShowsNoMuscleLine() {
        val unknown = sampleExercises[0].copy(primaryMuscle = "", secondaryMuscles = emptyList())

        setCard(unknown)

        composeTestRule.onNodeWithText(string(R.string.muscle_primary_label), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun aMovementAnInjuryAsksCareWithSaysWhatToWatch() {
        setCard(sampleExercises[0].copy(caution = Injury.KNEE))

        composeTestRule.onNodeWithText(string(R.string.caution_knee)).assertIsDisplayed()
    }

    @Test
    fun aWeightNeverLiftedBeforeIsMarkedAsAGuess() {
        val weighted = sampleExercises[0].copy(
            measure = ExerciseMeasure.WEIGHT_AND_REPS,
            sets = listOf(ExerciseSet(setNumber = 1, targetReps = 10, targetWeightKg = 20f)),
            previousSets = emptyList()
        )

        setCard(weighted)

        composeTestRule.onNodeWithText(string(R.string.estimated_weight_note)).assertIsDisplayed()
    }

    @Test
    fun aWeightLiftedBeforeIsNotCalledAGuess() {
        val weighted = sampleExercises[0].copy(
            measure = ExerciseMeasure.WEIGHT_AND_REPS,
            sets = listOf(ExerciseSet(setNumber = 1, targetReps = 10, targetWeightKg = 20f)),
            previousSets = listOf(ExerciseSet(setNumber = 1, actualReps = 10, actualWeightKg = 20f, isCompleted = true))
        )

        setCard(weighted)

        composeTestRule.onNodeWithText(string(R.string.estimated_weight_note)).assertDoesNotExist()
    }
}
