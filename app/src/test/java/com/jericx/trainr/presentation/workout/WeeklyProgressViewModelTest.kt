package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.WeekStatus
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyProgressViewModelTest {

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

    private val weekStart = WorkoutWeek.mondayOf(1_755_000_000_000L)

    private fun day(number: Int, status: WorkoutStatus) = WorkoutDay(
        dayNumber = number,
        title = "Day $number",
        status = status,
        duration = 45,
        exerciseCount = 5,
        equipment = emptyList()
    )

    private fun plan(
        weekNumber: Int = 1,
        start: Long? = weekStart,
        vararg statuses: WorkoutStatus
    ) = WeeklyWorkoutPlan(
        id = weekNumber.toLong(),
        userId = 1,
        weekNumber = weekNumber,
        title = "Week $weekNumber",
        startDateMillis = start,
        workoutDays = statuses.mapIndexed { index, status -> day(index + 1, status) }
    )

    private fun duringTheWeek() = weekStart + TimeUnit.DAYS.toMillis(3)
    private fun afterTheWeek() = weekStart + TimeUnit.DAYS.toMillis(8)
    private fun beforeTheWeek() = weekStart - TimeUnit.DAYS.toMillis(2)

    @Test
    fun aFullyDoneWeekIsCompletedWheneverItIsLookedAt() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED)),
            nowMillis = duringTheWeek()
        )

        assertThat(progress.status).isEqualTo(WeekStatus.COMPLETED)
        assertThat(progress.completedDays).isEqualTo(2)
        assertThat(progress.completionPercentage).isEqualTo(100)
    }

    @Test
    fun anUnfinishedWeekStillInPlayIsInProgress() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)),
            nowMillis = duringTheWeek()
        )

        assertThat(progress.status).isEqualTo(WeekStatus.IN_PROGRESS)
    }

    @Test
    fun aPastWeekWithSomeWorkoutsDoneIsNotCompleted() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)),
            nowMillis = afterTheWeek()
        )

        assertThat(progress.status).isEqualTo(WeekStatus.NOT_COMPLETED)
    }

    @Test
    fun aPastWeekNeverTouchedWasSkipped() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.NOT_STARTED, WorkoutStatus.NOT_STARTED)),
            nowMillis = afterTheWeek()
        )

        assertThat(progress.status).isEqualTo(WeekStatus.SKIPPED)
    }

    // A week generated from the completion screen before its Monday arrives.
    @Test
    fun aWeekGeneratedAheadOfItsStartIsUpcoming() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.NOT_STARTED, WorkoutStatus.NOT_STARTED)),
            nowMillis = beforeTheWeek()
        )

        assertThat(progress.status).isEqualTo(WeekStatus.UPCOMING)
    }

    @Test
    fun theWeeksDatesComeFromItsOwnStart() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.COMPLETED)),
            nowMillis = duringTheWeek()
        )

        assertThat(progress.startDateMillis).isEqualTo(weekStart)
        assertThat(progress.endDateMillis).isEqualTo(WorkoutWeek.dateOfDay(weekStart, 7))
    }

    @Test
    fun storedWeeksAreListedInOrder() = runTest {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(
            listOf(
                plan(weekNumber = 2, statuses = arrayOf(WorkoutStatus.NOT_STARTED)),
                plan(weekNumber = 1, statuses = arrayOf(WorkoutStatus.COMPLETED))
            )
        )

        val viewModel = WeeklyProgressViewModel(userRepository)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(isSampleData).isFalse()
            assertThat(weeks.map { it.weekNumber }).containsExactly(1, 2).inOrder()
        }
    }

    @Test
    fun fallsBackToSampleDataWhenThereIsNoUser() = runTest {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = WeeklyProgressViewModel(userRepository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.weeks).isEqualTo(SampleWeeklyProgress.weeks)
        assertThat(viewModel.uiState.value.isSampleData).isTrue()
    }
    // A week that was trained is still the client's to drop; the app asks
    // first and names what goes, rather than refusing on their behalf.
    @Test
    fun aTrainedWeekCanBeDeleted() = runTest {
        val trained = plan(weekNumber = 1, statuses = arrayOf(WorkoutStatus.COMPLETED))
        val second = plan(weekNumber = 2, statuses = arrayOf(WorkoutStatus.NOT_STARTED))
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(trained, second))

        val viewModel = WeeklyProgressViewModel(userRepository)
        advanceUntilIdle()
        viewModel.deleteWeek(1)
        advanceUntilIdle()

        coVerify { userRepository.deleteWeeklyWorkoutPlan(trained.id) }
    }

    // Deleting from the middle would leave week two missing between one and
    // three. The numbers are a running order, not a record — the dates say when
    // each week was, and those do not move.
    @Test
    fun theWeeksAfterADeletedOneCloseTheGap() = runTest {
        val one = plan(weekNumber = 1, statuses = arrayOf(WorkoutStatus.COMPLETED))
        val two = plan(weekNumber = 2, statuses = arrayOf(WorkoutStatus.COMPLETED))
        val three = plan(weekNumber = 3, statuses = arrayOf(WorkoutStatus.NOT_STARTED))
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(one, two, three))

        val viewModel = WeeklyProgressViewModel(userRepository)
        advanceUntilIdle()
        viewModel.deleteWeek(2)
        advanceUntilIdle()

        coVerify { userRepository.deleteWeeklyWorkoutPlan(two.id) }
        coVerify { userRepository.updateWeeklyWorkoutPlan(match { it.id == three.id && it.weekNumber == 2 }) }
        coVerify(exactly = 0) { userRepository.updateWeeklyWorkoutPlan(match { it.id == one.id }) }
    }

    // Deleting down to nothing is allowed: the plan screen says there is none
    // and offers to build another, which beats keeping a week nobody wanted.
    @Test
    fun theLastWeekCanBeDeletedToo() = runTest {
        val only = plan(weekNumber = 1, statuses = arrayOf(WorkoutStatus.NOT_STARTED))
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(only))

        val viewModel = WeeklyProgressViewModel(userRepository)
        advanceUntilIdle()
        viewModel.deleteWeek(1)
        advanceUntilIdle()

        coVerify { userRepository.deleteWeeklyWorkoutPlan(only.id) }
    }

    // Found by hand: a week dated in the future but already trained in showed
    // as Upcoming, which also made it swipe-deletable along with its logs.
    @Test
    fun aFutureWeekAlreadyTrainedInHasStarted() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED)),
            nowMillis = beforeTheWeek()
        )

        assertThat(progress.status).isEqualTo(WeekStatus.IN_PROGRESS)
        assertThat(progress.hasTraining).isTrue()
    }

    @Test
    fun anUntouchedWeekHasNoTrainingToLose() {
        val progress = WeeklyProgressViewModel.weekProgressOf(
            plan(statuses = arrayOf(WorkoutStatus.NOT_STARTED, WorkoutStatus.NOT_STARTED)),
            nowMillis = beforeTheWeek()
        )

        assertThat(progress.hasTraining).isFalse()
    }

}
