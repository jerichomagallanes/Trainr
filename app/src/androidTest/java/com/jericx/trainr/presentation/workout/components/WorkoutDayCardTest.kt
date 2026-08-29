package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private fun setCard(duration: Int, exerciseCount: Int) {
        val day = WorkoutDay(
            id = 1,
            dayNumber = 1,
            title = "Full Body Strength",
            status = WorkoutStatus.NOT_STARTED,
            duration = duration,
            exerciseCount = exerciseCount,
            equipment = listOf("Dumbbells")
        )
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutDayCard(weekday = "Monday", day = day, onClick = {})
            }
        }
    }

    @Test
    fun severalMinutesAndExercisesReadAsPlurals() {
        setCard(duration = 45, exerciseCount = 6)

        composeTestRule.onNodeWithText("45 mins").assertIsDisplayed()
        composeTestRule.onNodeWithText("6 Exercises").assertIsDisplayed()
    }

    // A one-minute, one-exercise day must not read "1 mins" / "1 Exercises".
    @Test
    fun oneMinuteAndOneExerciseReadAsSingulars() {
        setCard(duration = 1, exerciseCount = 1)

        composeTestRule.onNodeWithText("1 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Exercise").assertIsDisplayed()
    }
}
