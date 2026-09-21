package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.TimePresets
import com.jericx.trainr.domain.unstuck.TrainingPreference
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditPreferenceUiState(
    val isLoaded: Boolean = false,
    val weekdayName: String = "",
    val presets: List<Int> = emptyList(),
    val selectedMinutes: Int? = null,
    val customMinutesText: String = "",
    val minutesError: Boolean = false
) {
    val isPresetSelected: Boolean get() = customMinutesText.isEmpty() && selectedMinutes != null

    val canSave: Boolean get() = selectedMinutes != null && !minutesError
}

@HiltViewModel
class EditPreferenceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository
) : ViewModel() {

    private val preferenceId: Long = savedStateHandle[Screen.EditPreference.ARG_ID] ?: 0L

    private val _uiState = MutableStateFlow(EditPreferenceUiState())
    val uiState: StateFlow<EditPreferenceUiState> = _uiState.asStateFlow()

    private val _savedEvents = Channel<Unit>(Channel.BUFFERED)
    val savedEvents: Flow<Unit> = _savedEvents.receiveAsFlow()

    private var stored: TrainingPreference? = null

    init {
        viewModelScope.launch { load() }
    }

    fun selectMinutes(minutes: Int) {
        _uiState.update {
            it.copy(selectedMinutes = minutes, customMinutesText = "", minutesError = false)
        }
    }

    // The same refusal as the time screen: an unsupported number is never
    // clamped into a different one.
    fun typeMinutes(text: String) {
        val digits = text.filter { it.isDigit() }
        val minutes = digits.toIntOrNull()
        _uiState.update {
            it.copy(
                customMinutesText = digits,
                selectedMinutes = minutes?.takeIf { value -> TimePresets.isSupported(value) },
                minutesError = digits.isNotEmpty() &&
                    (minutes == null || !TimePresets.isSupported(minutes))
            )
        }
    }

    // confirmedAt is when the person agreed to remember this, which editing the
    // value does not repeat.
    fun save() {
        val existing = stored ?: return
        val minutes = _uiState.value.selectedMinutes ?: return

        viewModelScope.launch {
            adjustmentRepository.updatePreference(
                existing.copy(minutes = minutes, updatedAt = System.currentTimeMillis())
            )
            _savedEvents.send(Unit)
        }
    }

    private suspend fun load() {
        val profile = userRepository.getCurrentUser()
        val existing = profile
            ?.let { adjustmentRepository.getPreferences(it.id) }
            ?.firstOrNull { it.id == preferenceId }

        if (profile == null || existing == null) {
            _uiState.update { it.copy(isLoaded = true) }
            return
        }
        stored = existing
        val presets = TimePresets.forPlanned(profile.workoutDuration)
            .filter(TimePresets::isSupported)

        _uiState.update {
            it.copy(
                isLoaded = true,
                weekdayName = WorkoutDateFormatter.formatWeekdayName(
                    existing.weekday,
                    Locale.getDefault()
                ),
                presets = presets,
                selectedMinutes = existing.minutes,
                // A stored limit that is not a preset has to show in the field
                // rather than leave the screen looking unanswered.
                customMinutesText = if (existing.minutes in presets) {
                    ""
                } else {
                    existing.minutes.toString()
                }
            )
        }
    }
}
