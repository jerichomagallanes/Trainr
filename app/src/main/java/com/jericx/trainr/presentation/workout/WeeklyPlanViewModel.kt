package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyPlanDay(
    val day: WorkoutDay,
    val dateMillis: Long
)

data class WeeklyPlanUiState(
    val plan: WeeklyWorkoutPlan = SampleWorkoutData.weekOne,
    val days: List<WeeklyPlanDay> = emptyList(),
    val weekStartMillis: Long = SampleWorkoutData.weekStartMillis,
    val weekEndMillis: Long = SampleWorkoutData.weekEndMillis,
    val isSampleData: Boolean = true
) {
    // "Today's workout" is the first one still outstanding; once the week is
    // done the button falls back to the start of it rather than doing nothing.
    val todaysDay: WorkoutDay?
        get() = plan.workoutDays.firstOrNull { it.status != WorkoutStatus.COMPLETED }
            ?: plan.workoutDays.firstOrNull()
}

@HiltViewModel
class WeeklyPlanViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(stateFor(SampleWorkoutData.weekOne, isSample = true))
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val stored = userRepository.getCurrentUser()
                ?.let { userRepository.getWeeklyWorkoutPlan(it.id, FIRST_WEEK) }

            _uiState.value = if (stored == null) {
                stateFor(SampleWorkoutData.weekOne, isSample = true)
            } else {
                stateFor(stored, isSample = false)
            }
        }
    }

    companion object {
        private const val FIRST_WEEK = 1
        private const val LAST_ISO_DAY = 7

        // Plans stored before startDateMillis existed fall back to the sample week.
        fun stateFor(plan: WeeklyWorkoutPlan, isSample: Boolean): WeeklyPlanUiState {
            val start = plan.startDateMillis ?: SampleWorkoutData.weekStartMillis

            return WeeklyPlanUiState(
                plan = plan,
                days = plan.workoutDays.map {
                    WeeklyPlanDay(day = it, dateMillis = WorkoutWeek.dateOfDay(start, it.dayNumber))
                },
                weekStartMillis = start,
                weekEndMillis = WorkoutWeek.dateOfDay(start, LAST_ISO_DAY),
                isSampleData = isSample
            )
        }
    }
}
