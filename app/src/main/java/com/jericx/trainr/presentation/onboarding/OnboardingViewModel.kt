package com.jericx.trainr.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.*
import com.jericx.trainr.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _onboardingState = MutableStateFlow(OnboardingState())
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    fun updateBasicInfo(age: Int, gender: Gender, experience: ExperienceLevel) {
        _onboardingState.value = _onboardingState.value.copy(
            userProfile = _onboardingState.value.userProfile.copy(
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

    fun saveUserProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _onboardingState.value = _onboardingState.value.copy(isLoading = true)
                val userId = userRepository.saveUser(_onboardingState.value.userProfile)
                _onboardingState.value = _onboardingState.value.copy(
                    isLoading = false,
                    isCompleted = true
                )
                onSuccess()
            } catch (e: Exception) {
                _onboardingState.value = _onboardingState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

data class OnboardingState(
    val userProfile: UserProfile = UserProfile(),
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCompleted: Boolean = false
)