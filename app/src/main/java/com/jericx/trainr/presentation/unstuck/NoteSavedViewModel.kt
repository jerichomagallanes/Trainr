package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NoteSavedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adjustmentRepository: AdjustmentRepository
) : ViewModel() {

    private val dayId: Long = savedStateHandle[Screen.NoteSaved.ARG_DAY_ID] ?: 0L

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    init {
        viewModelScope.launch {
            adjustmentRepository.getNote(dayId)?.let { saved -> _note.update { saved.text } }
        }
    }
}
