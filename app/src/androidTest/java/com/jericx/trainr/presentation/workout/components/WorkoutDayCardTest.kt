package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDayCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setCard(
        duration: Int,
        exerciseCount: Int,
        status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
        finishedEarly: Boolean = false
    ) {
        val day = WorkoutDay(
            id = 1,
            dayNumber = 1,
            title = "Full Body Strength",
            status = status,
            duration = duration,
            exerciseCount = exerciseCount,
            equipment = listOf("Dumbbells")
        )
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutDayCard(
                    weekday = "Monday",
                    day = day,
                    onClick = {},
                    finishedEarly = finishedEarly
                )
            }
        }
    }

    @Test
    fun severalMinutesAndExercisesReadAsPlurals() {
        setCard(duration = 45, exerciseCount = 6)

        composeTestRule.onNodeWithText("45 mins").assertIsDisplayed()
        composeTestRule.onNodeWithText("6 Exercises").assertIsDisplayed()
    }

    @Test
    fun oneMinuteAndOneExerciseReadAsSingulars() {
        setCard(duration = 1, exerciseCount = 1)

        composeTestRule.onNodeWithText("1 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Exercise").assertIsDisplayed()
    }

    @Test
    fun aDayFinishedEarlySaysSoInsteadOfCompleted() {
        setCard(duration = 45, exerciseCount = 6, status = WorkoutStatus.COMPLETED, finishedEarly = true)

        composeTestRule.onNodeWithText(string(R.string.finished_early)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.completed)).assertDoesNotExist()
    }

    @Test
    fun aDayFinishedInFullStillReadsCompleted() {
        setCard(duration = 45, exerciseCount = 6, status = WorkoutStatus.COMPLETED)

        composeTestRule.onNodeWithText(string(R.string.completed)).assertIsDisplayed()
    }
}
