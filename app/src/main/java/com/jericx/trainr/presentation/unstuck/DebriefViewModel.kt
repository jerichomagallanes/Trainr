package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DebriefViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository
) : ViewModel() {

    private val dayNumber: Int? =
        savedStateHandle.get<Int>(Screen.Debrief.ARG_DAY_NUMBER)?.takeIf { it > 0 }

    private val weekNumber: Int? =
        savedStateHandle.get<Int>(Screen.Debrief.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _savedEvents = Channel<Long>(Channel.BUFFERED)
    val savedEvents: Flow<Long> = _savedEvents.receiveAsFlow()

    private var user: UserProfile? = null
    private var dayId: Long? = null
    private var existingNote: SessionNote? = null
    private var isSaving = false

    init {
        viewModelScope.launch { load() }
    }

    fun typeNote(text: String) {
        _note.update { text }
    }

    fun save() {
        val text = _note.value.trim()
        val profile = user
        val day = dayId
        if (text.isEmpty() || profile == null || day == null || isSaving) return
        isSaving = true

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = existingNote
            if (existing == null) {
                val id = adjustmentRepository.saveNote(
                    SessionNote(
                        userId = profile.id,
                        workoutDayId = day,
                        text = text,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                existingNote = SessionNote(
                    id = id,
                    userId = profile.id,
                    workoutDayId = day,
                    text = text,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                val updated = existing.copy(text = text, updatedAt = now)
                adjustmentRepository.updateNote(updated)
                existingNote = updated
            }
            isSaving = false
            _savedEvents.send(day)
        }
    }

    private suspend fun load() {
        val number = dayNumber ?: return
        val profile = userRepository.getCurrentUser() ?: return
        user = profile

        val outline = userRepository.getWeekOutline(profile.id, weekNumber) ?: return
        val day = outline.days.firstOrNull { it.dayNumber == number }?.id ?: return
        dayId = day

        val existing = adjustmentRepository.getNote(day)
        existingNote = existing
        existing?.let { saved -> _note.update { saved.text } }
    }
}
