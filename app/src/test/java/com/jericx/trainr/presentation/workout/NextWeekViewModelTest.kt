package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

@OptIn(ExperimentalCoroutinesApi::class)
class NextWeekViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var planGenerator: PlanGenerator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        planGenerator = mockk()
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 1)
        coEvery { userRepository.getWeeklyWorkoutPlan(1, any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NextWeekViewModel(userRepository, planGenerator) { "en" }

    private val weekOneStart = WorkoutWeek.mondayOf(1_755_000_000_000L)

    private val finishedWeek = WeeklyWorkoutPlan(
        id = 9,
        userId = 1,
        weekNumber = 1,
        title = "Foundation",
        startDateMillis = weekOneStart,
        workoutDays = listOf(
            WorkoutDay(
                id = 4,
                dayNumber = 1,
                title = "Full body",
                status = WorkoutStatus.COMPLETED,
                duration = 45,
                exerciseCount = 1,
                equipment = emptyList(),
                completedAt = 5L,
                exercises = listOf(
                    WorkoutExercise(
                        id = 7,
                        name = "Goblet squat",
                        isCompleted = true,
                        sets = listOf(
                            ExerciseSet(
                                id = 3,
                                setNumber = 1,
                                targetReps = 10,
                                actualReps = 12,
                                actualWeightKg = 16f,
                                isCompleted = true
                            )
                        )
                    )
                )
            )
        )
    )

    @Test
    fun asksTheGeneratorForTheWeekAfterTheLatestOne() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        val request = slot<PlanRequest>()
        val generated = finishedWeek.copy(id = 0, weekNumber = 2, title = "Progression")
        coEvery { planGenerator.generate(capture(request)) } returns generated

        var done = false
        viewModel().generateNextWeek { done = true }
        advanceUntilIdle()

        assertThat(request.captured.weekNumber).isEqualTo(2)
        assertThat(request.captured.previousWeek).isEqualTo(finishedWeek)
        assertThat(request.captured.startDateMillis)
            .isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 8))
        coVerify { userRepository.saveWeeklyWorkoutPlan(generated) }
        assertThat(done).isTrue()
    }

    @Test
    fun buildsOnTheLatestWeekWhenSeveralAreStored() = runTest {
        val weekTwo = finishedWeek.copy(id = 10, weekNumber = 2)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(finishedWeek, weekTwo))
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            weekTwo.copy(id = 0, weekNumber = 3)

        viewModel().generateNextWeek {}
        advanceUntilIdle()

        assertThat(request.captured.weekNumber).isEqualTo(3)
        assertThat(request.captured.previousWeek).isEqualTo(weekTwo)
    }

    // The fallback repeats the finished week rather than inventing progress,
    // and every log and storage id from the old week must be cleared.
    @Test
    fun fallsBackToRepeatingTheWeekWithClearedLogs() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { planGenerator.generate(any()) } returns null
        val saved = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveWeeklyWorkoutPlan(capture(saved)) } returns 2L

        viewModel().generateNextWeek {}
        advanceUntilIdle()

        with(saved.captured) {
            assertThat(id).isEqualTo(0)
            assertThat(weekNumber).isEqualTo(2)
            assertThat(startDateMillis).isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 8))
            val day = workoutDays.single()
            assertThat(day.id).isEqualTo(0)
            assertThat(day.status).isEqualTo(WorkoutStatus.NOT_STARTED)
            assertThat(day.completedAt).isNull()
            val exercise = day.exercises.single()
            assertThat(exercise.id).isEqualTo(0)
            assertThat(exercise.isCompleted).isFalse()
            val set = exercise.sets.single()
            assertThat(set.id).isEqualTo(0)
            assertThat(set.targetReps).isEqualTo(10)
            assertThat(set.actualReps).isNull()
            assertThat(set.actualWeightKg).isNull()
            assertThat(set.isCompleted).isFalse()
        }
    }

    // Reopening the completion screen after the next week exists must not
    // stack duplicates.
    @Test
    fun anAlreadyGeneratedWeekIsNotGeneratedAgain() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { userRepository.getWeeklyWorkoutPlan(1, 2) } returns
            finishedWeek.copy(id = 10, weekNumber = 2)

        var done = false
        viewModel().generateNextWeek { done = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { planGenerator.generate(any()) }
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        assertThat(done).isTrue()
    }

    @Test
    fun finishesQuietlyWhenThereIsNothingToBuildOn() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(emptyList())

        var done = false
        viewModel().generateNextWeek { done = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        assertThat(done).isTrue()
    }
}
