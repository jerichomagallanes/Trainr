package com.jericx.trainr.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
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

    // A returning user editing or regenerating starts from the profile they
    // saved, not from blank forms.
    init {
        viewModelScope.launch {
            userRepository.getCurrentUser()?.let { stored ->
                _onboardingState.value = _onboardingState.value.copy(userProfile = stored)
            }
        }
    }

    fun updateBasicInfo(firstName: String, age: Int, gender: Gender, experience: ExperienceLevel) {
        _onboardingState.value = _onboardingState.value.copy(
            userProfile = _onboardingState.value.userProfile.copy(
                firstName = firstName,
                age = age,
                gender = gender,
                experienceLevel = experience
            )
        )
    }

    fun updateBodyMetrics(height: Float, weight: Float) {
        _onboardingState.value = _onboardingState.value.copy(
            userProfile = _onboardingState.value.userProfile.copy(
                height = height,
                weight = weight
            )
        )
    }

    fun updateFitnessGoal(goal: FitnessGoal) {
        _onboardingState.value = _onboardingState.value.copy(
            userProfile = _onboardingState.value.userProfile.copy(fitnessGoal = goal)
        )
    }

    fun updateWorkoutSetup(
        location: WorkoutLocation,
        equipment: List<Equipment>,
        daysPerWeek: Int,
        duration: Int,
        preferredTime: WorkoutTime
    ) {
        _onboardingState.value = _onboardingState.value.copy(
            userProfile = _onboardingState.value.userProfile.copy(
                workoutLocation = location,
                availableEquipment = equipment,
                workoutDaysPerWeek = daysPerWeek,
                workoutDuration = duration,
                preferredWorkoutTime = preferredTime
            )
        )
    }

    fun updateLimitations(injuries: List<String>, workoutType: WorkoutType) {
        _onboardingState.value = _onboardingState.value.copy(
            userProfile = _onboardingState.value.userProfile.copy(
                injuries = injuries,
                workoutType = workoutType
            )
        )
    }

    suspend fun hasCompletedOnboarding(): Boolean = userRepository.hasUsers()

    // Editing the profile from the plan must leave training history alone, so
    // the stored user is updated in place: saveUserProfile's REPLACE would
    // cascade every stored week away. The change takes effect on the next week
    // generated, which reads the profile fresh.
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

    // One plan at a time, and completion starts false: this view model outlives
    // the screen, so a regeneration would otherwise begin already "complete"
    // from the run before it and walk straight past the wait.
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

                // Nothing is written until there is a plan to write. Saving the
                // user first would replace the stored one, and that REPLACE
                // cascades every existing week away — so a regeneration that
                // failed used to destroy the history it was meant to build on.
                val result = planGenerator.generate(
                    PlanRequest(
                        user = profile,
                        weekNumber = FIRST_WEEK,
                        startDateMillis = start,
                        languageCode = languageCode.current()
                    )
                )

                if (result !is PlanGenerationResult.Generated) {
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

data class OnboardingState(
    val userProfile: UserProfile = UserProfile(),
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCompleted: Boolean = false,
    // Set when a plan could not be written, so the screen can say why rather
    // than handing over a week nobody asked for.
    val generationFailure: PlanGenerationResult.Failure? = null
)