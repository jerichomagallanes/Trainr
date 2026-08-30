package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val isSampleData: Boolean = true,
    // Next week is offered once this one is finished or its dates have run
    // out; a missed day must not strand the plan on the same week forever.
    val canStartNextWeek: Boolean = false
) {
    // "Today's workout" is the first one still outstanding; once the week is
    // done the button falls back to the start of it rather than doing nothing.
    val todaysDay: WorkoutDay?
        get() = plan.workoutDays.firstOrNull { it.status != WorkoutStatus.COMPLETED }
            ?: plan.workoutDays.firstOrNull()
}

@HiltViewModel
class WeeklyPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    // Absent on home, which always shows the newest week; set when one
    // particular week was opened from Weekly Progress.
    private val requestedWeekNumber: Int? =
        savedStateHandle.get<Int>(Screen.WeekPlan.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(stateFor(SampleWorkoutData.weekOne, isSample = true))
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val stored = userRepository.getCurrentUser()
                ?.let { user ->
                    val plans = userRepository.getWeeklyWorkoutPlans(user.id).first()
                    if (requestedWeekNumber == null) {
                        plans.maxByOrNull { it.weekNumber }
                    } else {
                        plans.firstOrNull { it.weekNumber == requestedWeekNumber }
                    }
                }

            _uiState.value = if (stored == null) {
                stateFor(SampleWorkoutData.weekOne, isSample = true)
            } else {
                stateFor(stored, isSample = false)
            }
        }
    }

    companion object {
        private const val LAST_ISO_DAY = 7

        // Plans stored before startDateMillis existed fall back to the sample week.
        fun stateFor(
            plan: WeeklyWorkoutPlan,
            isSample: Boolean,
            nowMillis: Long = System.currentTimeMillis()
        ): WeeklyPlanUiState {
            val start = plan.startDateMillis ?: SampleWorkoutData.weekStartMillis
            val allDone = plan.workoutDays.isNotEmpty() &&
                plan.workoutDays.all { it.status == WorkoutStatus.COMPLETED }
            val weekIsOver = nowMillis >= WorkoutWeek.dateOfDay(start, LAST_ISO_DAY + 1)

            return WeeklyPlanUiState(
                plan = plan,
                days = plan.workoutDays.map {
                    WeeklyPlanDay(day = it, dateMillis = WorkoutWeek.dateOfDay(start, it.dayNumber))
                },
                weekStartMillis = start,
                weekEndMillis = WorkoutWeek.dateOfDay(start, LAST_ISO_DAY),
                isSampleData = isSample,
                // Sample data stands for nothing stored, so it leads nowhere.
                canStartNextWeek = !isSample && (allDone || weekIsOver)
            )
        }
    }
}
