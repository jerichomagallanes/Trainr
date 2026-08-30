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
import io.mockk.coVerify
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
import java.util.concurrent.TimeUnit

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
    private fun weekStarting(start: Long, vararg statuses: WorkoutStatus) = storedPlan.copy(
        startDateMillis = start,
        workoutDays = statuses.mapIndexed { index, status ->
            storedPlan.workoutDays.first().copy(id = index + 1L, dayNumber = index + 1, status = status)
        }
    )

    // Next week is offered once the week is done...
    @Test
    fun aFinishedWeekCanStartTheNextOne() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekStarting(start, WorkoutStatus.COMPLETED, WorkoutStatus.COMPLETED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(2)
        )

        assertThat(state.canStartNextWeek).isTrue()
    }

    // ...or once its dates have run out, so a missed day cannot strand the plan.
    @Test
    fun anExpiredWeekCanStartTheNextOneEvenUnfinished() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekStarting(start, WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(8)
        )

        assertThat(state.canStartNextWeek).isTrue()
    }

    @Test
    fun anUnfinishedWeekStillRunningCannotSkipAhead() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekStarting(start, WorkoutStatus.COMPLETED, WorkoutStatus.NOT_STARTED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(2)
        )

        assertThat(state.canStartNextWeek).isFalse()
    }

    // Sample data stands for nothing stored, so it can generate nothing.
    @Test
    fun theSampleWeekNeverOffersToStartTheNextOne() {
        val state = WeeklyPlanViewModel.stateFor(
            plan = SampleWorkoutData.weekOne,
            isSample = true,
            nowMillis = Long.MAX_VALUE / 2
        )

        assertThat(state.canStartNextWeek).isFalse()
    }

    private fun sessions(vararg statuses: WorkoutStatus) = listOf(1, 3, 5)
        .mapIndexed { index, slot ->
            storedPlan.workoutDays.first().copy(
                id = index + 1L,
                dayNumber = slot,
                title = "Session $index",
                status = statuses.getOrElse(index) { WorkoutStatus.NOT_STARTED }
            )
        }

    // Sessions swap weekday slots; the slots themselves stay where they are.
    @Test
    fun movingASessionLaterHandsItTheLaterWeekdaySlot() {
        val moved = WeeklyPlanViewModel.reorderedDays(sessions(), from = 0, to = 2)

        assertThat(moved.map { it.title })
            .containsExactly("Session 1", "Session 2", "Session 0").inOrder()
        assertThat(moved.map { it.dayNumber }).containsExactly(1, 3, 5).inOrder()
    }

    @Test
    fun movingASessionEarlierPushesTheOthersDown() {
        val moved = WeeklyPlanViewModel.reorderedDays(sessions(), from = 2, to = 0)

        assertThat(moved.map { it.title })
            .containsExactly("Session 2", "Session 0", "Session 1").inOrder()
        assertThat(moved.map { it.dayNumber }).containsExactly(1, 3, 5).inOrder()
    }

    // A finished session is the record of a date it was trained on.
    @Test
    fun aFinishedSessionCannotBeMoved() {
        val days = sessions(WorkoutStatus.COMPLETED)

        assertThat(WeeklyPlanViewModel.reorderedDays(days, from = 0, to = 2)).isEqualTo(days)
    }

    @Test
    fun nothingCanBeDraggedAcrossAFinishedSession() {
        val days = sessions(WorkoutStatus.NOT_STARTED, WorkoutStatus.COMPLETED)

        assertThat(WeeklyPlanViewModel.reorderedDays(days, from = 2, to = 0)).isEqualTo(days)
        assertThat(WeeklyPlanViewModel.reorderedDays(days, from = 0, to = 2)).isEqualTo(days)
    }

    @Test
    fun aMoveThatGoesNowhereChangesNothing() {
        val days = sessions()

        assertThat(WeeklyPlanViewModel.reorderedDays(days, from = 1, to = 1)).isEqualTo(days)
        assertThat(WeeklyPlanViewModel.reorderedDays(days, from = 0, to = 9)).isEqualTo(days)
    }

    @Test
    fun aMovedSessionIsPersistedOnItsNewWeekday() = runTest {
        val plan = storedPlan.copy(workoutDays = sessions())
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(plan))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.moveDay(from = 0, to = 1)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plan.workoutDays.map { it.title })
            .containsExactly("Session 1", "Session 0", "Session 2").inOrder()
        coVerify {
            userRepository.updateWorkoutDay(
                match { it.title == "Session 0" && it.dayNumber == 3 },
                plan.id
            )
        }
        coVerify {
            userRepository.updateWorkoutDay(
                match { it.title == "Session 1" && it.dayNumber == 1 },
                plan.id
            )
        }
    }

    private fun weekOfSessions(start: Long, vararg statuses: WorkoutStatus) = storedPlan.copy(
        startDateMillis = start,
        workoutDays = statuses.mapIndexed { index, status ->
            storedPlan.workoutDays.first().copy(
                id = index + 1L,
                dayNumber = index + 1,
                title = "Session $index",
                status = status
            )
        }
    )

    // Missed is derived from the calendar, never stored: an unfinished day whose
    // date has gone is missed, and the same day moved later stops being missed
    // with no flag to correct.
    @Test
    fun anUnfinishedDayThatHasPassedReadsAsMissed() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekOfSessions(start, WorkoutStatus.NOT_STARTED, WorkoutStatus.NOT_STARTED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(1)
        )

        assertThat(state.days[0].isMissed).isTrue()
        assertThat(state.days[1].isMissed).isFalse()
    }

    @Test
    fun aFinishedDayInThePastIsNotMissed() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekOfSessions(start, WorkoutStatus.COMPLETED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(3)
        )

        assertThat(state.days[0].isMissed).isFalse()
    }

    // The past holds its place: neither a missed nor a finished day can be
    // dragged, and nothing can be dropped onto a date that has gone.
    @Test
    fun everyDayThatHasPassedIsFrozen() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekOfSessions(start, WorkoutStatus.NOT_STARTED, WorkoutStatus.NOT_STARTED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(1)
        )

        assertThat(state.days[0].isFrozen).isTrue()
        assertThat(state.days[1].isFrozen).isFalse()
    }

    // The button must not open last Monday while calling it today's workout.
    @Test
    fun theStartButtonSkipsDaysThatHavePassed() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekOfSessions(start, WorkoutStatus.NOT_STARTED, WorkoutStatus.NOT_STARTED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(1)
        )

        assertThat(state.nextWorkout?.day?.title).isEqualTo("Session 1")
        assertThat(state.nextWorkoutIsToday).isTrue()
    }

    @Test
    fun theStartButtonNamesTheNextDayWhenThereIsNoneToday() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            // Sessions on Monday and Wednesday, looked at on Tuesday.
            plan = storedPlan.copy(
                startDateMillis = start,
                workoutDays = listOf(1, 3).mapIndexed { index, slot ->
                    storedPlan.workoutDays.first().copy(
                        id = index + 1L,
                        dayNumber = slot,
                        title = "Session $index",
                        status = WorkoutStatus.NOT_STARTED
                    )
                }
            ),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(1)
        )

        assertThat(state.nextWorkout?.day?.title).isEqualTo("Session 1")
        assertThat(state.nextWorkoutIsToday).isFalse()
    }

    // Everything behind you and nothing ahead: the button still has a target
    // rather than doing nothing.
    @Test
    fun theStartButtonFallsBackToAMissedDayWhenNothingIsLeft() {
        val start = WorkoutWeek.mondayOf(1_755_000_000_000L)
        val state = WeeklyPlanViewModel.stateFor(
            plan = weekOfSessions(start, WorkoutStatus.NOT_STARTED),
            isSample = false,
            nowMillis = start + TimeUnit.DAYS.toMillis(5)
        )

        assertThat(state.nextWorkout?.day?.title).isEqualTo("Session 0")
        assertThat(state.nextWorkoutIsToday).isFalse()
    }

    // The built-in week stands in for a moment while the stored plan loads. It
    // is dated in the past, so read against the clock it would greet a new user
    // with a screen of missed workouts.
    @Test
    fun thePlaceholderWeekNeverLooksMissed() = runTest {
        coEvery { userRepository.getCurrentUser() } returns null

        val viewModel = viewModel()
        assertThat(viewModel.uiState.value.days.any { it.isMissed }).isFalse()

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.days.any { it.isMissed }).isFalse()
    }

}
