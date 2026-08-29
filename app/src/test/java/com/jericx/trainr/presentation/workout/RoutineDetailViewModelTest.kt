package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

    private fun viewModel(dayNumber: Int = SampleWorkoutData.DEFAULT_DAY_NUMBER) = RoutineDetailViewModel(
        SavedStateHandle(mapOf(Screen.RoutineDetail.ARG_DAY_NUMBER to dayNumber))
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
}
