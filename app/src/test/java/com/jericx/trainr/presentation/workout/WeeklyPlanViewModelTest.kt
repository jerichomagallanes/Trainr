package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import com.jericx.trainr.presentation.Screen
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
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

    // Home passes no week; Weekly Progress passes the one that was tapped.
    private fun viewModel(weekNumber: Int? = null) = WeeklyPlanViewModel(
        SavedStateHandle(
            weekNumber?.let { mapOf(Screen.WeekPlan.ARG_WEEK_NUMBER to it) } ?: emptyMap()
        ),
        userRepository
    )

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

        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan).isEqualTo(SampleWorkoutData.weekOne)
        assertThat(viewModel.uiState.value.isSampleData).isTrue()
    }

    @Test
    fun fallsBackToSampleDataWhenTheUserHasNoPlanYet() = runTest {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(emptyList())

        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSampleData).isTrue()
    }

    @Test
    fun usesTheStoredPlanWhenOneExists() = runTest {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(storedPlan))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan).isEqualTo(storedPlan)
        assertThat(viewModel.uiState.value.isSampleData).isFalse()
    }

    // A week opened from Weekly Progress shows that week, not the newest.
    @Test
    fun showsTheWeekThatWasAskedFor() = runTest {
        val weekTwo = storedPlan.copy(id = 10, weekNumber = 2, title = "Second week")
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(storedPlan, weekTwo))

        val viewModel = viewModel(weekNumber = 1)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan).isEqualTo(storedPlan)
    }

    // After a regeneration there are several stored weeks; home shows the
    // newest one.
    @Test
    fun showsTheLatestWeekWhenSeveralAreStored() = runTest {
        val weekTwo = storedPlan.copy(id = 10, weekNumber = 2, title = "Second week")
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(weekTwo, storedPlan))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan).isEqualTo(weekTwo)
    }

    // The known gap this closes: a stored plan used to render the sample
    // week's hardcoded July dates whatever week it actually was.
    @Test
    fun aStoredPlanRendersItsOwnDates() = runTest {
        val start = 1_755_000_000_000L
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(storedPlan.copy(startDateMillis = start)))

        val viewModel = viewModel()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(weekStartMillis).isEqualTo(start)
            assertThat(days.single().dateMillis)
                .isEqualTo(WorkoutWeek.dateOfDay(start, storedPlan.workoutDays.single().dayNumber))
            assertThat(weekEndMillis).isEqualTo(WorkoutWeek.dateOfDay(start, 7))
        }
    }

    @Test
    fun givesEveryWorkoutDayADate() = runTest {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = viewModel()
        advanceUntilIdle()

        val days = viewModel.uiState.value.days
        assertThat(days).hasSize(SampleWorkoutData.weekOne.workoutDays.size)
        assertThat(days.map { it.dateMillis }.toSet()).hasSize(days.size)
    }

    @Test
    fun exposesSampleDataBeforeTheRepositoryAnswers() {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = viewModel()

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
