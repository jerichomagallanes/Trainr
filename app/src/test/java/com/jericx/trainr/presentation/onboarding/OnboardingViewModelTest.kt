package com.jericx.trainr.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutTime
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        viewModel = OnboardingViewModel(userRepository)
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
        assertThat(state.userProfile).isEqualTo(UserProfile())
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
}
