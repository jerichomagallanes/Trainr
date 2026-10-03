package com.jericx.trainr.presentation.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.ReasonCode
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.unstuck.feedback.FeedbackPromptViewModel
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.testing.InMemoryAdjustmentRepository
import com.jericx.trainr.testing.OneWeekRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeekCompletedScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        weekNumber: Int = 1,
        onViewProgressClick: () -> Unit = {},
        onPreviewNextWeekClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                WeekCompletedScreen(
                    weekNumber = weekNumber,
                    onViewProgressClick = onViewProgressClick,
                    onPreviewNextWeekClick = onPreviewNextWeekClick
                )
            }
        }
    }

    @Test
    fun namesTheWeekThatWasCompleted() {
        setScreen(weekNumber = 3)

        composeTestRule.onNodeWithText(string(R.string.week_completed_format, 3)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.week_completed_message)).assertIsDisplayed()
    }

    @Test
    fun offersBothWaysOnward() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.view_weekly_progress).uppercase())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.generate_next_week).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun eachRouteOnwardReportsItself() {
        var viewedProgress = false
        var previewedNext = false
        setScreen(
            onViewProgressClick = { viewedProgress = true },
            onPreviewNextWeekClick = { previewedNext = true }
        )

        composeTestRule.onNodeWithText(string(R.string.view_weekly_progress).uppercase())
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.generate_next_week).uppercase())
            .performClick()

        assertThat(viewedProgress).isTrue()
        assertThat(previewedNext).isTrue()
    }

    @Test
    fun theFirstWeekReadsAsWeekOne() {
        setScreen(weekNumber = 1)

        composeTestRule.onNodeWithText(string(R.string.week_completed_format, 1)).assertIsDisplayed()
    }

    // The week that completes is the last day of it: a three-day week stores
    // 1, 3, 5, so the third session is the day numbered 5.
    private val day = SampleWorkoutData.weekOne.workoutDays.last()
    private val ordinal = SampleWorkoutData.weekOne.workoutDays.size

    private fun setRoute(
        adjustments: InMemoryAdjustmentRepository = InMemoryAdjustmentRepository(),
        onFeedback: (Long) -> Unit = {},
        onLeaveNote: () -> Unit = {}
    ) {
        val viewModel = FeedbackPromptViewModel(
            SavedStateHandle(mapOf(Screen.SessionSaved.ARG_DAY_NUMBER to ordinal)),
            OneWeekRepository(SampleWorkoutData.weekOne),
            adjustments
        )

        composeTestRule.setContent {
            TrainrTheme {
                WeekCompletedRoute(
                    weekNumber = 1,
                    onFeedback = onFeedback,
                    onLeaveNote = onLeaveNote,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun adjustedDay() = InMemoryAdjustmentRepository().also { repository ->
        runBlocking {
            repository.recordAdjustment(
                AppliedAdjustment(
                    workoutDayId = day.id,
                    proposal = proposal(),
                    reason = AdjustmentReason.LESS_TIME,
                    appliedAt = 1L
                )
            )
        }
    }

    private fun proposal() = AdjustmentProposal(
        proposalId = "proposal-1",
        requestId = "request-1",
        sessionId = "day:${day.id}",
        baseRevision = "revision-1",
        policyVersion = "policy-test",
        changes = listOf(
            ProposalChange(
                kind = ChangeKind.OMIT_UNPERFORMED,
                before = ExerciseSnapshot(
                    exerciseInstanceId = "exercise-1",
                    catalogKey = "goblet_squat",
                    sets = listOf(SetSnapshot("set-2", 10, 12.5f, null, 60))
                ),
                after = null
            )
        ),
        preservedPerformedSetIds = emptyList(),
        reasonCode = ReasonCode.TIME_CONSTRAINT,
        tradeoffCode = "reduced_session",
        factReferences = emptyList()
    )

    // The last day of a week is still a day worth a note, and an adjustment
    // made on it is still worth asking about.
    @Test
    fun theLastDayOfAWeekIsOfferedTheNoteAndTheQuestion() {
        var offered: Long? = null
        var noteAsked = false
        setRoute(adjustments = adjustedDay(), onFeedback = { offered = it }, onLeaveNote = { noteAsked = true })

        composeTestRule.onNodeWithText(string(R.string.anything_to_change_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tell_us_how_it_went)).performClick()
        composeTestRule.onNodeWithText(string(R.string.leave_a_note)).performClick()

        assertThat(offered).isNotNull()
        assertThat(noteAsked).isTrue()
    }

    @Test
    fun anUnadjustedWeekIsOfferedTheNoteAndNoQuestion() {
        var noteAsked = false
        setRoute(onLeaveNote = { noteAsked = true })

        composeTestRule.onNodeWithText(string(R.string.tell_us_how_it_went)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.leave_a_note)).performClick()

        assertThat(noteAsked).isTrue()
    }
}
