package com.jericx.trainr.presentation.unstuck.feedback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class FeedbackPromptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository
) : ViewModel() {

    // Every post-save destination names the day and its week with the same
    // arguments.
    private val dayNumber: Int? =
        savedStateHandle.get<Int>(Screen.SessionSaved.ARG_DAY_NUMBER)?.takeIf { it > 0 }

    // A session saved on an earlier week's day belongs to that week, not to the
    // same weekday of the newest one.
    private val weekNumber: Int? =
        savedStateHandle.get<Int>(Screen.SessionSaved.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    private val _pendingAdjustmentId = MutableStateFlow<Long?>(null)
    val pendingAdjustmentId: StateFlow<Long?> = _pendingAdjustmentId.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val day = dayNumber ?: return
        val user = userRepository.getCurrentUser() ?: return
        val plans = userRepository.getWeeklyWorkoutPlans(user.id).first()
        val plan = if (weekNumber == null) {
            plans.maxByOrNull { it.weekNumber }
        } else {
            plans.firstOrNull { it.weekNumber == weekNumber }
        } ?: return
        val stored = plan.workoutDays.firstOrNull { it.dayNumber == day } ?: return

        val adjustment = adjustmentRepository.getActiveAdjustment(stored.id) ?: return
        if (adjustmentRepository.getFeedback(adjustment.id) != null) return

        _pendingAdjustmentId.value = adjustment.id
    }
}
