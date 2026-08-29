package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import com.jericx.trainr.presentation.workout.model.RoutineUi
import com.jericx.trainr.presentation.workout.sample.SampleRoutine
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RoutineDetailUiState(
    val routine: RoutineUi,
    val equipment: List<String>,
    val dateMillis: Long
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(sampleState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    fun toggleExercise(position: Int) {
        _uiState.update { it.copy(routine = it.routine.toggleCompleted(position)) }
    }

    fun completeRoutine() {
        _uiState.update { it.copy(routine = it.routine.completeAll()) }
    }

    companion object {
        // Only the Cardio & Core day has exercises, so the screen shows that one
        // until routines are generated.
        fun sampleState(): RoutineDetailUiState {
            val day = SampleWorkoutData.weekOne.workoutDays
                .first { it.dayNumber == SampleRoutine.DAY_NUMBER }

            return RoutineDetailUiState(
                routine = SampleRoutine.cardioAndCore,
                equipment = day.equipment,
                dateMillis = SampleWorkoutData.dateOf(day.dayNumber)
            )
        }
    }
}
