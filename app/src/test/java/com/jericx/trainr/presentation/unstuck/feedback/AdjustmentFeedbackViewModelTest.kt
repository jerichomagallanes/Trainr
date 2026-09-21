package com.jericx.trainr.presentation.unstuck.feedback

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.domain.unstuck.testCatalog
import com.jericx.trainr.domain.unstuck.testDay
import com.jericx.trainr.domain.unstuck.testUser
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.unstuck.planStartingToday
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class AdjustmentFeedbackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The adjusted session sits in week 1 while week 2 is the newest one.
    private val weeks = listOf(
        planStartingToday(),
        planStartingToday(testDay(id = 99L)).copy(id = 2, weekNumber = 2)
    )

    private fun users(goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN): UserRepository =
        mockk<UserRepository>(relaxed = true).also {
            coEvery { it.getCurrentUser() } returns testUser(goal = goal)
            every { it.getWeeklyWorkoutPlans(any()) } returns flowOf(weeks)
        }

    private fun adjustments(
        proposal: AdjustmentProposal = reduceProposal()
    ): AdjustmentRepository = mockk<AdjustmentRepository>(relaxed = true).also {
        coEvery { it.getAdjustmentById(ADJUSTMENT_ID) } returns appliedAdjustment(7L, proposal)
        coEvery { it.getFeedback(ADJUSTMENT_ID) } returns null
        coEvery { it.saveFeedback(any()) } returns 1L
    }

    private fun TestScope.viewModel(
        adjustments: AdjustmentRepository = adjustments(),
        users: UserRepository = users()
    ) = AdjustmentFeedbackViewModel(
        SavedStateHandle(mapOf(Screen.Feedback.ARG_ADJUSTMENT_ID to ADJUSTMENT_ID)),
        adjustments,
        users,
        testCatalog
    ).also { advanceUntilIdle() }

    @Test
    fun answeringSavesOnceAndEmitsOnce() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustments = repository)

        val seen = mutableListOf<FeedbackAnswer?>()
        val job = launch { viewModel.savedEvents.collect { seen += it } }
        viewModel.answer(FeedbackAnswer.HELPED)
        viewModel.answer(FeedbackAnswer.HELPED)
        advanceUntilIdle()
        job.cancel()

        val written = slot<AdjustmentFeedback>()
        coVerify(exactly = 1) { repository.saveFeedback(capture(written)) }
        assertThat(written.captured.adjustmentId).isEqualTo(ADJUSTMENT_ID)
        assertThat(written.captured.answer).isEqualTo(FeedbackAnswer.HELPED)
        assertThat(written.captured.answeredAt).isNotNull()
        assertThat(written.captured.dismissedAt).isNull()
        assertThat(seen).containsExactly(FeedbackAnswer.HELPED)
        assertThat(viewModel.uiState.value.answer).isEqualTo(FeedbackAnswer.HELPED)
    }

    @Test
    fun notNowRecordsADismissal() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustments = repository)

        val seen = mutableListOf<FeedbackAnswer?>()
        val job = launch { viewModel.savedEvents.collect { seen += it } }
        viewModel.dismiss()
        advanceUntilIdle()
        job.cancel()

        val written = slot<AdjustmentFeedback>()
        coVerify(exactly = 1) { repository.saveFeedback(capture(written)) }
        assertThat(written.captured.answer).isNull()
        assertThat(written.captured.answeredAt).isNull()
        assertThat(written.captured.dismissedAt).isNotNull()
        assertThat(seen).containsExactly(null)
    }

    @Test
    fun theEquipmentBodyNamesBothExercises() = runTest {
        val viewModel = viewModel(adjustments = adjustments(replaceProposal()))

        with(viewModel.uiState.value) {
            assertThat(isReplacement).isTrue()
            assertThat(originalName).isEqualTo(testCatalog["goblet_squat"]?.name)
            assertThat(substituteName).isEqualTo(testCatalog["dumbbell_step_up"]?.name)
            assertThat(guidanceKey).isEqualTo("dumbbell_step_up")
        }
    }

    @Test
    fun aTimeChangeNamesNoExercise() = runTest {
        with(viewModel().uiState.value) {
            assertThat(isReplacement).isFalse()
            assertThat(originalName).isEmpty()
            assertThat(substituteName).isEmpty()
            assertThat(guidanceKey).isEqualTo("dumbbell_bicep_curl")
        }
    }

    @Test
    fun anExerciseTheAdjustmentDroppedOffersNoGuidance() = runTest {
        val viewModel = viewModel(adjustments = adjustments(omitProposal()))
        viewModel.answer(FeedbackAnswer.EXERCISE_CONFUSING)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(guidanceKey).isNull()
            assertThat(offersGuidance).isFalse()
        }
    }

    // A note belongs to the session the adjustment was made on, which need not
    // be in the newest week.
    @Test
    fun theFollowUpCarriesTheWeekTheAdjustmentWasMadeIn() = runTest {
        with(viewModel().uiState.value) {
            assertThat(dayNumber).isEqualTo(1)
            assertThat(weekNumber).isEqualTo(1)
        }
    }

    @Test
    fun aStrengthGoalNamesStrengthEveryOtherGoalNamesTrainingPerformance() = runTest {
        assertThat(viewModel(users = users(FitnessGoal.STRENGTH)).uiState.value.trendLabelRes)
            .isEqualTo(R.string.trend_strength)

        FitnessGoal.entries.filterNot { it == FitnessGoal.STRENGTH }.forEach { goal ->
            assertThat(viewModel(users = users(goal)).uiState.value.trendLabelRes)
                .isEqualTo(R.string.trend_training_performance)
        }
    }
}
