package com.jericx.trainr.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutTime
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.data.preferences.LanguageCodeProvider
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val planGenerator: PlanGenerator,
    private val languageCode: LanguageCodeProvider
) : ViewModel() {

    private val _onboardingState = MutableStateFlow(OnboardingState())
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser()?.let { stored ->
                _onboardingState.value = _onboardingState.value.copy(userProfile = stored)
            }
        }
    }

    fun updateBasicInfo(firstName: String, age: Int, gender: Gender, experience: ExperienceLevel) {
        _onboardingState.value = _onboardingState.value.copy(
            answeredSteps = answeredWith(OnboardingStep.BASIC_INFO),
            userProfile = _onboardingState.value.userProfile.copy(
                firstName = firstName,
                age = age,
                gender = gender,
                experienceLevel = experience
            )
        )
    }

    fun updateBodyMetrics(height: Float, weight: Float, units: UnitSystem) {
        _onboardingState.value = _onboardingState.value.copy(
            answeredSteps = answeredWith(OnboardingStep.BODY_METRICS),
            userProfile = _onboardingState.value.userProfile.copy(
                height = height,
                weight = weight,
                bodyUnitSystem = units
            )
        )
    }

    fun updateFitnessGoal(goal: FitnessGoal, workoutType: WorkoutType) {
        _onboardingState.value = _onboardingState.value.copy(
            answeredSteps = answeredWith(OnboardingStep.GOALS),
            userProfile = _onboardingState.value.userProfile.copy(
                fitnessGoal = goal,
                workoutType = workoutType
            )
        )
    }

    fun updateWorkoutSetup(
        location: WorkoutLocation,
        equipment: List<Equipment>,
        liftingUnits: UnitSystem?,
        daysPerWeek: Int,
        duration: Int,
        preferredTime: WorkoutTime
    ) {
        _onboardingState.value = _onboardingState.value.copy(
            answeredSteps = answeredWith(OnboardingStep.SETUP),
            userProfile = _onboardingState.value.userProfile.copy(
                workoutLocation = location,
                liftingUnitSystem = liftingUnits,
                availableEquipment = equipment,
                workoutDaysPerWeek = daysPerWeek,
                workoutDuration = duration,
                preferredWorkoutTime = preferredTime
            )
        )
    }

    fun updateLimitations(injuries: List<String>) {
        _onboardingState.value = _onboardingState.value.copy(
            answeredSteps = answeredWith(OnboardingStep.LIMITATIONS),
            userProfile = _onboardingState.value.userProfile.copy(injuries = injuries)
        )
    }

    private fun answeredWith(step: OnboardingStep): Set<OnboardingStep> =
        _onboardingState.value.answeredSteps + step

    suspend fun hasCompletedOnboarding(): Boolean = userRepository.hasUsers()

    // Updated in place to leave training history alone: saveUserProfile's
    // REPLACE would cascade every stored week away.
    fun updateProfileOnly(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _onboardingState.value = _onboardingState.value.copy(isLoading = true)
                userRepository.getCurrentUser()?.let { existing ->
                    userRepository.updateUser(
                        _onboardingState.value.userProfile.copy(id = existing.id)
                    )
                }
                _onboardingState.value = _onboardingState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _onboardingState.value = _onboardingState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // This view model outlives the screen, so completion has to be reset or a
    // regeneration begins already "complete" and walks past the wait. It is also
    // the single-flight guard: a second tap must not write a second plan.
    private var isWorking = false

    fun saveUserProfile(onSuccess: () -> Unit = {}) {
        if (isWorking) return
        isWorking = true
        viewModelScope.launch {
            try {
                _onboardingState.value = _onboardingState.value.copy(
                    isLoading = true,
                    isCompleted = false,
                    generationFailure = null
                )
                val existing = userRepository.getCurrentUser()
                val profile = _onboardingState.value.userProfile
                    .let { if (existing == null) it else it.copy(id = existing.id) }
                // The plan starts today. Anchoring it to the Monday just gone
                // would hand a new user a week of sessions already missed.
                val start = WorkoutWeek.startOfDay()

                // Nothing is written until there is a plan: saving the user
                // first REPLACEs it, cascading every existing week away.
                val result = planGenerator.generate(
                    PlanRequest(
                        user = profile,
                        weekNumber = FIRST_WEEK,
                        startDateMillis = start,
                        languageCode = languageCode.current()
                    )
                )

                if (result !is PlanGenerationResult.Generated) {
                    // Keep what they typed, but only for a first profile: an
                    // existing one is already stored and saveUser REPLACEs,
                    // which would cascade a training client's weeks away.
                    if (existing == null) {
                        runCatching { userRepository.saveUser(profile) }
                    }

                    _onboardingState.value = _onboardingState.value.copy(
                        isLoading = false,
                        generationFailure = result as PlanGenerationResult.Failure
                    )
                    return@launch
                }

                val userId = userRepository.saveUser(profile)
                userRepository.saveWeeklyWorkoutPlan(result.plan.copy(userId = userId))
                _onboardingState.value = _onboardingState.value.copy(
                    isLoading = false,
                    isCompleted = true
                )
                onSuccess()
            } catch (e: Exception) {
                _onboardingState.value = _onboardingState.value.copy(
                    isLoading = false,
                    error = e.message,
                    generationFailure = PlanGenerationResult.Failed
                )
            } finally {
                isWorking = false
            }
        }
    }

    companion object {
        private const val FIRST_WEEK = 1
    }
}

// Cannot be read off the profile: every enum field starts on a real value, so
// an untouched profile looks exactly like an answered one.
enum class OnboardingStep {
    BASIC_INFO,
    BODY_METRICS,
    GOALS,
    SETUP,
    LIMITATIONS
}

data class OnboardingState(
    val userProfile: UserProfile = UserProfile(),
    val answeredSteps: Set<OnboardingStep> = emptySet(),
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val generationFailure: PlanGenerationResult.Failure? = null
)