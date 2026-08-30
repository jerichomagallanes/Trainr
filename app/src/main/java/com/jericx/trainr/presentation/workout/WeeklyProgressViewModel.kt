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
    // No stand-in weeks: the screen lists what is stored, and nothing when
    // nothing is. Showing a built-in set here would read as a training history
    // that never happened.
    val weeks: List<WeekProgressUi> = emptyList(),
    // An empty list means "none stored" only once the reading is done. Before
    // that it means "not looked yet", and the two must not be confused: one of
    // them sends the screen away.
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

    // Any week can go, trained or not, down to the last one: it is the client's
    // record to keep or drop, and a plan emptied out says so and offers to
    // build another rather than pretending one is still there.
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

    // Deleting from the middle would otherwise leave week two missing between
    // one and three. The numbers are the plan's running order, not a record of
    // anything — each week's dates say when it was, and those never move — so
    // closing the gap tells the truth and reads as it should. Renumbered in
    // ascending order, since two of the same number cannot exist at once.
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
            // The week is over once the Monday after it has arrived; until then
            // an unfinished week is still in play, however little got done.
            val over = nowMillis >= WorkoutWeek.dateOfDay(start, LAST_ISO_DAY + 1)
            val status = when {
                total > 0 && completed == total -> WeekStatus.COMPLETED
                over && completed == 0 -> WeekStatus.SKIPPED
                over -> WeekStatus.NOT_COMPLETED
                // Training ahead of schedule still counts as started: a week
                // with work logged in it is not "upcoming" any more.
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
