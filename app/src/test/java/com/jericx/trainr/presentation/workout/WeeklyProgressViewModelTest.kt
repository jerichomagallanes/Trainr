package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.WeekStatus
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.presentation.workout.util.mondayOf
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

    private val weekStart = mondayOf(1_755_000_000_000L)

    private fun day(
        number: Int,
        status: WorkoutStatus,
        performed: Boolean = status == WorkoutStatus.COMPLETED
    ) = WorkoutDay(
        dayNumber = number,
        title = "Day $number",
        status = status,
        duration = 45,
        exerciseCount = 5,
        equipment = emptyList(),
        exercises = listOf(WorkoutExercise(name = "Movement", isCompleted = performed))
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

    // Finishing early closes the day whatever was logged, and a day nobody
    // trained is not a day of training.
    @Test
    fun aDayClosedWithNothingPerformedIsNotACompletedDay() {
        val plan = plan(statuses = arrayOf(WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED))
        val closedEarly = plan.copy(
            workoutDays = listOf(
                day(1, WorkoutStatus.COMPLETED, performed = false),
                plan.workoutDays[1]
            )
        )

        val progress = WeeklyProgressViewModel.weekProgressOf(
            closedEarly,
            nowMillis = duringTheWeek()
        )

        assertThat(progress.completedDays).isEqualTo(1)
        assertThat(progress.completionPercentage).isEqualTo(50)
        assertThat(progress.status).isEqualTo(WeekStatus.IN_PROGRESS)
    }

    // Sets ticked without finishing their exercise are still training, and the
    // week's percentage has to say so.
    @Test
    fun aDayClosedEarlyWithSetsLoggedIsACompletedDay() {
        val plan = plan(statuses = arrayOf(WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED))
        val partlyTrained = plan.copy(
            workoutDays = listOf(
                day(1, WorkoutStatus.COMPLETED, performed = false).let { day ->
                    day.copy(
                        exercises = day.exercises.map {
                            it.copy(
                                sets = listOf(
                                    ExerciseSet(setNumber = 1, isCompleted = true),
                                    ExerciseSet(setNumber = 2, isCompleted = false)
                                )
                            )
                        }
                    )
                },
                plan.workoutDays[1]
            )
        )

        val progress = WeeklyProgressViewModel.weekProgressOf(
            partlyTrained,
            nowMillis = duringTheWeek()
        )

        assertThat(progress.completedDays).isEqualTo(1)
        assertThat(progress.hasTraining).isTrue()
    }

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

        assertThat(viewModel.uiState.value.weeks.map { it.weekNumber })
            .containsExactly(1, 2).inOrder()
    }

    @Test
    fun withNothingStoredThereAreNoWeeksToShow() = runTest {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = WeeklyProgressViewModel(userRepository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.weeks).isEmpty()
    }
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

    // Week numbers are a running order, not a record: the dates say when each week was and do not move
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
