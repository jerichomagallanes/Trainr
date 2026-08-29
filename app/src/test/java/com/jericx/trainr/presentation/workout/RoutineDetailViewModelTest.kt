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
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private fun viewModel(
        dayNumber: Int = SampleWorkoutData.DEFAULT_DAY_NUMBER,
        repository: UserRepository = emptyRepository()
    ) = RoutineDetailViewModel(
        SavedStateHandle(mapOf(Screen.RoutineDetail.ARG_DAY_NUMBER to dayNumber)),
        repository
    )

    private fun RoutineDetailViewModel.exercise(position: Int): ExerciseUi =
        uiState.value.routine.exercises.first { it.position == position }

    private fun RoutineDetailViewModel.isCompleted(position: Int) = exercise(position).isCompleted

    // The design's completion screen reads "Day 2": Cardio & Core is the second
    // workout day of the week, even though its dayNumber is 3 (Wednesday).
    @Test
    fun theRoutineKnowsWhichWorkoutDayOfTheWeekItIs() = runTest {
        assertThat(viewModel().uiState.value.dayNumber).isEqualTo(2)
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

    // A started-but-unfinished day is still outstanding.
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

    // Friday is still untouched in the sample plan, so Wednesday ends the day.
    @Test
    fun theSampleRoutineEndsTheDayRatherThanTheWeek() = runTest {
        assertThat(viewModel().uiState.value.completesTheWeek).isFalse()
    }

    @Test
    fun aRoutineIsNotCompleteUntilEveryExerciseIs() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.uiState.value.routine.isComplete).isFalse()

        viewModel.completeRoutine()

        assertThat(viewModel.uiState.value.routine.isComplete).isTrue()
    }

    @Test
    fun startingATimerCountsDownFromTheExerciseDuration() = runTest {
        val viewModel = viewModel()

        viewModel.startTimer(viewModel.exercise(2))
        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(600)

        advanceTimeBy(3_000)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(597)
    }

    @Test
    fun pausingHoldsTheCountdownWhereItIs() = runTest {
        val viewModel = viewModel()

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
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(30_000)
        runCurrent()
        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(570)

        viewModel.resetTimer()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(600)
        assertThat(viewModel.uiState.value.timer?.isRunning).isFalse()
    }

    // Reset prepares another go; it does not start one.
    @Test
    fun aResetTimerDoesNotTickUntilItIsResumed() = runTest {
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.resetTimer()

        assertThat(viewModel.uiState.value.timer).isNull()
    }

    @Test
    fun stoppingClearsTheTimerWithoutCompletingTheExercise() = runTest {
        val viewModel = viewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.stopTimer()

        assertThat(viewModel.uiState.value.timer).isNull()
        assertThat(viewModel.isCompleted(2)).isFalse()
    }

    // Running out of time is what finishes an exercise.
    @Test
    fun theCountdownReachingZeroCompletesTheExercise() = runTest {
        val viewModel = viewModel()

        viewModel.startTimer(viewModel.exercise(4))
        advanceTimeBy(4 * 60 * 1_000L)
        runCurrent()

        assertThat(viewModel.uiState.value.timer).isNull()
        assertThat(viewModel.isCompleted(4)).isTrue()
    }

    @Test
    fun theCountdownKeepsRunningRightUpToTheLastSecond() = runTest {
        val viewModel = viewModel()

        viewModel.startTimer(viewModel.exercise(4))
        advanceTimeBy(4 * 60 * 1_000L - 1_000L)
        runCurrent()

        assertThat(viewModel.uiState.value.timer?.remainingSeconds).isEqualTo(1)
        assertThat(viewModel.isCompleted(4)).isFalse()
    }

    // Two countdowns racing would be nonsense, so a new one replaces the old.
    @Test
    fun startingAnotherExerciseReplacesTheRunningTimer() = runTest {
        val viewModel = viewModel()

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
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.startTimer(viewModel.exercise(2))
        advanceTimeBy(3_000)
        runCurrent()
        viewModel.toggleExercise(1)

        assertThat(viewModel.uiState.value.timer?.position).isEqualTo(2)
    }

    @Test
    fun tutorialsStartCollapsedAndToggleIndependently() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.uiState.value.expandedVideos).isEmpty()

        viewModel.toggleVideo(2)
        viewModel.toggleVideo(3)
        assertThat(viewModel.uiState.value.expandedVideos).containsExactly(2, 3)

        viewModel.toggleVideo(2)
        assertThat(viewModel.uiState.value.expandedVideos).containsExactly(3)
    }

    // Only one WebView should ever be alive, so playing one stops the other.
    @Test
    fun playingATutorialStopsWhicheverWasPlaying() = runTest {
        val viewModel = viewModel()

        viewModel.toggleVideo(2)
        viewModel.playVideo(2)
        assertThat(viewModel.uiState.value.playingVideo).isEqualTo(2)

        viewModel.toggleVideo(3)
        viewModel.playVideo(3)
        assertThat(viewModel.uiState.value.playingVideo).isEqualTo(3)
    }

    @Test
    fun collapsingAPlayingTutorialStopsIt() = runTest {
        val viewModel = viewModel()

        viewModel.toggleVideo(2)
        viewModel.playVideo(2)
        viewModel.toggleVideo(2)

        assertThat(viewModel.uiState.value.playingVideo).isNull()
        assertThat(viewModel.uiState.value.expandedVideos).isEmpty()
    }

    @Test
    fun collapsingADifferentTutorialLeavesThePlayingOneAlone() = runTest {
        val viewModel = viewModel()

        viewModel.toggleVideo(2)
        viewModel.playVideo(2)
        viewModel.toggleVideo(3)
        viewModel.toggleVideo(3)

        assertThat(viewModel.uiState.value.playingVideo).isEqualTo(2)
    }

    @Test
    fun completingTheWholeRoutineClearsTheTimer() = runTest {
        val viewModel = viewModel()

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
            prescription = "2 sets of 12 reps",
            instructions = "Stored instructions.",
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
            coEvery { it.getWeeklyWorkoutPlan(1, 1) } returns plan
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

    // The stored routine replaces the sample synchronously shown before it; the
    // screen must be able to tell the swap from the user finishing the day.
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
    fun theSampleFallbackPersistsNothing() = runTest {
        val repository = emptyRepository()
        val viewModel = viewModel(repository = repository)
        advanceUntilIdle()

        viewModel.toggleExercise(3)
        viewModel.updateSet(3, viewModel.exercise(3).sets.first().copy(actualReps = 20))
        viewModel.addSet(3)
        viewModel.completeRoutine()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateWorkoutExercise(any(), any()) }
        coVerify(exactly = 0) { repository.updateWorkoutDay(any(), any()) }
        coVerify(exactly = 0) { repository.updateExerciseSet(any(), any()) }
        coVerify(exactly = 0) { repository.addExerciseSet(any(), any()) }
    }
}
