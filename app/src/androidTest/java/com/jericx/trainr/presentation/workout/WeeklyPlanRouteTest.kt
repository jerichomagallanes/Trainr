package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.OneWeekRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The route is where the screen meets the rest of the app, and a callback it
// accepts but forgets to pass on becomes a control that draws and does nothing.
// That has happened twice — once when a week opened from the list kept the
// plan's menu, and once when this file gained a callback the route dropped —
// and both times the screen's own tests passed, because they never go through
// the route. These do.
@RunWith(AndroidJUnit4::class)
class WeeklyPlanRouteTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    // Today, untouched: the week being trained, with everything still to do.
    private val liveWeek = SampleWorkoutData.weekOne.copy(
        startDateMillis = WorkoutWeek.startOfDay(),
        workoutDays = SampleWorkoutData.weekOne.workoutDays.map {
            it.copy(status = WorkoutStatus.NOT_STARTED)
        }
    )

    private fun setRoute(
        onDayClick: (com.jericx.trainr.domain.model.WorkoutDay) -> Unit = {},
        onStartTodayClick: (com.jericx.trainr.domain.model.WorkoutDay) -> Unit = {},
        onRegenerateWeekClick: () -> Unit = {},
        onRepeatWeekClick: () -> Unit = {},
        onTrackProgressClick: () -> Unit = {},
        onUpdateProfileClick: () -> Unit = {}
    ) {
        // Built out here rather than inside the composable: the same view model
        // instance for the whole test, and nothing constructed during
        // composition.
        val viewModel = WeeklyPlanViewModel(SavedStateHandle(), OneWeekRepository(liveWeek))

        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanRoute(
                    onDayClick = onDayClick,
                    onStartTodayClick = onStartTodayClick,
                    onRegenerateWeekClick = onRegenerateWeekClick,
                    onRepeatWeekClick = onRepeatWeekClick,
                    onTrackProgressClick = onTrackProgressClick,
                    onUpdateProfileClick = onUpdateProfileClick,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun theRouteHandsOnTheDayActions() {
        var started = false
        setRoute(onStartTodayClick = { started = true })

        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()

        assertThat(started).isTrue()
    }

    @Test
    fun theRouteHandsOnGeneratingThisWeekAgain() {
        var regenerated = false
        setRoute(onRegenerateWeekClick = { regenerated = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options)).performClick()
        composeTestRule.onNodeWithText(string(R.string.regenerate_week)).performClick()

        assertThat(regenerated).isTrue()
    }

    @Test
    fun theRouteHandsOnTheProgressLinkAndTheProfile() {
        var progress = false
        var profile = false
        setRoute(onTrackProgressClick = { progress = true }, onUpdateProfileClick = { profile = true })

        composeTestRule.onNodeWithText(string(R.string.track_weekly_progress) + " →").performClick()
        assertThat(progress).isTrue()

        composeTestRule.onNodeWithContentDescription(string(R.string.profile_and_app)).performClick()
        composeTestRule.onNodeWithText(string(R.string.update_profile)).performClick()
        assertThat(profile).isTrue()
    }

    @Test
    fun theRouteShowsTheStoredWeekRatherThanAPlaceholder() {
        setRoute()

        composeTestRule.onNodeWithText(liveWeek.workoutDays.first().title).assertIsDisplayed()
    }
}
