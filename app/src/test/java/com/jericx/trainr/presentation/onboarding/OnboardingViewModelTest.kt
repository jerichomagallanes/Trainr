package com.jericx.trainr.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.model.WorkoutTime
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
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
        viewModel = OnboardingViewModel(userRepository, planGenerator) { "en" }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default UserProfile and not loading`() {
        // Arrange / Act
        val state = viewModel.onboardingState.value

        // Assert
        // createdAt defaults to System.currentTimeMillis(), so comparing whole
        // profiles fails whenever the clock ticks between the two constructions.
        assertThat(state.userProfile).isEqualTo(UserProfile(createdAt = state.userProfile.createdAt))
        assertThat(state.isLoading).isFalse()
        assertThat(state.isCompleted).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `updateBasicInfo updates firstName age gender experience`() {
        // Arrange / Act
        viewModel.updateBasicInfo(
            firstName = "Jericho",
            age = 30,
            gender = Gender.MALE,
            experience = ExperienceLevel.INTERMEDIATE
        )

        // Assert
        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.firstName).isEqualTo("Jericho")
        assertThat(profile.age).isEqualTo(30)
        assertThat(profile.gender).isEqualTo(Gender.MALE)
        assertThat(profile.experienceLevel).isEqualTo(ExperienceLevel.INTERMEDIATE)
    }

    @Test
    fun `updateBodyMetrics updates height and weight without clobbering other fields`() {
        // Arrange
        viewModel.updateBasicInfo("Ana", 25, Gender.FEMALE, ExperienceLevel.BEGINNER)

        // Act
        viewModel.updateBodyMetrics(height = 165f, weight = 60f)

        // Assert
        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.height).isEqualTo(165f)
        assertThat(profile.weight).isEqualTo(60f)
        assertThat(profile.firstName).isEqualTo("Ana")
        assertThat(profile.age).isEqualTo(25)
    }

    @Test
    fun `updateFitnessGoal sets the chosen goal`() {
        // Arrange / Act
        viewModel.updateFitnessGoal(FitnessGoal.MUSCLE_GAIN)

        // Assert
        assertThat(viewModel.onboardingState.value.userProfile.fitnessGoal)
            .isEqualTo(FitnessGoal.MUSCLE_GAIN)
    }

    @Test
    fun `updateWorkoutSetup sets all five fields`() {
        // Arrange
        val equipment = listOf(Equipment.DUMBBELLS, Equipment.BENCH)

        // Act
        viewModel.updateWorkoutSetup(
            location = WorkoutLocation.HOME,
            equipment = equipment,
            daysPerWeek = 4,
            duration = 45,
            preferredTime = WorkoutTime.EVENING
        )

        // Assert
        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.workoutLocation).isEqualTo(WorkoutLocation.HOME)
        assertThat(profile.availableEquipment).containsExactlyElementsIn(equipment)
        assertThat(profile.workoutDaysPerWeek).isEqualTo(4)
        assertThat(profile.workoutDuration).isEqualTo(45)
        assertThat(profile.preferredWorkoutTime).isEqualTo(WorkoutTime.EVENING)
    }

    @Test
    fun `updateLimitations sets injuries and workout type`() {
        // Arrange
        val injuries = listOf("Lower back", "Right knee")

        // Act
        viewModel.updateLimitations(injuries, WorkoutType.YOGA)

        // Assert
        val profile = viewModel.onboardingState.value.userProfile
        assertThat(profile.injuries).containsExactlyElementsIn(injuries)
        assertThat(profile.workoutType).isEqualTo(WorkoutType.YOGA)
    }

    @Test
    fun `saveUserProfile on success sets isCompleted and invokes callback`() = runTest(testDispatcher) {
        // Arrange
        coEvery { userRepository.saveUser(any()) } returns 42L
        var callbackInvoked = false

        // Act
        viewModel.saveUserProfile(onSuccess = { callbackInvoked = true })
        advanceUntilIdle()

        // Assert
        val state = viewModel.onboardingState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isCompleted).isTrue()
        assertThat(state.error).isNull()
        assertThat(callbackInvoked).isTrue()
        coVerify { userRepository.saveUser(any()) }
    }

    @Test
    fun `the generated plan is stored against the saved user`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns null
        coEvery { userRepository.saveUser(any()) } returns 42L
        val plan = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveWeeklyWorkoutPlan(capture(plan)) } returns 1L

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        with(plan.captured) {
            assertThat(userId).isEqualTo(42L)
            assertThat(weekNumber).isEqualTo(1)
            assertThat(startDateMillis).isNotNull()
        }
    }

    // A plan that could not be written is said out loud. It used to be replaced
    // by the built-in week, which told the client their coach had written them
    // a plan when it had not.
    @Test
    fun `a failed generation writes nothing and reports why`() = runTest(testDispatcher) {
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Offline
        var done = false

        viewModel.saveUserProfile(onSuccess = { done = true })
        advanceUntilIdle()

        with(viewModel.onboardingState.value) {
            assertThat(generationFailure).isEqualTo(PlanGenerationResult.Offline)
            assertThat(isCompleted).isFalse()
            assertThat(isLoading).isFalse()
        }
        assertThat(done).isFalse()
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
    }

    // Saving the user first would replace the stored one, and that REPLACE
    // cascades every stored week away: a regeneration that failed destroyed the
    // history it was meant to build on.
    @Test
    fun `a failed regeneration leaves the stored plan alone`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 7)
        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Failed

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.saveUser(any()) }
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    @Test
    fun `a generated plan is saved instead of the sample fallback`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns null
        coEvery { userRepository.saveUser(any()) } returns 42L
        val generated = WeeklyWorkoutPlan(
            userId = 42L, weekNumber = 1, title = "Generated week",
            startDateMillis = 1L, workoutDays = emptyList()
        )
        val request = slot<PlanRequest>()
        coEvery { planGenerator.generate(capture(request)) } returns
            PlanGenerationResult.Generated(generated)
        val saved = slot<WeeklyWorkoutPlan>()
        coEvery { userRepository.saveWeeklyWorkoutPlan(capture(saved)) } returns 1L

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        assertThat(saved.captured).isEqualTo(generated)
        with(request.captured) {
            // Generation runs before the user row exists, so the request
            // carries the id it will be saved under: none, for a new client.
            assertThat(user.id).isEqualTo(0L)
            assertThat(weekNumber).isEqualTo(1)
            assertThat(languageCode).isEqualTo("en")
            assertThat(previousWeek).isNull()
        }
    }

    @Test
    fun `a stored profile is loaded so editing starts from saved answers`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 7L, firstName = "Jeco", age = 26)
        val loaded = OnboardingViewModel(userRepository, planGenerator) { "en" }

        advanceUntilIdle()

        assertThat(loaded.onboardingState.value.userProfile.firstName).isEqualTo("Jeco")
        assertThat(loaded.onboardingState.value.userProfile.age).isEqualTo(26)
    }

    @Test
    fun `saveUserProfile replaces the existing user rather than adding another`() = runTest(testDispatcher) {
        coEvery { userRepository.getCurrentUser() } returns UserProfile(id = 7L, firstName = "Old")
        coEvery { userRepository.saveUser(any()) } returns 7L

        viewModel.saveUserProfile(onSuccess = {})
        advanceUntilIdle()

        coVerify { userRepository.saveUser(match { it.id == 7L }) }
    }

    @Test
    fun `saveUserProfile on repository failure surfaces error and does not complete`() = runTest(testDispatcher) {
        // Arrange
        coEvery { userRepository.saveUser(any()) } throws RuntimeException("DB write failed")
        var callbackInvoked = false

        // Act
        viewModel.saveUserProfile(onSuccess = { callbackInvoked = true })
        advanceUntilIdle()

        // Assert
        val state = viewModel.onboardingState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isCompleted).isFalse()
        assertThat(state.error).isEqualTo("DB write failed")
        assertThat(callbackInvoked).isFalse()
    }
    // The regenerate flow deliberately wipes history; editing the profile must
    // not, so it updates the stored user rather than re-inserting it.
    @Test
    fun `updateProfileOnly saves the profile and leaves the plan alone`() = runTest {
        val stored = UserProfile(id = 4, firstName = "Jet", age = 28)
        coEvery { userRepository.getCurrentUser() } returns stored
        val viewModel = OnboardingViewModel(userRepository, planGenerator) { "en" }
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
        coVerify(exactly = 0) { userRepository.saveWeeklyWorkoutPlan(any()) }
        coVerify(exactly = 0) { planGenerator.generate(any()) }
        assertThat(done).isTrue()
        assertThat(viewModel.onboardingState.value.isLoading).isFalse()
    }

    // Anchoring week one to the Monday just gone handed anyone who signed up
    // later in the week a plan of sessions that had already been missed.
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

    // Two taps on Generate used to start two runs, and for a client with no
    // stored user that meant two of everything.
    @Test
    fun `a second tap while generating is ignored`() = runTest(testDispatcher) {
        coEvery { userRepository.saveUser(any()) } returns 42L

        viewModel.saveUserProfile()
        viewModel.saveUserProfile()
        advanceUntilIdle()

        coVerify(exactly = 1) { planGenerator.generate(any()) }
        coVerify(exactly = 1) { userRepository.saveWeeklyWorkoutPlan(any()) }
    }

    // The view model outlives the screen, so a regeneration must not start out
    // already complete from the run before it.
    @Test
    fun `regenerating starts from not complete`() = runTest(testDispatcher) {
        coEvery { userRepository.saveUser(any()) } returns 42L
        viewModel.saveUserProfile()
        advanceUntilIdle()
        assertThat(viewModel.onboardingState.value.isCompleted).isTrue()

        coEvery { planGenerator.generate(any()) } returns PlanGenerationResult.Offline
        viewModel.saveUserProfile()
        advanceUntilIdle()

        assertThat(viewModel.onboardingState.value.isCompleted).isFalse()
    }

}
