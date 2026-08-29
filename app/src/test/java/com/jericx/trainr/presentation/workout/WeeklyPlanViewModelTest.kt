package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyPlanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val storedPlan = WeeklyWorkoutPlan(
        id = 9,
        userId = 1,
        weekNumber = 1,
        title = "Stored week",
        workoutDays = listOf(
            WorkoutDay(
                id = 1,
                dayNumber = 2,
                title = "Stored workout",
                status = WorkoutStatus.NOT_STARTED,
                duration = 30,
                exerciseCount = 3,
                equipment = emptyList()
            )
        )
    )

    @Test
    fun fallsBackToSampleDataWhenThereIsNoUser() = runTest {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = WeeklyPlanViewModel(userRepository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan).isEqualTo(SampleWorkoutData.weekOne)
        assertThat(viewModel.uiState.value.isSampleData).isTrue()
    }

    @Test
    fun fallsBackToSampleDataWhenTheUserHasNoPlanYet() = runTest {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        coEvery { userRepository.getWeeklyWorkoutPlan(1, 1) } returns null

        val viewModel = WeeklyPlanViewModel(userRepository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSampleData).isTrue()
    }

    @Test
    fun usesTheStoredPlanWhenOneExists() = runTest {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        coEvery { userRepository.getWeeklyWorkoutPlan(1, 1) } returns storedPlan

        val viewModel = WeeklyPlanViewModel(userRepository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan).isEqualTo(storedPlan)
        assertThat(viewModel.uiState.value.isSampleData).isFalse()
    }

    @Test
    fun givesEveryWorkoutDayADate() = runTest {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = WeeklyPlanViewModel(userRepository)
        advanceUntilIdle()

        val days = viewModel.uiState.value.days
        assertThat(days).hasSize(SampleWorkoutData.weekOne.workoutDays.size)
        assertThat(days.map { it.dateMillis }.toSet()).hasSize(days.size)
    }

    @Test
    fun exposesSampleDataBeforeTheRepositoryAnswers() {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = WeeklyPlanViewModel(userRepository)

        assertThat(viewModel.uiState.value.days).isNotEmpty()
    }

    @Test
    fun todaysWorkoutIsTheFirstDayStillOutstanding() {
        val state = WeeklyPlanViewModel.stateFor(SampleWorkoutData.weekOne, isSample = true)

        assertThat(state.todaysDay?.title).isEqualTo("Cardio & Core")
    }

    @Test
    fun aFinishedWeekFallsBackToItsFirstDay() {
        val finished = SampleWorkoutData.weekOne.let { plan ->
            plan.copy(workoutDays = plan.workoutDays.map { it.copy(status = WorkoutStatus.COMPLETED) })
        }

        val state = WeeklyPlanViewModel.stateFor(finished, isSample = true)

        assertThat(state.todaysDay?.title).isEqualTo("Full Body Strength")
    }
}
