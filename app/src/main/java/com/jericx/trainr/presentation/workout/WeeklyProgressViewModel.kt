package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.WeekProgressUi
import com.jericx.trainr.presentation.workout.model.WeekStatus
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyProgressUiState(
    val weeks: List<WeekProgressUi> = SampleWeeklyProgress.weeks,
    val isSampleData: Boolean = true
)

@HiltViewModel
class WeeklyProgressViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyProgressUiState())
    val uiState: StateFlow<WeeklyProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val plans = userRepository.getCurrentUser()
                ?.let { userRepository.getWeeklyWorkoutPlans(it.id).first() }
                .orEmpty()
                .sortedBy { it.weekNumber }

            _uiState.value = if (plans.isEmpty()) {
                WeeklyProgressUiState()
            } else {
                WeeklyProgressUiState(
                    weeks = plans.map { weekProgressOf(it) },
                    isSampleData = false
                )
            }
        }
    }

    // Deleting an upcoming week clears the way to generate it again; the guard
    // is repeated here so nothing but an unstarted week can be dropped.
    fun deleteWeek(weekNumber: Int) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val plan = userRepository.getWeeklyWorkoutPlans(user.id).first()
                .firstOrNull { it.weekNumber == weekNumber } ?: return@launch
            if (!weekProgressOf(plan).canDelete) return@launch

            userRepository.deleteWeeklyWorkoutPlan(plan.id)
            refresh()
        }
    }

    companion object {
        private const val LAST_ISO_DAY = 7

        fun weekProgressOf(
            plan: WeeklyWorkoutPlan,
            nowMillis: Long = System.currentTimeMillis()
        ): WeekProgressUi {
            val start = plan.startDateMillis ?: WorkoutWeek.mondayOf(plan.createdAt)
            val completed = plan.workoutDays.count { it.status == WorkoutStatus.COMPLETED }
            val total = plan.workoutDays.size
            // The week is over once the Monday after it has arrived; until then
            // an unfinished week is still in play, however little got done.
            val over = nowMillis >= WorkoutWeek.dateOfDay(start, LAST_ISO_DAY + 1)
            val status = when {
                total > 0 && completed == total -> WeekStatus.COMPLETED
                nowMillis < start -> WeekStatus.UPCOMING
                !over -> WeekStatus.IN_PROGRESS
                completed == 0 -> WeekStatus.SKIPPED
                else -> WeekStatus.NOT_COMPLETED
            }
            return WeekProgressUi(
                planId = plan.id,
                weekNumber = plan.weekNumber,
                completedDays = completed,
                totalDays = total,
                status = status,
                startDateMillis = start,
                endDateMillis = WorkoutWeek.dateOfDay(start, LAST_ISO_DAY)
            )
        }
    }
}
