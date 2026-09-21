package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.TrainingPreference
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TodayAdjustmentKind { SHORTER, ALTERNATIVE }

data class PreferenceCardUi(
    val id: Long,
    val weekdayName: String,
    val minutes: Int,
    val confirmedOn: String
)

data class PreferencesUiState(
    val isLoaded: Boolean = false,
    val preferences: List<PreferenceCardUi> = emptyList(),
    val notes: List<SessionNote> = emptyList(),
    val todayAdjustment: TodayAdjustmentKind? = null,
    val hasForgotten: Boolean = false
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun forget(id: Long) {
        viewModelScope.launch {
            adjustmentRepository.deletePreference(id)
            _uiState.update { it.copy(hasForgotten = true) }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { adjustmentRepository.deleteNote(id) }
    }

    private suspend fun load() {
        val profile = userRepository.getCurrentUser()
        if (profile == null) {
            _uiState.update { it.copy(isLoaded = true) }
            return
        }

        val locale = Locale.getDefault()
        _uiState.update { it.copy(todayAdjustment = todayAdjustment(profile.id)) }

        combine(
            adjustmentRepository.observePreferences(profile.id),
            adjustmentRepository.observeNotes(profile.id)
        ) { preferences, notes -> preferences to notes }
            .collect { (preferences, notes) ->
                _uiState.update { state ->
                    state.copy(
                        isLoaded = true,
                        preferences = preferences.map { cardFor(it, locale) },
                        notes = notes
                    )
                }
            }
    }

    private fun cardFor(preference: TrainingPreference, locale: Locale) = PreferenceCardUi(
        id = preference.id,
        weekdayName = WorkoutDateFormatter.formatWeekdayName(preference.weekday, locale),
        minutes = preference.minutes,
        confirmedOn = WorkoutDateFormatter.formatMediumDate(preference.confirmedAt, locale)
    )

    private suspend fun todayAdjustment(userId: Long): TodayAdjustmentKind? {
        val plan = userRepository.getWeeklyWorkoutPlans(userId).first()
            .maxByOrNull { it.weekNumber } ?: return null
        val start = plan.startDateMillis ?: return null
        val today = WorkoutWeek.startOfDay()

        val day = plan.workoutDays.firstOrNull {
            WorkoutWeek.startOfDay(WorkoutWeek.dateOfDay(start, it.dayNumber)) == today
        } ?: return null

        if (adjustmentRepository.getOutcome(day.id) != null) return null

        return when (adjustmentRepository.getActiveAdjustment(day.id)?.reason) {
            AdjustmentReason.LESS_TIME -> TodayAdjustmentKind.SHORTER
            AdjustmentReason.EQUIPMENT_UNAVAILABLE -> TodayAdjustmentKind.ALTERNATIVE
            null -> null
        }
    }
}
