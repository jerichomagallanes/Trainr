package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.WeekProgressUi
import com.jericx.trainr.presentation.workout.model.WeekStatus
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyProgressUiState(
    val weeks: List<WeekProgressUi> = emptyList(),
    // An empty list means "none stored" only once the reading is done, and an
    // empty list sends the screen away.
    val hasLoaded: Boolean = false
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

            _uiState.value = WeeklyProgressUiState(
                weeks = plans.map { weekProgressOf(it) },
                hasLoaded = true
            )
        }
    }

    fun deleteWeek(weekNumber: Int) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val plans = userRepository.getWeeklyWorkoutPlans(user.id).first()
            val plan = plans.firstOrNull { it.weekNumber == weekNumber } ?: return@launch

            userRepository.deleteWeeklyWorkoutPlan(plan.id)
            renumber(plans - plan)
            refresh()
        }
    }

    // Week numbers are the plan's running order, not a record: the dates are.
    // Ascending, since two weeks cannot hold the same number at once.
    private suspend fun renumber(remaining: List<WeeklyWorkoutPlan>) {
        remaining
            .sortedBy { it.weekNumber }
            .forEachIndexed { index, plan ->
                val number = index + 1
                if (plan.weekNumber != number) {
                    userRepository.updateWeeklyWorkoutPlan(plan.copy(weekNumber = number))
                }
            }
    }

    companion object {
        private const val LAST_ISO_DAY = 7

        fun weekProgressOf(
            plan: WeeklyWorkoutPlan,
            nowMillis: Long = System.currentTimeMillis()
        ): WeekProgressUi {
            val start = plan.startDateMillis ?: WorkoutWeek.startOfDay(plan.createdAt)
            val completed = plan.workoutDays.count { it.status == WorkoutStatus.COMPLETED }
            val total = plan.workoutDays.size
            // Over once the day after the week has arrived, not before.
            val over = nowMillis >= WorkoutWeek.dateOfDay(start, LAST_ISO_DAY + 1)
            val status = when {
                total > 0 && completed == total -> WeekStatus.COMPLETED
                over && completed == 0 -> WeekStatus.SKIPPED
                over -> WeekStatus.NOT_COMPLETED
                // Checked before UPCOMING: work logged early is not "upcoming".
                completed > 0 -> WeekStatus.IN_PROGRESS
                nowMillis < start -> WeekStatus.UPCOMING
                else -> WeekStatus.IN_PROGRESS
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
