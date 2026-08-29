package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeeklyPlanScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private val state = WeeklyPlanViewModel.stateFor(SampleWorkoutData.weekOne, isSample = true)

    private fun setScreen(
        onDayClick: (WorkoutDay) -> Unit = {},
        onStartTodayClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(
                    state = state,
                    onDayClick = onDayClick,
                    onStartTodayClick = onStartTodayClick
                )
            }
        }
    }

    @Test
    fun showsThePlanHeading() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.your_weekly_workout_plan))
            .assertIsDisplayed()
    }

    @Test
    fun showsEveryWorkoutDayInThePlan() {
        setScreen()

        composeTestRule.onNodeWithText("Full Body Strength").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cardio & Core").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lower Body Power").assertIsDisplayed()
    }

    @Test
    fun showsTheWeekdayEachWorkoutFallsOn() {
        setScreen()

        composeTestRule.onNodeWithText("Monday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wednesday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friday").assertIsDisplayed()
    }

    @Test
    fun showsEachWorkoutsStatus() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.completed)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.in_progress)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.not_started)).assertIsDisplayed()
    }

    @Test
    fun tappingACardReportsThatDay() {
        var tapped: WorkoutDay? = null
        setScreen(onDayClick = { tapped = it })

        composeTestRule.onNodeWithText("Cardio & Core").performClick()

        assertThat(tapped?.title).isEqualTo("Cardio & Core")
    }

    @Test
    fun tappingTheCallToActionStartsTodaysWorkout() {
        var started = false
        setScreen(onStartTodayClick = { started = true })

        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()

        assertThat(started).isTrue()
    }
}
