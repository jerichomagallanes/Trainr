package com.jericx.trainr.presentation.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.purchases.AdjustmentAllowance
import com.jericx.trainr.domain.time.ElapsedClock
import com.jericx.trainr.domain.time.WallClock
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.ReasonCode
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.domain.unstuck.TradeoffCode
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.HiltTestActivity
import com.jericx.trainr.testing.InMemoryAdjustmentRepository
import com.jericx.trainr.testing.OneWeekRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// A card that names one movement over a player demonstrating another can only
// be caught on screen, which is where the person reads the form from.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TutorialFollowsItsExerciseTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private object NoAllowance : AdjustmentAllowance {
        override fun includedCycleId(): String? = null
        override fun consume(cycleId: String) = Unit
        override fun restore(cycleId: String) = Unit
    }

    private val adjustments = InMemoryAdjustmentRepository()
    private val repository = OneWeekRepository(planWith(substitutedDay))

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setRoute() {
        val viewModel = RoutineDetailViewModel(
            SavedStateHandle(
                mapOf(
                    Screen.RoutineDetail.ARG_DAY_NUMBER to DAY_NUMBER,
                    Screen.RoutineDetail.ARG_WEEK_NUMBER to Screen.RoutineDetail.LATEST_WEEK
                )
            ),
            repository,
            adjustments,
            NoAllowance,
            SampleWorkoutData.catalog,
            ElapsedClock { 0L },
            WallClock { System.currentTimeMillis() }
        )
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailRoute(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(id: Int) =
        composeTestRule.onNodeWithText(string(id)).performScrollTo().performClick()

    private fun openTheSubstitutesVideo() {
        runBlocking { adjustments.recordAdjustment(replacement) }
        setRoute()

        composeTestRule.onNodeWithText(SUBSTITUTE_NAME).assertIsDisplayed()
        tap(R.string.show_how_to_perform)
        tap(R.string.show_video_tutorial)
        composeTestRule.onNodeWithText(string(R.string.hide_video_tutorial)).assertIsDisplayed()
    }

    @Test
    fun undoingAnAdjustmentLeavesNoPlayerOnTheWithdrawnSubstitute() {
        openTheSubstitutesVideo()

        repository.plan = planWith(restoredDay)
        tap(R.string.undo_adjustment)

        composeTestRule.onNodeWithText(ORIGINAL_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.hide_video_tutorial)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.hide_how_to_perform)).assertDoesNotExist()
    }

    @Test
    fun theRestoredMovementsOwnTutorialStillOpens() {
        openTheSubstitutesVideo()
        repository.plan = planWith(restoredDay)
        tap(R.string.undo_adjustment)

        tap(R.string.show_how_to_perform)
        tap(R.string.show_video_tutorial)

        composeTestRule.onNodeWithText(string(R.string.hide_video_tutorial)).assertIsDisplayed()
    }

    private companion object {
        const val DAY_NUMBER = 1
        const val DAY_ID = 71L
        const val ORIGINAL_NAME = "Goblet Squats"
        const val SUBSTITUTE_NAME = "Dumbbell Step-Ups"

        fun exercise(
            id: Long,
            key: String,
            name: String,
            videoId: String,
            omitted: Boolean = false
        ) = WorkoutExercise(
            id = id,
            exerciseKey = key,
            name = name,
            measure = ExerciseMeasure.WEIGHT_AND_REPS,
            sets = listOf(
                ExerciseSet(
                    id = id * 10,
                    setNumber = 1,
                    targetReps = 12,
                    targetWeightKg = 20f,
                    omittedBy = 1L.takeIf { omitted }
                )
            ),
            durationMinutes = 8,
            videoTutorialUrl = "https://www.youtube.com/watch?v=$videoId"
        )

        val original = exercise(82, "goblet_squat", ORIGINAL_NAME, "kDPxFoCmb-w")

        // What undo leaves behind: the original's sets are its own again and the
        // replacement, performed by nobody, is gone from the day.
        val restoredDay = listOf(original)

        val substitutedDay = listOf(
            original.copy(sets = original.sets.map { it.copy(omittedBy = 1L) }),
            exercise(83, "dumbbell_step_up", SUBSTITUTE_NAME, "dQw4w9WgXcQ")
        )

        fun planWith(exercises: List<WorkoutExercise>) = WeeklyWorkoutPlan(
            id = 9,
            userId = 1,
            weekNumber = 1,
            title = "Week 1",
            startDateMillis = WorkoutWeek.startOfDay(),
            workoutDays = listOf(
                WorkoutDay(
                    id = DAY_ID,
                    dayNumber = DAY_NUMBER,
                    title = "Lower Body",
                    status = WorkoutStatus.NOT_STARTED,
                    duration = 16,
                    exerciseCount = exercises.size,
                    equipment = listOf("Dumbbells"),
                    exercises = exercises
                )
            )
        )

        val replacement = AppliedAdjustment(
            workoutDayId = DAY_ID,
            proposal = AdjustmentProposal(
                proposalId = "proposal-1",
                requestId = "request-1",
                sessionId = "day:$DAY_ID",
                baseRevision = "revision-1",
                policyVersion = "test",
                changes = listOf(
                    ProposalChange(
                        kind = ChangeKind.REPLACE_UNPERFORMED,
                        before = ExerciseSnapshot(
                            exerciseInstanceId = "exercise:82",
                            catalogKey = "goblet_squat",
                            sets = listOf(SetSnapshot("set:820", 12, 20f, null, null))
                        ),
                        after = ExerciseSnapshot(
                            exerciseInstanceId = "exercise:83",
                            catalogKey = "dumbbell_step_up",
                            sets = listOf(SetSnapshot("set:830", 12, null, null, null))
                        )
                    )
                ),
                preservedPerformedSetIds = emptyList(),
                reasonCode = ReasonCode.EQUIPMENT_CONSTRAINT,
                tradeoffCode = TradeoffCode.DIFFERENT_RESISTANCE.wire,
                factReferences = emptyList()
            ),
            reason = AdjustmentReason.EQUIPMENT_UNAVAILABLE,
            appliedAt = 1L
        )
    }
}
