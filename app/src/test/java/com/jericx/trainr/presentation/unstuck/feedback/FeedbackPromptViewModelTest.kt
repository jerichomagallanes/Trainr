package com.jericx.trainr.presentation.unstuck.feedback

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.domain.unstuck.planned
import com.jericx.trainr.domain.unstuck.testDay
import com.jericx.trainr.domain.unstuck.testUser
import com.jericx.trainr.presentation.Screen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackPromptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val day = testDay(planned("barbell_bench_press", sets = 3, id = 2))

    private val sameDayNextWeek = testDay(planned("barbell_bench_press", sets = 3, id = 3), id = 8)

    private val users: UserRepository = mockk<UserRepository>(relaxed = true).also {
        coEvery { it.getCurrentUser() } returns testUser()
        every { it.getWeeklyWorkoutPlans(any()) } returns flowOf(
            listOf(
                WeeklyWorkoutPlan(
                    id = 1,
                    userId = 0,
                    weekNumber = 1,
                    title = "Week 1",
                    startDateMillis = 0L,
                    workoutDays = listOf(day)
                ),
                WeeklyWorkoutPlan(
                    id = 2,
                    userId = 0,
                    weekNumber = 2,
                    title = "Week 2",
                    startDateMillis = 0L,
                    workoutDays = listOf(sameDayNextWeek)
                )
            )
        )
    }

    private fun adjustments(
        feedback: AdjustmentFeedback? = null,
        active: Boolean = true,
        nextWeek: Boolean = false
    ): AdjustmentRepository = mockk<AdjustmentRepository>(relaxed = true).also {
        coEvery { it.getActiveAdjustment(day.id) } returns
            if (active) appliedAdjustment(day.id) else null
        coEvery { it.getActiveAdjustment(sameDayNextWeek.id) } returns
            if (nextWeek) appliedAdjustment(sameDayNextWeek.id) else null
        coEvery { it.getFeedback(ADJUSTMENT_ID) } returns feedback
    }

    private fun TestScope.viewModel(adjustments: AdjustmentRepository) = FeedbackPromptViewModel(
        SavedStateHandle(
            mapOf(
                Screen.SessionSaved.ARG_DAY_NUMBER to day.dayNumber,
                Screen.SessionSaved.ARG_WEEK_NUMBER to 1
            )
        ),
        users,
        adjustments
    ).also { advanceUntilIdle() }

    @Test
    fun anActiveAdjustmentWithoutFeedbackIsTheOnlyPendingCase() = runTest {
        val viewModel = viewModel(adjustments())

        assertThat(viewModel.pendingAdjustmentId.value).isEqualTo(ADJUSTMENT_ID)
    }

    @Test
    fun anAnsweredAdjustmentIsNotAskedAgain() = runTest {
        val answered = AdjustmentFeedback(
            id = 1,
            adjustmentId = ADJUSTMENT_ID,
            answer = FeedbackAnswer.HELPED,
            answeredAt = 10L,
            dismissedAt = null
        )

        assertThat(viewModel(adjustments(feedback = answered)).pendingAdjustmentId.value).isNull()
    }

    @Test
    fun aDismissedOfferIsNotAskedAgain() = runTest {
        val dismissed = AdjustmentFeedback(
            id = 1,
            adjustmentId = ADJUSTMENT_ID,
            answer = null,
            answeredAt = null,
            dismissedAt = 10L
        )

        assertThat(viewModel(adjustments(feedback = dismissed)).pendingAdjustmentId.value).isNull()
    }

    @Test
    fun anUndoneAdjustmentIsNothingToAskAbout() = runTest {
        assertThat(viewModel(adjustments(active = false)).pendingAdjustmentId.value).isNull()
    }

    @Test
    fun anEarlierWeeksDayIsNotAskedAboutTheNewestWeeksAdjustment() = runTest {
        val adjustments = adjustments(active = false, nextWeek = true)

        assertThat(viewModel(adjustments).pendingAdjustmentId.value).isNull()
    }
}
