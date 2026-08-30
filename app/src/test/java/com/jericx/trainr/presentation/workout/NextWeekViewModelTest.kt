package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
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
import java.util.concurrent.TimeUnit

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

    // Recent enough that the week after it still lies ahead, which is the
    // ordinary case: the next week follows the last one.
    private val weekOneStart = WorkoutWeek.startOfDay() - TimeUnit.DAYS.toMillis(3)

    private val finishedWeek = WeeklyWorkoutPlan(
        id = 9,
        userId = 1,
        weekNumber = 1,
        title = "Foundation - Week 1",
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
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(generated)

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
            PlanGenerationResult.Generated(weekTwo.copy(id = 0, weekNumber = 3))

        viewModel().generateNextWeek {}
        advanceUntilIdle()

        assertThat(request.captured.weekNumber).isEqualTo(3)
        assertThat(request.captured.previousWeek).isEqualTo(weekTwo)
    }

    // A week that could not be generated is said out loud. Repeating the last
    // one used to stand in silently, which told the client next week was ready
    // when the coach never wrote it.
    @Test
    fun aFailedGenerationSavesNothingAndReportsWhy() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Offline

        var done = false
        val viewModel = viewModel()
        viewModel.generateNextWeek { done = true }
        advanceUntilIdle()

        assertThat(viewModel.failure.value).isEqualTo(PlanGenerationResult.Offline)
        assertThat(done).isFalse()
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
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
    // Coming back long after the plan ran out, the next week starts today
    // rather than on a date that has already gone: nothing is missed before it
    // begins, and no two weeks ever cover the same days.
    @Test
    fun aWeekPickedUpLateStartsToday() = runTest {
        val longAgo = finishedWeek.copy(
            startDateMillis = WorkoutWeek.startOfDay() - TimeUnit.DAYS.toMillis(40)
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(longAgo))
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(longAgo.copy(weekNumber = 2))

        viewModel().generateNextWeek {}
        advanceUntilIdle()

        assertThat(request.captured.startDateMillis).isEqualTo(WorkoutWeek.startOfDay())
    }

    @Test
    fun aWeekThatStillLiesAheadFollowsTheOneBeforeIt() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(finishedWeek.copy(weekNumber = 2))

        viewModel().generateNextWeek {}
        advanceUntilIdle()

        assertThat(request.captured.startDateMillis)
            .isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 8))
        assertThat(request.captured.startDateMillis).isGreaterThan(WorkoutWeek.startOfDay())
    }

    // Running the same week again is sound coaching after a week that was not
    // finished, and it asks nothing of the network. It is chosen, not
    // substituted, so it saves the week with every log cleared.
    @Test
    fun repeatingTheLastWeekCopiesItWithNothingLogged() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        val saved = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveWeeklyWorkoutPlan(capture(saved)) } returns 2L

        var done = false
        viewModel().repeatLastWeek { done = true }
        advanceUntilIdle()

        with(saved.captured) {
            assertThat(id).isEqualTo(0)
            assertThat(weekNumber).isEqualTo(2)
            // A copy of week one must not sit at week two still calling itself
            // the first.
            assertThat(title).doesNotContain("Week 1")
            assertThat(startDateMillis).isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 8))
            val day = workoutDays.single()
            assertThat(day.status).isEqualTo(WorkoutStatus.NOT_STARTED)
            assertThat(day.completedAt).isNull()
            val set = day.exercises.single().sets.single()
            assertThat(set.targetReps).isEqualTo(10)
            assertThat(set.actualReps).isNull()
            assertThat(set.isCompleted).isFalse()
        }
        coVerify(exactly = 0) { planGenerator.generate(any()) }
        assertThat(done).isTrue()
    }

    @Test
    fun repeatingCannotStackASecondCopyOfAWeekThatExists() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { userRepository.getWeeklyWorkoutPlan(1, 2) } returns
            finishedWeek.copy(id = 10, weekNumber = 2)

        viewModel().repeatLastWeek {}
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

}
