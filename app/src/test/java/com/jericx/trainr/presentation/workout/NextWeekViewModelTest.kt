package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSource
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

    private fun viewModel() = NextWeekViewModel(userRepository, planGenerator)

    // Recent enough that the week after it still lies ahead, the ordinary case
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

                val viewModel = viewModel()
        viewModel.generateNextWeek()
        advanceUntilIdle()

        assertThat(request.captured.weekNumber).isEqualTo(2)
        assertThat(request.captured.freshCast).isFalse()
        assertThat(request.captured.previousWeek).isEqualTo(finishedWeek)
        assertThat(request.captured.startDateMillis)
            .isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 8))
        coVerify { userRepository.saveWeeklyWorkoutPlan(generated) }
        assertThat(viewModel.isReady.value).isTrue()
    }

    @Test
    fun buildsOnTheLatestWeekWhenSeveralAreStored() = runTest {
        val weekTwo = finishedWeek.copy(id = 10, weekNumber = 2)
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(finishedWeek, weekTwo))
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(weekTwo.copy(id = 0, weekNumber = 3))

        viewModel().generateNextWeek()
        advanceUntilIdle()

        assertThat(request.captured.weekNumber).isEqualTo(3)
        assertThat(request.captured.previousWeek).isEqualTo(weekTwo)
    }

    @Test
    fun aFailedGenerationSavesNothingAndReportsWhy() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Offline

                val viewModel = viewModel()
        viewModel.generateNextWeek()
        advanceUntilIdle()

        assertThat(viewModel.failure.value).isEqualTo(PlanGenerationResult.Offline)
        assertThat(viewModel.isReady.value).isFalse()
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    @Test
    fun anAlreadyGeneratedWeekIsNotGeneratedAgain() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { userRepository.getWeeklyWorkoutPlan(1, 2) } returns
            finishedWeek.copy(id = 10, weekNumber = 2)

                val viewModel = viewModel()
        viewModel.generateNextWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { planGenerator.generate(any()) }
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        assertThat(viewModel.isReady.value).isTrue()
    }

    @Test
    fun finishesQuietlyWhenThereIsNothingToBuildOn() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(emptyList())

                val viewModel = viewModel()
        viewModel.generateNextWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        assertThat(viewModel.isReady.value).isTrue()
    }
    // Starting on a date already gone would miss sessions before they began and overlap the week before
    @Test
    fun aWeekPickedUpLateStartsToday() = runTest {
        val longAgo = finishedWeek.copy(
            startDateMillis = WorkoutWeek.startOfDay() - TimeUnit.DAYS.toMillis(40)
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(longAgo))
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(longAgo.copy(weekNumber = 2))

        viewModel().generateNextWeek()
        advanceUntilIdle()

        assertThat(request.captured.startDateMillis).isEqualTo(WorkoutWeek.startOfDay())
    }

    @Test
    fun aWeekThatStillLiesAheadFollowsTheOneBeforeIt() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(finishedWeek.copy(weekNumber = 2))

        viewModel().generateNextWeek()
        advanceUntilIdle()

        assertThat(request.captured.startDateMillis)
            .isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 8))
        assertThat(request.captured.startDateMillis).isGreaterThan(WorkoutWeek.startOfDay())
    }

    // The week before seeds the request, so a replacement still progresses from what was lifted
    @Test
    fun regeneratingReplacesTheWeekBeingTrainedInPlace() = runTest {
        val current = finishedWeek.copy(
            id = 10,
            weekNumber = 2,
            startDateMillis = WorkoutWeek.dateOfDay(weekOneStart, 8),
            workoutDays = finishedWeek.workoutDays.map {
                it.copy(status = WorkoutStatus.NOT_STARTED, completedAt = null)
            }
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(finishedWeek, current))
        val request = slot<PlanRequest>()
        val replacement = current.copy(id = 0, title = "Something better")
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(replacement)

        viewModel().regenerateThisWeek()
        advanceUntilIdle()

        assertThat(request.captured.weekNumber).isEqualTo(2)
        assertThat(request.captured.startDateMillis).isEqualTo(current.startDateMillis)
        assertThat(request.captured.previousWeek?.weekNumber).isEqualTo(1)
        assertThat(request.captured.freshCast).isTrue()
        coVerify { userRepository.deleteWeeklyWorkoutPlan(10) }
        coVerify { userRepository.saveWeeklyWorkoutPlan(replacement) }
    }

    // Ask the model first and drop the old week only once there is one to put in its place
    @Test
    fun aFailedRegenerationLeavesTheWeekWhereItWas() = runTest {
        val current = finishedWeek.copy(
            workoutDays = finishedWeek.workoutDays.map {
                it.copy(status = WorkoutStatus.NOT_STARTED, completedAt = null)
            }
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(current))
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Offline

        val viewModel = viewModel()
        viewModel.regenerateThisWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.deleteWeeklyWorkoutPlan(any()) }
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        assertThat(viewModel.failure.value).isEqualTo(PlanGenerationResult.Offline)
        assertThat(viewModel.isReady.value).isFalse()
    }

    @Test
    fun aWeekYouAreDoneWithIsNotRegenerated() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))

        viewModel().regenerateThisWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { planGenerator.generate(any()) }
        coVerify(exactly = 0) { userRepository.deleteWeeklyWorkoutPlan(any()) }
    }

    // Enforced at the write, not only where the menu is drawn: a newer week would take the title of current
    @Test
    fun neitherWayOnIsTakenWhileTheWeekIsStillBeingTrained() = runTest {
        val unfinished = finishedWeek.copy(
            startDateMillis = WorkoutWeek.startOfDay(),
            workoutDays = finishedWeek.workoutDays.map {
                it.copy(status = WorkoutStatus.NOT_STARTED, completedAt = null)
            }
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(unfinished))

        viewModel().generateNextWeek()
        advanceUntilIdle()
        viewModel().repeatWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        coVerify(exactly = 0) { planGenerator.generate(any()) }
    }

    @Test
    fun repeatingAnOlderWeekAppendsItAtTheEnd() = runTest {
        val weekTwo = finishedWeek.copy(
            id = 10,
            weekNumber = 2,
            startDateMillis = WorkoutWeek.dateOfDay(weekOneStart, 8),
            workoutDays = finishedWeek.workoutDays.map { it.copy(id = 11, title = "Upper body") }
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns
            flowOf(listOf(finishedWeek, weekTwo))
        val saved = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveWeeklyWorkoutPlan(capture(saved)) } returns 3L

        viewModel().repeatWeek(sourceWeekNumber = 1)
        advanceUntilIdle()

        with(saved.captured) {
            assertThat(weekNumber).isEqualTo(3)
            assertThat(workoutDays.single().title).isEqualTo("Full body")
            // Follows week two rather than reaching back over it
            assertThat(startDateMillis)
                .isEqualTo(WorkoutWeek.dateOfDay(weekOneStart, 15))
            assertThat(workoutDays.single().status).isEqualTo(WorkoutStatus.NOT_STARTED)
        }
    }

    @Test
    fun repeatingTheLastWeekCopiesItWithNothingLogged() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        val saved = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveWeeklyWorkoutPlan(capture(saved)) } returns 2L

        val viewModel = viewModel()
        viewModel.repeatWeek()
        advanceUntilIdle()

        with(saved.captured) {
            assertThat(id).isEqualTo(0)
            assertThat(weekNumber).isEqualTo(2)
            // A copy of week one must not sit at week two still calling itself the first
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
        assertThat(viewModel.isReady.value).isTrue()
    }

    @Test
    fun repeatingCannotStackASecondCopyOfAWeekThatExists() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { userRepository.getWeeklyWorkoutPlan(1, 2) } returns
            finishedWeek.copy(id = 10, weekNumber = 2)

        viewModel().repeatWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    @Test
    fun aSecondAskWhileGeneratingIsIgnored() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        coEvery { planGenerator.generate(any()) } returns
            PlanGenerationResult.Generated(finishedWeek.copy(id = 0, weekNumber = 2))

        val viewModel = viewModel()
        viewModel.generateNextWeek()
        viewModel.generateNextWeek()
        viewModel.generateNextWeek()
        advanceUntilIdle()

        coVerify(exactly = 1) { planGenerator.generate(any()) }
        coVerify(exactly = 1) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    @Test
    fun repeatingTwiceOverStillWritesOneWeek() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))

        val viewModel = viewModel()
        viewModel.repeatWeek()
        viewModel.repeatWeek()
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    // Regenerating is asked for to get a different week from the coach, so
    // the app's own week is no answer to it.
    @Test
    fun aRegenerationTheCoachCouldNotAnswerLeavesTheWeekAndSaysWhy() = runTest {
        val current = finishedWeek.copy(
            workoutDays = finishedWeek.workoutDays.map {
                it.copy(status = WorkoutStatus.NOT_STARTED, completedAt = null)
            }
        )
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(current))
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Generated(
            finishedWeek.copy(id = 0), PlanSource.TEMPLATE, PlanGenerationResult.Offline
        )

        val viewModel = viewModel()
        viewModel.regenerateThisWeek()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.deleteWeeklyWorkoutPlan(any()) }
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        assertThat(viewModel.failure.value).isEqualTo(PlanGenerationResult.Offline)
        assertThat(viewModel.isReady.value).isFalse()
    }

    @Test
    fun aNextWeekBuiltInPlaceOfTheCoachsIsSavedAndSaysWhy() = runTest {
        every { userRepository.getWeeklyWorkoutPlans(1) } returns flowOf(listOf(finishedWeek))
        val built = finishedWeek.copy(id = 0, weekNumber = 2, title = "Built")
        coEvery { planGenerator.generate(any()) } returns
            PlanGenerationResult.Generated(built, PlanSource.TEMPLATE, PlanGenerationResult.DailyLimitReached)

        val viewModel = viewModel()
        viewModel.generateNextWeek()
        advanceUntilIdle()

        coVerify { userRepository.saveWeeklyWorkoutPlan(built) }
        assertThat(viewModel.isReady.value).isTrue()
        assertThat(viewModel.failure.value).isNull()
        assertThat(viewModel.source.value).isEqualTo(PlanSource.TEMPLATE)
        assertThat(viewModel.builtInsteadOf.value).isEqualTo(PlanGenerationResult.DailyLimitReached)
    }
}
