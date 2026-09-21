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
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
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
class SessionSavedScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    private fun plural(id: Int, count: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(id, count, *args)

    private val day = SampleWorkoutData.weekOne.workoutDays.first()

    private fun setScreen(onDoneClick: () -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme {
                SessionSavedScreen(
                    performedExercises = 4,
                    plannedExercises = 6,
                    onDoneClick = onDoneClick
                )
            }
        }
    }

    private fun setRoute(
        adjustments: InMemoryAdjustmentRepository = InMemoryAdjustmentRepository(),
        onFeedback: (Long) -> Unit = {}
    ) {
        val viewModel = FeedbackPromptViewModel(
            SavedStateHandle(mapOf(Screen.SessionSaved.ARG_DAY_NUMBER to day.dayNumber)),
            OneWeekRepository(SampleWorkoutData.weekOne),
            adjustments
        )

        composeTestRule.setContent {
            TrainrTheme {
                SessionSavedRoute(
                    performedExercises = 4,
                    plannedExercises = 6,
                    onFeedback = onFeedback,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun adjustedDay(answer: FeedbackAnswer? = null, answered: Boolean = false) =
        InMemoryAdjustmentRepository().also { repository ->
            runBlocking {
                val id = repository.recordAdjustment(
                    AppliedAdjustment(
                        workoutDayId = day.id,
                        proposal = proposal(),
                        reason = AdjustmentReason.LESS_TIME,
                        appliedAt = 1L
                    )
                )
                if (answered) {
                    repository.saveFeedback(
                        AdjustmentFeedback(
                            adjustmentId = id,
                            answer = answer,
                            answeredAt = answer?.let { 2L },
                            dismissedAt = if (answer == null) 2L else null
                        )
                    )
                }
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

    @Test
    fun saysTheWorkoutWasSavedAndHowMuchWasDone() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.workout_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText(plural(R.plurals.finished_early_summary_format, 6, 4, 6))
            .assertIsDisplayed()
    }

    @Test
    fun doneIsTheOnlyWayOnward() {
        var done = false
        setScreen(onDoneClick = { done = true })

        composeTestRule.onNodeWithText(string(R.string.view_weekly_progress).uppercase())
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.done).uppercase()).performClick()

        assertThat(done).isTrue()
    }

    @Test
    fun anUnadjustedSessionIsAskedNothing() {
        setRoute()

        composeTestRule.onNodeWithText(string(R.string.anything_to_change_title))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.tell_us_how_it_went)).assertDoesNotExist()
    }

    @Test
    fun anAdjustedSessionOffersTheQuestionWithoutBlockingTheSave() {
        var offered: Long? = null
        setRoute(adjustments = adjustedDay(), onFeedback = { offered = it })

        composeTestRule.onNodeWithText(string(R.string.workout_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.anything_to_change_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.anything_to_change_body))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tell_us_how_it_went)).performClick()

        assertThat(offered).isNotNull()
    }

    @Test
    fun anAnsweredAdjustmentIsNotAskedAgain() {
        setRoute(adjustments = adjustedDay(answer = FeedbackAnswer.HELPED, answered = true))

        composeTestRule.onNodeWithText(string(R.string.tell_us_how_it_went)).assertDoesNotExist()
    }

    @Test
    fun aDismissedOfferIsNotAskedAgain() {
        setRoute(adjustments = adjustedDay(answered = true))

        composeTestRule.onNodeWithText(string(R.string.tell_us_how_it_went)).assertDoesNotExist()
    }
}
