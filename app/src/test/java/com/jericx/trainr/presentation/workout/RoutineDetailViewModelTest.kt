package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.R
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.ActualOrigin
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.ReasonCode
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.domain.unstuck.TradeoffCode
import com.jericx.trainr.domain.unstuck.UndoResult
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.derivedExerciseCount
import com.jericx.trainr.presentation.workout.model.remainingMinutes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun emptyRepository(): UserRepository = mockk<UserRepository>(relaxed = true)
        .also { coEvery { it.getCurrentUser() } returns null }

    private fun emptyAdjustments(): AdjustmentRepository = mockk<AdjustmentRepository>(relaxed = true)
        .also { coEvery { it.getOutcome(any()) } returns null }

    private fun viewModel(
        dayNumber: Int = SampleWorkoutData.DEFAULT_DAY_NUMBER,
        repository: UserRepository = emptyRepository(),
        weekNumber: Int = Screen.RoutineDetail.LATEST_WEEK,
        adjustments: AdjustmentRepository = emptyAdjustments()
    ) = RoutineDetailViewModel(
        SavedStateHandle(
            mapOf(
                Screen.RoutineDetail.ARG_DAY_NUMBER to dayNumber,
                Screen.RoutineDetail.ARG_WEEK_NUMBER to weekNumber
            )
        ),
        repository,
        adjustments,
        SampleWorkoutData.catalog
    )

    // The routine screen holds nothing until the stored week has been read, so tests must let that happen first
    private fun TestScope.loadedViewModel(
        dayNumber: Int = SampleWorkoutData.DEFAULT_DAY_NUMBER,
        repository: UserRepository = repositoryWith(SampleWorkoutData.weekOne),
        weekNumber: Int = Screen.RoutineDetail.LATEST_WEEK
    ) = viewModel(dayNumber, repository, weekNumber).also { advanceUntilIdle() }

    private fun RoutineDetailViewModel.exercise(position: Int): ExerciseUi =
        uiState.value.routine.exercises.first { it.position == position }

    private fun RoutineDetailViewModel.isCompleted(position: Int) = exercise(position).isCompleted

    // Cardio & Core is the week's second workout day even though its dayNumber is 3 (Wednesday)
    @Test
    fun theRoutineKnowsWhichWorkoutDayOfTheWeekItIs() = runTest {
        assertThat(loadedViewModel().uiState.value.dayNumber).isEqualTo(2)
    }

    private fun day(number: Int, status: WorkoutStatus) = WorkoutDay(
        dayNumber = number,
        title = "Day $number",
        status = status,
        duration = 30,
        exerciseCount = 4,
        equipment = emptyList()
    )

    @Test
    fun finishingTheLastOutstandingDayEndsTheWeek() {
        val days = listOf(
            day(1, WorkoutStatus.COMPLETED),
            day(3, WorkoutStatus.IN_PROGRESS),
            day(5, WorkoutStatus.COMPLETED)
        )

        assertThat(RoutineDetailViewModel.completesTheWeek(days, dayNumber = 2)).isTrue()
    }

    @Test
    fun aDayStillToDoLeavesTheWeekOpen() {
        val days = listOf(
            day(1, WorkoutStatus.COMPLETED),
            day(3, WorkoutStatus.IN_PROGRESS),
            day(5, WorkoutStatus.NOT_STARTED)
        )

        assertThat(RoutineDetailViewModel.completesTheWeek(days, dayNumber = 2)).isFalse()
    }

    @Test
    fun aDayInProgressAlsoLeavesTheWeekOpen() {
        val days = listOf(
            day(1, WorkoutStatus.IN_PROGRESS),
            day(3, WorkoutStatus.IN_PROGRESS)
        )

        assertThat(RoutineDetailViewModel.completesTheWeek(days, dayNumber = 2)).isFalse()
    }

    @Test
    fun aOneDayWeekEndsWithItsOnlyDay() {
        val days = listOf(day(1, WorkoutStatus.IN_PROGRESS))

        assertThat(RoutineDetailViewModel.completesTheWeek(days, dayNumber = 1)).isTrue()
    }

    // Friday is still untouched in the stored week, so Wednesday ends the day.
    @Test
    fun theSampleRoutineEndsTheDayRatherThanTheWeek() = runTest {
        assertThat(loadedViewModel().uiState.value.completesTheWeek).isFalse()
    }

    @Test
    fun aRoutineIsNotCompleteUntilEveryExerciseIs() = runTest {
        val viewModel = loadedViewModel()

        assertThat(viewModel.uiState.value.routine.isComplete).isFalse()

        viewModel.completeRoutine()

        assertThat(viewModel.uiState.value.routine.isComplete).isTrue()
    }

    @Test
    fun startingATimerCountsDownFromTheExerciseDuration() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(600)

        advanceTimeBy(3_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(597)
    }

    @Test
    fun pausingHoldsTheCountdownWhereItIs() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.pauseTimer()
        advanceTimeBy(10_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(597)
        assertThat(viewModel.uiState.value.timer?.isRunning).isFalse()
    }

    @Test
    fun resumingCarriesOnFromWhereItPaused() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.pauseTimer()
        viewModel.resumeTimer()
        advanceTimeBy(2_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(595)
        assertThat(viewModel.uiState.value.timer?.isRunning).isTrue()
    }

    @Test
    fun resettingReturnsToTheFullIntervalAndHoldsItThere() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(30_000)
        runCurrent()
        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(570)

        viewModel.resetTimer()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(600)
        assertThat(viewModel.uiState.value.timer?.isRunning).isFalse()
    }

    @Test
    fun aResetTimerDoesNotTickUntilItIsResumed() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(30_000)
        runCurrent()
        viewModel.resetTimer()
        advanceTimeBy(10_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(600)

        viewModel.resumeTimer()
        advanceTimeBy(3_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(597)
    }

    @Test
    fun resettingWithNoTimerRunningChangesNothing() = runTest {
        val viewModel = loadedViewModel()

        viewModel.resetTimer()

        assertThat(viewModel.uiState.value.timer).isNull()
    }

    @Test
    fun stoppingClearsTheTimerWithoutCompletingTheExercise() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.stopTimer()

        assertThat(viewModel.uiState.value.timer).isNull()
        assertThat(viewModel.isCompleted(2)).isFalse()
    }

    @Test
    fun theCountdownReachingZeroCompletesTheExercise() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(4))
        advanceTimeBy(4 * 60 * 1_000L)
        runCurrent()

        assertThat(viewModel.uiState.value.timer).isNull()
        assertThat(viewModel.isCompleted(4)).isTrue()
    }

    @Test
    fun theCountdownKeepsRunningRightUpToTheLastSecond() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(4))
        advanceTimeBy(4 * 60 * 1_000L - 1_000L)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(1)
        assertThat(viewModel.isCompleted(4)).isFalse()
    }

    @Test
    fun startingAnotherExerciseReplacesTheRunningTimer() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.startTimer(viewModel.exercise(3))
        advanceTimeBy(1_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.position).isEqualTo(3)
        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(299)
    }

    @Test
    fun tickingTheExerciseOffClearsItsTimer() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.toggleExercise(2)
        advanceTimeBy(5_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer).isNull()
        assertThat(viewModel.isCompleted(2)).isTrue()
    }

    @Test
    fun untickingAnExerciseLeavesAnotherExercisesTimerAlone() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.toggleExercise(1)

        assertThat(viewModel.uiState.value.timer?.position).isEqualTo(2)
    }

    @Test
    fun tutorialsStartClosedAndToggleOpenAndShut() = runTest {
        val viewModel = loadedViewModel()

        assertThat(viewModel.uiState.value.expandedVideo).isNull()

        viewModel.toggleVideo(2)
        assertThat(viewModel.uiState.value.expandedVideo).isEqualTo(2)

        viewModel.toggleVideo(2)
        assertThat(viewModel.uiState.value.expandedVideo).isNull()
    }

    // The player is a WebView living as long as the section is open, so two open at once means two of them
    @Test
    fun openingATutorialClosesWhicheverWasOpen() = runTest {
        val viewModel = loadedViewModel()

        viewModel.toggleVideo(2)
        viewModel.toggleVideo(3)

        assertThat(viewModel.uiState.value.expandedVideo).isEqualTo(3)
    }

    @Test
    fun completingTheWholeRoutineClearsTheTimer() = runTest {
        val viewModel = loadedViewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.completeRoutine()
        advanceTimeBy(5_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer).isNull()
        assertThat(viewModel.uiState.value.routine.isComplete).isTrue()
    }

    private fun storedExercise(id: Long, key: String, name: String, done: Boolean = false) =
        WorkoutExercise(
            id = id,
            exerciseKey = key,
            name = name,
            measure = ExerciseMeasure.WEIGHT_AND_REPS,
            sets = listOf(
                ExerciseSet(id = id * 10, setNumber = 1, targetReps = 12, targetWeightKg = 20f),
                ExerciseSet(id = id * 10 + 1, setNumber = 2, targetReps = 12, targetWeightKg = 20f)
            ),
            durationMinutes = 8,
            isCompleted = done
        )

    private val storedPlan = WeeklyWorkoutPlan(
        id = 7,
        userId = 1,
        weekNumber = 1,
        title = "Stored week",
        startDateMillis = 1_000_000_000_000L,
        workoutDays = listOf(
            WorkoutDay(
                id = 21,
                dayNumber = 1,
                title = "Stored Strength",
                status = WorkoutStatus.COMPLETED,
                duration = 8,
                exerciseCount = 1,
                equipment = listOf("Dumbbells"),
                exercises = listOf(storedExercise(30, "plank", "Plank", done = true)),
                completedAt = 1L
            ),
            WorkoutDay(
                id = 22,
                dayNumber = 3,
                title = "Stored Pull",
                status = WorkoutStatus.NOT_STARTED,
                duration = 16,
                exerciseCount = 2,
                equipment = listOf("Dumbbells"),
                exercises = listOf(
                    storedExercise(32, "bent_over_row", "Bent-Over Rows"),
                    storedExercise(33, "goblet_squat", "Goblet Squats")
                )
            )
        )
    )

    private fun repositoryWith(plan: WeeklyWorkoutPlan): UserRepository =
        mockk<UserRepository>(relaxed = true).also {
            coEvery { it.getCurrentUser() } returns UserProfile(id = 1)
            every { it.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(plan))
        }

    // The weekday exists in every week, so only the requested week number tells the routines apart
    @Test
    fun loadsTheDayOfTheRequestedWeek() = runTest {
        val laterWeek = storedPlan.copy(
            id = 8,
            weekNumber = 2,
            title = "Later week",
            workoutDays = storedPlan.workoutDays.map {
                it.copy(id = it.id + 100, title = it.title + " Again")
            }
        )
        val repository = mockk<UserRepository>(relaxed = true).also {
            coEvery { it.getCurrentUser() } returns UserProfile(id = 1)
            every { it.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(storedPlan, laterWeek))
        }

        val viewModel = viewModel(dayNumber = 3, repository = repository, weekNumber = 1)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(routine.title).isEqualTo("Stored Pull")
            assertThat(weekNumber).isEqualTo(1)
        }
    }

    @Test
    fun loadsTheNewestWeekWhenNoneWasRequested() = runTest {
        val laterWeek = storedPlan.copy(
            id = 8,
            weekNumber = 2,
            title = "Later week",
            workoutDays = storedPlan.workoutDays.map {
                it.copy(id = it.id + 100, title = it.title + " Again")
            }
        )
        val repository = mockk<UserRepository>(relaxed = true).also {
            coEvery { it.getCurrentUser() } returns UserProfile(id = 1)
            every { it.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(storedPlan, laterWeek))
        }

        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(routine.title).isEqualTo("Stored Pull Again")
            assertThat(weekNumber).isEqualTo(2)
        }
    }

    @Test
    fun loadsTheStoredDayWhenOneExists() = runTest {
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan))
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(routine.title).isEqualTo("Stored Pull")
            assertThat(isLoaded).isTrue()
            assertThat(dayNumber).isEqualTo(2)
            assertThat(equipment).containsExactly("Dumbbells")
        }
    }

    // The completion guard has to tell the routine's arrival from the client finishing the day
    @Test
    fun theStateIsNotLoadedUntilTheRepositoryAnswers() = runTest {
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan))

        assertThat(viewModel.uiState.value.isLoaded).isFalse()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isLoaded).isTrue()
    }

    @Test
    fun togglingAnExercisePersistsItAndMarksTheDayInProgress() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.toggleExercise(1)
        advanceUntilIdle()

        coVerify {
            repository.updateWorkoutExercise(match { it.id == 32L && it.isCompleted }, 22L)
            repository.updateWorkoutDay(
                match { it.status == WorkoutStatus.IN_PROGRESS && it.completedAt == null },
                7L
            )
        }
    }

    @Test
    fun finishingTheRoutinePersistsTheDayAsCompleted() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.completeRoutine()
        advanceUntilIdle()

        coVerify {
            repository.updateWorkoutExercise(match { it.id == 32L && it.isCompleted }, 22L)
            repository.updateWorkoutExercise(match { it.id == 33L && it.isCompleted }, 22L)
            repository.updateWorkoutDay(
                match { it.status == WorkoutStatus.COMPLETED && it.completedAt != null },
                7L
            )
        }
    }

    @Test
    fun editingASetPersistsIt() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        val logged = viewModel.exercise(1).sets.first().copy(actualReps = 9, isCompleted = true)
        viewModel.updateSet(1, logged)
        advanceUntilIdle()

        coVerify {
            repository.updateExerciseSet(match { it.id == 320L && it.actualReps == 9 }, 32L)
        }
    }

    @Test
    fun aTypedNumberIsStoredAsTyped() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.updateSet(1, viewModel.exercise(1).sets.first().copy(actualReps = 9))
        advanceUntilIdle()

        coVerify {
            repository.updateExerciseSet(
                match { it.id == 320L && it.actualReps == 9 && it.actualOrigin == ActualOrigin.TYPED },
                32L
            )
        }
    }

    @Test
    fun deletingASetRemovesItsRowAndRenumbersTheRest() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.deleteSet(1, viewModel.exercise(1).sets.first().setNumber)
        advanceUntilIdle()

        assertThat(viewModel.exercise(1).sets.map { it.setNumber }).containsExactly(1)
        coVerify { repository.deleteExerciseSet(320L) }
        coVerify {
            repository.updateExerciseSet(match { it.id == 321L && it.setNumber == 1 }, 32L)
        }
    }

    // Deleting down to no sets at all is allowed, because Add set brings one back
    @Test
    fun everySetOfAnExerciseCanBeDeleted() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        repeat(2) {
            viewModel.exercise(1).sets.firstOrNull()?.let { set ->
                viewModel.deleteSet(1, set.setNumber)
            }
        }
        advanceUntilIdle()

        assertThat(viewModel.exercise(1).sets).isEmpty()
        coVerify(exactly = 2) { repository.deleteExerciseSet(any()) }
    }

    @Test
    fun anAddedSetIsPersistedAndKeepsItsStorageId() = runTest {
        val repository = repositoryWith(storedPlan)
        coEvery { repository.addExerciseSet(any(), 32L) } returns 99L
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.addSet(1)
        advanceUntilIdle()

        val added = viewModel.exercise(1).sets.last()
        assertThat(added.setNumber).isEqualTo(3)
        assertThat(added.id).isEqualTo(99L)
        coVerify { repository.addExerciseSet(match { it.setNumber == 3 }, 32L) }
    }

    @Test
    fun theRoutineCarriesEachExercisesPreviousPerformance() = runTest {
        val repository = repositoryWith(storedPlan)
        val history = listOf(
            ExerciseSet(id = 900, setNumber = 1, actualReps = 11, actualWeightKg = 18f, isCompleted = true)
        )
        coEvery {
            repository.getPreviousSets(1, "bent_over_row", 22, Long.MAX_VALUE)
        } returns history

        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        assertThat(viewModel.exercise(1).previousSets).isEqualTo(history)
        assertThat(viewModel.exercise(2).previousSets).isEmpty()
    }

    // A finished day shows the history it had at its own completion, not performances logged since
    @Test
    fun aFinishedDaysHistoryStopsAtItsOwnCompletion() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 1, repository = repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.routine.title).isEqualTo("Stored Strength")
        coVerify { repository.getPreviousSets(1, "plank", 21, 1L) }
    }

    @Test
    fun nothingStoredMeansNothingWritten() = runTest {
        val repository = emptyRepository()
        val viewModel = viewModel(repository = repository)
        advanceUntilIdle()

        viewModel.toggleExercise(3)
        viewModel.updateSet(3, ExerciseSet(setNumber = 1, targetReps = 12, actualReps = 20))
        viewModel.addSet(3)
        viewModel.deleteSet(3, 1)
        viewModel.completeRoutine()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateWorkoutExercise(any(), any()) }
        coVerify(exactly = 0) { repository.updateWorkoutDay(any(), any()) }
        coVerify(exactly = 0) { repository.updateExerciseSet(any(), any()) }
        coVerify(exactly = 0) { repository.addExerciseSet(any(), any()) }
        coVerify(exactly = 0) { repository.deleteExerciseSet(any()) }
    }
    // The day is stored the way it is read back: by the PREVIOUS column and by next week's prompt
    @Test
    fun completingTheRoutineStoresThePrescribedNumbers() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.completeRoutine()
        advanceUntilIdle()

        coVerify {
            repository.updateExerciseSet(
                match { it.id == 320L && it.actualReps == it.targetReps && it.isCompleted },
                32L
            )
        }
    }

    @Test
    fun tickingASingleExerciseStoresItsPrescribedNumbers() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.toggleExercise(1)
        advanceUntilIdle()

        coVerify {
            repository.updateExerciseSet(match { it.actualReps == it.targetReps }, 32L)
        }
    }

    private fun partialOutcome(dayId: Long = 22L) = SessionOutcome(
        id = 5,
        workoutDayId = dayId,
        finishKind = FinishKind.PARTIAL,
        finishedAt = 2L,
        performedSetCount = 1,
        plannedSetCount = 4
    )

    @Test
    fun finishingEarlyMarksTheDayCompleteWithoutFillingASet() = runTest {
        val repository = repositoryWith(storedPlan)
        val adjustments = emptyAdjustments()
        val viewModel = viewModel(dayNumber = 3, repository = repository, adjustments = adjustments)
        advanceUntilIdle()

        viewModel.updateSet(1, viewModel.exercise(1).sets.first().copy(actualReps = 9, isCompleted = true))
        advanceUntilIdle()
        viewModel.askToFinishEarly()
        viewModel.finishEarly()
        advanceUntilIdle()

        coVerify {
            repository.updateWorkoutDay(
                match { it.id == 22L && it.status == WorkoutStatus.COMPLETED && it.completedAt != null },
                7L
            )
            adjustments.saveOutcome(
                match {
                    it.workoutDayId == 22L && it.finishKind == FinishKind.PARTIAL &&
                        it.performedSetCount == 1 && it.plannedSetCount == 4
                }
            )
        }
        coVerify(exactly = 1) { repository.updateExerciseSet(any(), any()) }
        coVerify(exactly = 0) { repository.updateWorkoutExercise(any(), any()) }
        with(viewModel.uiState.value) {
            assertThat(outcome?.finishKind).isEqualTo(FinishKind.PARTIAL)
            assertThat(isConfirmingFinishEarly).isFalse()
            assertThat(routine.exercises.count { it.isCompleted }).isEqualTo(0)
        }
    }

    @Test
    fun finishingEarlyReportsAFailedSaveAndRetries() = runTest {
        val repository = repositoryWith(storedPlan)
        val adjustments = emptyAdjustments()
        coEvery { adjustments.saveOutcome(any()) } throws IllegalStateException("disk full") andThen 1L
        val viewModel = viewModel(dayNumber = 3, repository = repository, adjustments = adjustments)
        advanceUntilIdle()

        viewModel.askToFinishEarly()
        viewModel.finishEarly()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(saveFailed).isTrue()
            assertThat(outcome).isNull()
            assertThat(isConfirmingFinishEarly).isTrue()
        }
        coVerify {
            repository.updateWorkoutDay(match { it.id == 22L && it.status == WorkoutStatus.COMPLETED }, 7L)
            repository.updateWorkoutDay(match { it.id == 22L && it.status == WorkoutStatus.NOT_STARTED }, 7L)
        }

        viewModel.retryFinishEarly()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(saveFailed).isFalse()
            assertThat(outcome?.finishKind).isEqualTo(FinishKind.PARTIAL)
            assertThat(isConfirmingFinishEarly).isFalse()
        }
        coVerify(exactly = 2) { adjustments.saveOutcome(any()) }
    }

    @Test
    fun aSecondTapWhileSavingIsIgnored() = runTest {
        val adjustments = emptyAdjustments()
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan), adjustments = adjustments)
        advanceUntilIdle()
        val events = mutableListOf<SessionSavedEvent>()
        val collecting = launch { viewModel.savedEvents.toList(events) }

        viewModel.finishEarly()
        viewModel.finishEarly()
        advanceUntilIdle()

        assertThat(events).hasSize(1)
        coVerify(exactly = 1) { adjustments.saveOutcome(any()) }
        collecting.cancel()
    }

    @Test
    fun slidingToCompleteRecordsAFullOutcome() = runTest {
        val adjustments = emptyAdjustments()
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan), adjustments = adjustments)
        advanceUntilIdle()

        viewModel.completeRoutine()
        advanceUntilIdle()

        coVerify {
            adjustments.saveOutcome(
                match {
                    it.workoutDayId == 22L && it.finishKind == FinishKind.FULL &&
                        it.performedSetCount == 4 && it.plannedSetCount == 4
                }
            )
        }
        assertThat(viewModel.uiState.value.outcome?.finishKind).isEqualTo(FinishKind.FULL)
    }

    @Test
    fun aFinishedEarlyDayStaysCompleteWhenASetIsEdited() = runTest {
        val repository = repositoryWith(storedPlan)
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.finishEarly()
        advanceUntilIdle()
        viewModel.updateSet(1, viewModel.exercise(1).sets.first().copy(actualReps = 9, isCompleted = true))
        viewModel.toggleExercise(2)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            repository.updateWorkoutDay(match { it.status != WorkoutStatus.COMPLETED }, any())
        }
        coVerify { repository.updateExerciseSet(match { it.id == 320L && it.actualReps == 9 }, 32L) }
    }

    @Test
    fun aStoredOutcomeIsLoadedIntoState() = runTest {
        val adjustments = emptyAdjustments()
        coEvery { adjustments.getOutcome(22L) } returns partialOutcome()

        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan), adjustments = adjustments)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.outcome).isEqualTo(partialOutcome())
    }

    // The event is what navigates, and only a save sends one: a screen rebuilt
    // around a stored outcome must not be sent to the saved screen again.
    @Test
    fun theSavedEventFiresOnce() = runTest {
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan))
        advanceUntilIdle()
        val events = mutableListOf<SessionSavedEvent>()
        val collecting = launch { viewModel.savedEvents.toList(events) }

        viewModel.finishEarly()
        advanceUntilIdle()

        assertThat(events).containsExactly(
            SessionSavedEvent(
                dayNumber = 2,
                weekNumber = 1,
                performedExercises = 0,
                plannedExercises = 2
            )
        )

        val adjustments = emptyAdjustments()
        coEvery { adjustments.getOutcome(22L) } returns partialOutcome()
        val restored = viewModel(dayNumber = 3, repository = repositoryWith(storedPlan), adjustments = adjustments)
        val restoredEvents = mutableListOf<SessionSavedEvent>()
        val collectingRestored = launch { restored.savedEvents.toList(restoredEvents) }
        advanceUntilIdle()

        assertThat(restoredEvents).isEmpty()
        assertThat(restored.uiState.value.outcome?.finishKind).isEqualTo(FinishKind.PARTIAL)
        collecting.cancel()
        collectingRestored.cancel()
    }

    private fun WorkoutExercise.omitting(from: Int) = copy(
        sets = sets.map { if (it.setNumber >= from) it.copy(omittedBy = 5L) else it }
    )

    private fun adjustedPlan() = storedPlan.copy(
        workoutDays = storedPlan.workoutDays.map { day ->
            if (day.id != 22L) {
                day
            } else {
                day.copy(
                    exercises = listOf(
                        day.exercises[0].omitting(from = 1),
                        day.exercises[1].omitting(from = 2)
                    )
                )
            }
        }
    )

    private fun replaceProposal() = AdjustmentProposal(
        proposalId = "proposal-1",
        requestId = "request-1",
        sessionId = "day:22",
        baseRevision = "revision-1",
        policyVersion = "test",
        changes = listOf(
            ProposalChange(
                kind = ChangeKind.REPLACE_UNPERFORMED,
                before = ExerciseSnapshot(
                    exerciseInstanceId = "exercise:33",
                    catalogKey = "goblet_squat",
                    sets = listOf(SetSnapshot("set:330", 12, 20f, null, null))
                ),
                after = ExerciseSnapshot(
                    exerciseInstanceId = "new",
                    catalogKey = "dumbbell_step_up",
                    sets = listOf(SetSnapshot("new:1", 12, null, null, null))
                )
            )
        ),
        preservedPerformedSetIds = emptyList(),
        reasonCode = ReasonCode.EQUIPMENT_CONSTRAINT,
        tradeoffCode = TradeoffCode.DIFFERENT_RESISTANCE.wire,
        factReferences = emptyList()
    )

    private fun applied(proposal: AdjustmentProposal = replaceProposal()) = AppliedAdjustment(
        id = 5,
        workoutDayId = 22,
        proposal = proposal,
        reason = AdjustmentReason.EQUIPMENT_UNAVAILABLE,
        appliedAt = 1L
    )

    @Test
    fun anOmittedSetIsHiddenFromTheRoutine() = runTest {
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(adjustedPlan()))
        advanceUntilIdle()

        val exercises = viewModel.uiState.value.routine.exercises
        assertThat(exercises.map { it.name }).containsExactly("Goblet Squats")
        assertThat(exercises.single().sets).hasSize(1)
    }

    @Test
    fun aHiddenExerciseDoesNotBlockCompletion() = runTest {
        val viewModel = viewModel(dayNumber = 3, repository = repositoryWith(adjustedPlan()))
        advanceUntilIdle()

        viewModel.toggleExercise(1)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.routine.isComplete).isTrue()
    }

    @Test
    fun anAdjustedDayReportsFewerExercisesAndMinutesThanItsStoredColumns() {
        val day = adjustedPlan().workoutDays.first { it.id == 22L }

        assertThat(day.derivedExerciseCount()).isLessThan(day.exerciseCount)
        assertThat(day.remainingMinutes(UserProfile(id = 1), SampleWorkoutData.catalog))
            .isLessThan(day.duration)
    }

    @Test
    fun theBannerNamesTheReplacedExercise() = runTest {
        val adjustments = emptyAdjustments()
        coEvery { adjustments.getActiveAdjustment(22L) } returns applied()
        val viewModel = viewModel(
            dayNumber = 3,
            repository = repositoryWith(adjustedPlan()),
            adjustments = adjustments
        )
        advanceUntilIdle()

        val banner = checkNotNull(viewModel.uiState.value.adjustedBanner)
        assertThat(banner.messageRes).isEqualTo(R.string.adjusted_replaced_banner_format)
        assertThat(banner.fromName).isEqualTo(SampleWorkoutData.catalog["goblet_squat"]?.name)
        assertThat(banner.toName).isEqualTo(SampleWorkoutData.catalog["dumbbell_step_up"]?.name)
    }

    @Test
    fun undoRefreshesTheDay() = runTest {
        val adjustments = emptyAdjustments()
        coEvery { adjustments.getActiveAdjustment(22L) } returns applied() andThen null
        coEvery { adjustments.undo(5L, any()) } returns UndoResult.Restored(applied(), 2)
        val repository = mockk<UserRepository>(relaxed = true).also {
            coEvery { it.getCurrentUser() } returns UserProfile(id = 1)
            every { it.getWeeklyWorkoutPlans(1) } returns
                flowOf(listOf(adjustedPlan())) andThen flowOf(listOf(storedPlan))
        }

        val viewModel = viewModel(dayNumber = 3, repository = repository, adjustments = adjustments)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.routine.exercises).hasSize(1)

        viewModel.undoAdjustment()
        advanceUntilIdle()

        coVerify { adjustments.undo(5L, any()) }
        assertThat(viewModel.uiState.value.routine.exercises).hasSize(2)
        assertThat(viewModel.uiState.value.adjustedBanner).isNull()
        assertThat(viewModel.uiState.value.undoKeptSets).isEqualTo(2)

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.undoKeptSets).isNull()
    }

    // Undo brings the omitted rows back, and two sets numbered the same would
    // send an edit or a swipe to the wrong row.
    @Test
    fun anAddedSetNumbersPastTheOmittedRows() = runTest {
        val repository = repositoryWith(adjustedPlan())
        val viewModel = viewModel(dayNumber = 3, repository = repository)
        advanceUntilIdle()

        viewModel.addSet(1)
        advanceUntilIdle()

        assertThat(viewModel.exercise(1).sets.map { it.setNumber }).containsExactly(1, 3).inOrder()
        coVerify { repository.addExerciseSet(match { it.setNumber == 3 }, 33L) }
    }
}
