package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import com.jericx.trainr.domain.model.WorkoutStatus
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

    // The sample week is dated in the past, so every test says when "today" is:
    // it decides which sessions are missed, and so which of them can be moved.
    private val state = WeeklyPlanViewModel.stateFor(
        plan = SampleWorkoutData.weekOne,
        isSample = true,
        nowMillis = SampleWorkoutData.dateOf(3)
    )

    private fun setScreen(
        onDayClick: (WorkoutDay) -> Unit = {},
        onStartTodayClick: (WorkoutDay) -> Unit = {},
        onLeavePlanConfirmed: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(
                    state = state,
                    onDayClick = onDayClick,
                    onStartTodayClick = onStartTodayClick,
                    onLeavePlanConfirmed = onLeavePlanConfirmed
                )
            }
        }
    }

    // A week opened from Weekly Progress is a record to read: it needs a way
    // back, and must not offer to regenerate, start today, or bounce the
    // reader back to the progress screen they arrived from.
    @Test
    fun aBrowsedWeekOffersAWayBackAndNoPlanActions() {
        var wentBack = false
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(state = state, onBackClick = { wentBack = true })
            }
        }

        composeTestRule.onNodeWithText(string(R.string.start_todays_workout))
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.track_weekly_progress) + " →")
            .assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertThat(wentBack).isTrue()
    }

    // Home is the plan being trained: no back arrow, all actions present.
    @Test
    fun theHomePlanKeepsItsActionsAndHasNoWayBack() {
        setScreen()

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options))
            .assertIsDisplayed()
    }

    // The CTA has to name a day, and it should be the one still to do.
    @Test
    fun startingTodaysWorkoutOpensTheFirstOutstandingDay() {
        var started: WorkoutDay? = null
        setScreen(onStartTodayClick = { started = it })

        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()

        assertThat(started?.title).isEqualTo("Cardio & Core")
    }

    private fun openMenu() {
        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options)).performClick()
    }

    // Changing the profile has to be reachable without the destructive
    // regenerate, which is the only other way in.
    @Test
    fun theMenuOffersUpdatingTheProfile() {
        var asked = false
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(state = state, onUpdateProfileClick = { asked = true })
            }
        }

        openMenu()
        composeTestRule.onNodeWithText(string(R.string.update_profile)).performClick()

        assertThat(asked).isTrue()
    }

    // A finished week can roll straight into the next one.
    @Test
    fun aFinishedWeekOffersStartingTheNextOne() {
        val finished = WeeklyPlanViewModel.stateFor(
            plan = SampleWorkoutData.weekOne.copy(
                workoutDays = SampleWorkoutData.weekOne.workoutDays.map {
                    it.copy(status = WorkoutStatus.COMPLETED)
                }
            ),
            isSample = false
        )
        var started = false
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(state = finished, onStartNextWeekClick = { started = true })
            }
        }

        openMenu()
        composeTestRule.onNodeWithText(string(R.string.start_next_week)).performClick()

        assertThat(started).isTrue()
    }

    // A week behind you can be progressed from or run again; the second is a
    // coaching decision, so it sits beside the first rather than happening on
    // its own when something fails.
    @Test
    fun aFinishedWeekCanAlsoBeRepeated() {
        val finished = WeeklyPlanViewModel.stateFor(
            plan = SampleWorkoutData.weekOne.copy(
                workoutDays = SampleWorkoutData.weekOne.workoutDays.map {
                    it.copy(status = WorkoutStatus.COMPLETED)
                }
            ),
            isSample = false
        )
        var repeated = false
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(state = finished, onRepeatWeekClick = { repeated = true })
            }
        }

        openMenu()
        composeTestRule.onNodeWithText(string(R.string.repeat_this_week)).performClick()

        assertThat(repeated).isTrue()
    }

    @Test
    fun anUnfinishedWeekOffersNeitherWayOn() {
        setScreen()

        openMenu()

        composeTestRule.onNodeWithText(string(R.string.repeat_this_week)).assertDoesNotExist()
    }

    @Test
    fun anUnfinishedWeekDoesNotOfferToStartTheNextOne() {
        setScreen()

        openMenu()

        composeTestRule.onNodeWithText(string(R.string.start_next_week)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.regenerate_plan)).assertIsDisplayed()
    }

    private fun openRegenerate() {
        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options)).performClick()
        composeTestRule.onNodeWithText(string(R.string.regenerate_plan)).performClick()
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

    @Test
    fun regeneratingAsksBeforeLeavingThePlan() {
        var left = false
        setScreen(onLeavePlanConfirmed = { left = true })

        openRegenerate()

        composeTestRule.onNodeWithText(string(R.string.leave_plan_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.leave_plan_message)).assertIsDisplayed()
        assertThat(left).isFalse()
    }

    // The plan is home: nothing to go back to, so no back arrow to mislead.
    @Test
    fun theHomeScreenOffersNoBackArrow() {
        setScreen()

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).assertDoesNotExist()
    }

    @Test
    fun cancellingKeepsYouOnThePlan() {
        var left = false
        setScreen(onLeavePlanConfirmed = { left = true })

        openRegenerate()
        composeTestRule.onNodeWithText(string(R.string.cancel)).performClick()

        composeTestRule.onNodeWithText(string(R.string.leave_plan_title)).assertDoesNotExist()
        assertThat(left).isFalse()
        composeTestRule.onNodeWithText(string(R.string.your_weekly_workout_plan)).assertIsDisplayed()
    }

    @Test
    fun confirmingLeavesSoThePlanCanBeRegenerated() {
        var left = false
        setScreen(onLeavePlanConfirmed = { left = true })

        openRegenerate()
        composeTestRule.onNodeWithText(string(R.string.leave_plan_confirm)).performClick()

        assertThat(left).isTrue()
    }
    private val freshWeek = WeeklyPlanViewModel.stateFor(
        plan = SampleWorkoutData.weekOne.copy(
            workoutDays = SampleWorkoutData.weekOne.workoutDays.map {
                it.copy(status = WorkoutStatus.NOT_STARTED)
            }
        ),
        isSample = false,
        nowMillis = SampleWorkoutData.dateOf(1)
    )

    // Long-press lifts a session; dragging it past the next card hands it that
    // card's weekday and reports the move.
    @Test
    fun draggingASessionPastTheNextOneReschedulesIt() {
        var move: Pair<Int, Int>? = null
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(state = freshWeek, onMoveDay = { from, to -> move = from to to })
            }
        }

        val first = freshWeek.days[0].day.title
        val secondCardHeight = composeTestRule.onNodeWithText(freshWeek.days[1].day.title)
            .fetchSemanticsNode().size.height

        composeTestRule.onNodeWithText(first).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, secondCardHeight * 0.4f))
            advanceEventTime(32)
            moveBy(Offset(0f, secondCardHeight * 0.4f))
            advanceEventTime(32)
            up()
        }
        composeTestRule.waitForIdle()

        assertThat(move).isEqualTo(0 to 1)
    }

    // A finished session is the record of a date it was trained on.
    @Test
    fun aFinishedSessionDoesNotLift() {
        var move: Pair<Int, Int>? = null
        val withFinishedFirst = WeeklyPlanViewModel.stateFor(
            plan = SampleWorkoutData.weekOne.copy(
                workoutDays = SampleWorkoutData.weekOne.workoutDays.mapIndexed { index, day ->
                    day.copy(
                        status = if (index == 0) WorkoutStatus.COMPLETED
                        else WorkoutStatus.NOT_STARTED
                    )
                }
            ),
            isSample = false,
            nowMillis = SampleWorkoutData.dateOf(1)
        )
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(
                    state = withFinishedFirst,
                    onMoveDay = { from, to -> move = from to to }
                )
            }
        }

        val height = composeTestRule.onNodeWithText(withFinishedFirst.days[1].day.title)
            .fetchSemanticsNode().size.height
        composeTestRule.onNodeWithText(withFinishedFirst.days[0].day.title).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, height * 0.9f))
            advanceEventTime(32)
            up()
        }
        composeTestRule.waitForIdle()

        assertThat(move).isNull()
    }

    // Found by hand: a week opened from Weekly Progress let its cards be
    // dragged into a new order that was never saved, so the screen showed a
    // move that had not happened.
    @Test
    fun aBrowsedWeekDoesNotLetItsSessionsBeDragged() {
        var move: Pair<Int, Int>? = null
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(
                    state = freshWeek,
                    onBackClick = {},
                    onMoveDay = { from, to -> move = from to to }
                )
            }
        }

        val first = freshWeek.days[0].day.title
        val secondCardHeight = composeTestRule.onNodeWithText(freshWeek.days[1].day.title)
            .fetchSemanticsNode().size.height

        composeTestRule.onNodeWithText(first).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, secondCardHeight * 0.8f))
            advanceEventTime(32)
            up()
        }
        composeTestRule.waitForIdle()

        assertThat(move).isNull()
        // The order on screen is still the plan's own.
        composeTestRule.onAllNodesWithText(freshWeek.days[0].day.title)
            .fetchSemanticsNodes().let { assertThat(it).hasSize(1) }
    }

    private val weekGoneBy = WeeklyPlanViewModel.stateFor(
        plan = SampleWorkoutData.weekOne.copy(
            workoutDays = SampleWorkoutData.weekOne.workoutDays.map {
                it.copy(status = WorkoutStatus.NOT_STARTED)
            }
        ),
        isSample = false,
        // Looked at after the week has run out, so every session was missed.
        nowMillis = SampleWorkoutData.dateOf(9)
    )

    // Missed is stated plainly and neutrally: the app says where you stand
    // without turning a quiet week into a scolding.
    @Test
    fun aSessionWhoseDateHasGoneReadsAsMissed() {
        composeTestRule.setContent {
            TrainrTheme { WeeklyPlanScreen(state = weekGoneBy) }
        }

        assertThat(
            composeTestRule.onAllNodesWithText(string(R.string.missed)).fetchSemanticsNodes()
        ).hasSize(weekGoneBy.days.size)
        // Nothing is scheduled today, so the button does not claim there is.
        composeTestRule.onNodeWithText(string(R.string.start_next_workout)).assertIsDisplayed()
    }

    // The past is a record: a missed session holds its date until it is either
    // trained or rescheduled, and dragging is not how that happens.
    @Test
    fun aMissedSessionDoesNotLift() {
        var move: Pair<Int, Int>? = null
        composeTestRule.setContent {
            TrainrTheme {
                WeeklyPlanScreen(state = weekGoneBy, onMoveDay = { from, to -> move = from to to })
            }
        }

        val height = composeTestRule.onNodeWithText(weekGoneBy.days[1].day.title)
            .fetchSemanticsNode().size.height
        composeTestRule.onNodeWithText(weekGoneBy.days[0].day.title).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, height * 0.9f))
            advanceEventTime(32)
            up()
        }
        composeTestRule.waitForIdle()

        assertThat(move).isNull()
    }

}
