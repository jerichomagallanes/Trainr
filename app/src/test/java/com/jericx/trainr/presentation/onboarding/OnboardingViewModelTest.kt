package com.jericx.trainr.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.onboarding.OnboardingStep
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.purchases.UnspentAllowance
import com.jericx.trainr.domain.purchases.gateOver
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
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
class OnboardingViewModelTest {

    private val catalog = InMemoryExerciseCatalog(
        listOf(
            CatalogExercise(
                "push_up", "Push Up", MuscleGroup.CHEST, emptyList(),
                Equipment.NONE, ExerciseMeasure.REPS,
                MovementPattern.HORIZONTAL_PUSH, staple = true
            )
        )
    )


    private val allowance = UnspentAllowance()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var planGenerator: PlanGenerator
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        coEvery { userRepository.getCurrentUser() } returns null
        planGenerator = mockk()
        coEvery { planGenerator.generate(any()) } answers {
            PlanGenerationResult.Generated(
                WeeklyWorkoutPlan(
                    userId = firstArg<PlanRequest>().user.id,
                    weekNumber = firstArg<PlanRequest>().weekNumber,
                    title = "Generated week",
                    startDateMillis = firstArg<PlanRequest>().startDateMillis,
                    workoutDays = emptyList()
                )
            )
        }
        viewModel = OnboardingViewModel(userRepository, planGenerator, gateOver(allowance), catalog)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default UserProfile and not loading`() {
        val state = viewModel.onboardingState.value

        // createdAt defaults to the clock, so whole-profile comparison needs it pinned
        assertThat(state.userProfile).isEqualTo(UserProfile(createdAt = state.userProfile.createdAt))
        assertThat(state.isCompleted).isFalse()
    }

    @Test
    fun `updateBasicInfo updates firstName age gender experience`() {
        viewModel.updateBasicInfo(
            firstName = "Jericho",
            age = 30,
            gender = Gender.MALE,
            experience = ExperienceLevel.INTERMEDIATE
        )

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.firstName).isEqualTo("Jericho")
        assertThat(profile.age).isEqualTo(30)
        assertThat(profile.gender).isEqualTo(Gender.MALE)
        assertThat(profile.experienceLevel).isEqualTo(ExperienceLevel.INTERMEDIATE)
    }

    @Test
    fun `updateBodyMetrics updates height and weight without clobbering other fields`() {
        viewModel.updateBasicInfo("Ana", 25, Gender.FEMALE, ExperienceLevel.BEGINNER)

        viewModel.updateBodyMetrics(height = 165f, weight = 60f, UnitSystem.METRIC)

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.height).isEqualTo(165f)
        assertThat(profile.weight).isEqualTo(60f)
        assertThat(profile.firstName).isEqualTo("Ana")
        assertThat(profile.age).isEqualTo(25)
    }

    // A catalog this thin cannot fill an hour and a half, and the screen that
    // asks for the length is told what the sessions will really come to rather
    // than quietly handing over a third of one.
    @Test
    fun aSessionLengthTheSplitCannotFillIsReportedAtWhatItWillComeTo() = runTest {
        val minutes = viewModel.longestSessionMinutes(listOf(Equipment.NONE), daysPerWeek = 3, duration = 90)

        assertThat(minutes).isLessThan(90)
        assertThat(minutes).isGreaterThan(0)
    }

    @Test
    fun `updateFitnessGoal sets the goal`() {
        viewModel.updateFitnessGoal(FitnessGoal.MUSCLE_GAIN)

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.fitnessGoal).isEqualTo(FitnessGoal.MUSCLE_GAIN)
    }

    @Test
    fun `updateWorkoutSetup sets every field it is given`() {
        val equipment = listOf(Equipment.DUMBBELL, Equipment.OTHER)

        viewModel.updateWorkoutSetup(
            equipment = equipment,
            liftingUnits = UnitSystem.IMPERIAL,
            daysPerWeek = 4,
            duration = 45,
        )

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.availableEquipment).containsExactlyElementsIn(equipment)
        assertThat(profile.workoutDaysPerWeek).isEqualTo(4)
        assertThat(profile.workoutDuration).isEqualTo(45)
        assertThat(profile.liftingUnitSystem).isEqualTo(UnitSystem.IMPERIAL)
    }

    @Test
    fun `body units and lifting units are kept apart`() {
        viewModel.updateBodyMetrics(178f, 75f, UnitSystem.IMPERIAL)
        viewModel.updateWorkoutSetup(
            equipment = listOf(Equipment.BARBELL),
            liftingUnits = UnitSystem.METRIC,
            daysPerWeek = 4,
            duration = 45,
        )

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.bodyUnitSystem).isEqualTo(UnitSystem.IMPERIAL)
        assertThat(profile.liftingUnitSystem).isEqualTo(UnitSystem.METRIC)
        assertThat(profile.weightUnits).isEqualTo(UnitSystem.METRIC)
    }

    @Test
    fun `without loaded equipment the sets follow the body units`() {
        viewModel.updateBodyMetrics(178f, 75f, UnitSystem.IMPERIAL)
        viewModel.updateWorkoutSetup(
            equipment = listOf(Equipment.NONE),
            liftingUnits = null,
            daysPerWeek = 3,
            duration = 30,
        )

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.liftingUnitSystem).isNull()
        assertThat(profile.weightUnits).isEqualTo(UnitSystem.IMPERIAL)
    }

    @Test
    fun `updateLimitations sets injuries`() {
        val injuries = listOf(Injury.LOWER_BACK, Injury.KNEE)

        viewModel.updateLimitations(injuries)

        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.injuries).containsExactlyElementsIn(injuries)
    }

    @Test
    fun `updateLimitations leaves the goal alone`() {
        viewModel.updateFitnessGoal(FitnessGoal.ENDURANCE)

        viewModel.updateLimitations(listOf(Injury.KNEE))

        assertThat(viewModel.onboardingState.value.userProfile.fitnessGoal)
            .isEqualTo(FitnessGoal.ENDURANCE)
    }

    @Test
    fun `saveUserProfile on success sets isCompleted and invokes callback`() = runTest(testDispatcher) {
        var callbackInvoked = false

        viewModel.saveUserProfile(onSuccess = { callbackInvoked = true })
        advanceUntilIdle()

        val state = viewModel.onboardingState.value
        assertThat(state.isCompleted).isTrue()
        assertThat(callbackInvoked).isTrue()
        coVerify { userRepository.saveUserWithFirstWeek(any(), any()) }
    }

    @Test
    fun `the generated plan is stored with the user in one operation`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns null
        val plan = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveUserWithFirstWeek(any(), capture(plan)) } returns 42L

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        with(plan.captured) {
            assertThat(weekNumber).isEqualTo(1)
            assertThat(startDateMillis).isNotNull()
        }
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    @Test
    fun `a failed generation writes no plan and reports why`() = runTest(testDispatcher) {
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Failed
        var done = false

        viewModel.saveUserProfile(onSuccess = { done = true })
        advanceUntilIdle()

        with(viewModel.onboardingState.value) {
            assertThat(generationFailure).isEqualTo(PlanGenerationResult.Failed)
            assertThat(isCompleted).isFalse()
        }
        assertThat(done).isFalse()
        coVerify(exactly = 0) { userRepository.saveUserWithFirstWeek(any(), any()) }
    }

    // The answers survive a failure nobody caused; saved without a plan, the app opens on the empty state
    @Test
    fun `a first profile is kept when generation fails`() = runTest(testDispatcher) {
        coEvery { planGenerator.generate(any()) } returns
            PlanGenerationResult.Failed

        viewModel.saveUserProfile()
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 0) { userRepository.saveUserWithFirstWeek(any(), any()) }
    }

    // Saving the user first REPLACEs the row, which cascades every stored week away
    @Test
    fun `a failed regeneration leaves the stored plan alone`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 7)
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Failed

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveUser(any()) }
        coVerify(exactly = 0) { userRepository.saveUserWithFirstWeek(any(), any()) }
    }

    @Test
    fun `a generated plan is saved instead of the sample fallback`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns null
        val generated = WeeklyWorkoutPlan(
            userId = 42L, weekNumber = 1, title = "Generated week",
            startDateMillis = 1L, workoutDays = emptyList()
        )
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(generated)
        val saved = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveUserWithFirstWeek(any(), capture(saved)) } returns 42L

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        assertThat(saved.captured).isEqualTo(generated)
        with(request.captured) {
            // Generation runs before the user row exists, so a new client's id is still 0
            assertThat(user.id).isEqualTo(0L)
            assertThat(weekNumber).isEqualTo(1)
            assertThat(previousWeek).isNull()
        }
    }

    @Test
    fun `a stored profile is loaded so editing starts from saved answers`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 7L, firstName = "Jeco", age = 26)
        val loaded = OnboardingViewModel(userRepository, planGenerator, gateOver(allowance), catalog)

        advanceUntilIdle()

        assertThat(loaded.onboardingState.value.userProfile.firstName).isEqualTo("Jeco")
        assertThat(loaded.onboardingState.value.userProfile.age).isEqualTo(26)
    }

    @Test
    fun `saveUserProfile replaces the existing user rather than adding another`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 7L, firstName = "Old")

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        coVerify { userRepository.saveUserWithFirstWeek(match { it.id == 7L }, any()) }
    }

    @Test
    fun `saveUserProfile on repository failure surfaces error and does not complete`() = runTest(testDispatcher) {
        coEvery { userRepository.saveUserWithFirstWeek(any(), any()) } throws
            RuntimeException("DB write failed")
        var callbackInvoked = false

        viewModel.saveUserProfile(onSuccess = { callbackInvoked = true })
        advanceUntilIdle()

        val state = viewModel.onboardingState.value
        assertThat(state.isCompleted).isFalse()
        assertThat(callbackInvoked).isFalse()
    }
    // Regenerating wipes history; editing the profile must not, so it updates rather than re-inserts
    @Test
    fun `updateProfileOnly saves the profile and leaves the plan alone`() = runTest {
        val stored = UserProfile(id = 4, firstName = "Jet", age = 28)
        coEvery { userRepository.getCurrentUser() } returns stored
        val viewModel = OnboardingViewModel(userRepository, planGenerator, gateOver(allowance), catalog)
        advanceUntilIdle()
        viewModel.updateFitnessGoal(FitnessGoal.STRENGTH)

        var done = false
        viewModel.updateProfileOnly { done = true }
        advanceUntilIdle()

        coVerify {
            userRepository.updateUser(
                match { it.id == 4L && it.fitnessGoal == FitnessGoal.STRENGTH }
            )
        }
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
        coVerify(exactly = 0) { userRepository.saveUserWithFirstWeek(any(), any()) }
        coVerify(exactly = 0) { planGenerator.generate(any()) }
        assertThat(done).isTrue()
    }

    // Anchoring week one to the Monday just gone would hand a late signup already-missed sessions
    @Test
    fun `the first week starts today`() = runTest {
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns PlanGenerationResult.Failed
        // The request is captured before generation is allowed to fail.

        viewModel.saveUserProfile {}
        advanceUntilIdle()

        assertThat(request.captured.startDateMillis)
            .isEqualTo(WorkoutWeek.startOfDay())
    }

    @Test
    fun `a second tap while generating is ignored`() = runTest(testDispatcher) {
        viewModel.saveUserProfile()
        viewModel.saveUserProfile()
        advanceUntilIdle()

        coVerify(exactly = 1) { planGenerator.generate(any()) }
        coVerify(exactly = 1) { userRepository.saveUserWithFirstWeek(any(), any()) }
    }

    // The view model outlives the screen, so a regeneration must not start out complete
    @Test
    fun `regenerating starts from not complete`() = runTest(testDispatcher) {
        viewModel.saveUserProfile()
        advanceUntilIdle()
        assertThat(viewModel.onboardingState.value.isCompleted).isTrue()

        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Failed
        viewModel.saveUserProfile()
        advanceUntilIdle()

        assertThat(viewModel.onboardingState.value.isCompleted).isFalse()
    }


    // The profile cannot say whether a step was answered: every enum field starts on a real value
    @Test
    fun `a step is only marked answered once it has been filled in`() {
        assertThat(viewModel.onboardingState.value.answeredSteps).isEmpty()

        viewModel.updateBodyMetrics(175f, 70f, UnitSystem.METRIC)

        assertThat(viewModel.onboardingState.value.answeredSteps)
            .containsExactly(OnboardingStep.BODY_METRICS)
    }

    @Test
    fun `answered steps accumulate rather than replace one another`() {
        viewModel.updateBasicInfo("Jericho", 31, Gender.FEMALE, ExperienceLevel.ADVANCED)
        viewModel.updateFitnessGoal(FitnessGoal.STRENGTH)

        assertThat(viewModel.onboardingState.value.answeredSteps)
            .containsExactly(OnboardingStep.BASIC_INFO, OnboardingStep.GOALS)
    }

    @Test
    fun `a preferred name is stored without the space that was typed after it`() {
        viewModel.updateBasicInfo("Jericho ", 30, Gender.MALE, ExperienceLevel.BEGINNER)

        assertThat(viewModel.onboardingState.value.userProfile.firstName).isEqualTo("Jericho")
    }

    @Test
    fun `measurements that were typed but not submitted are given back`() {
        viewModel.rememberBodyMetrics(BodyMetricsEntry("5", "154", useMetric = false))

        assertThat(viewModel.bodyMetricsInProgress())
            .isEqualTo(BodyMetricsEntry("5", "154", useMetric = false))
    }

    @Test
    fun `submitting the measurements step clears what it was holding`() {
        viewModel.rememberBodyMetrics(BodyMetricsEntry("175", "70", useMetric = true))

        viewModel.updateBodyMetrics(175f, 70f, UnitSystem.METRIC)

        assertThat(viewModel.bodyMetricsInProgress()).isNull()
    }

    // The screen can be left while this runs, and it writes the week anyway.
    // Backing out of it used to hand over a week of training for nothing.
    @Test
    fun `a week written after the screen is gone is charged for all the same`() =
        runTest(testDispatcher) {
            viewModel.saveUserProfile()
            advanceUntilIdle()

            coVerify { userRepository.saveUserWithFirstWeek(any(), any()) }
            assertThat(allowance.wasSpent).isTrue()
        }

    @Test
    fun `a generation that wrote no week costs nothing`() = runTest(testDispatcher) {
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Failed

        viewModel.saveUserProfile()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveUserWithFirstWeek(any(), any()) }
        assertThat(allowance.wasSpent).isFalse()
    }
}
