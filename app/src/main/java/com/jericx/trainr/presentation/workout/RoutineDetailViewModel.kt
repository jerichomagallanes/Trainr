package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.presentation.workout.model.ExerciseTimerUi
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.RoutineUi
import com.jericx.trainr.presentation.workout.sample.SampleRoutine
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineDetailUiState(
    val routine: RoutineUi,
    val equipment: List<String>,
    val dateMillis: Long,
    val timer: ExerciseTimerUi? = null
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(sampleState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    fun toggleExercise(position: Int) {
        val state = _uiState.value
        val routine = state.routine.toggleCompleted(position)
        val nowCompleted = routine.exercises.any { it.position == position && it.isCompleted }
        val clearsTimer = nowCompleted && state.timer?.position == position

        if (clearsTimer) cancelTick()
        _uiState.update {
            it.copy(routine = routine, timer = if (clearsTimer) null else it.timer)
        }
    }

    fun completeRoutine() {
        cancelTick()
        _uiState.update { it.copy(routine = it.routine.completeAll(), timer = null) }
    }

    // One timer at a time: starting an exercise replaces whatever was running.
    fun startTimer(exercise: ExerciseUi) {
        cancelTick()
        _uiState.update {
            it.copy(
                timer = ExerciseTimerUi(
                    position = exercise.position,
                    remainingSeconds = exercise.minutes * SECONDS_PER_MINUTE,
                    isRunning = true
                )
            )
        }
        tickJob = viewModelScope.launch { tick() }
    }

    fun pauseTimer() {
        cancelTick()
        _uiState.update { state -> state.copy(timer = state.timer?.copy(isRunning = false)) }
    }

    fun resumeTimer() {
        if (_uiState.value.timer == null) return

        cancelTick()
        _uiState.update { state -> state.copy(timer = state.timer?.copy(isRunning = true)) }
        tickJob = viewModelScope.launch { tick() }
    }

    fun stopTimer() {
        cancelTick()
        _uiState.update { it.copy(timer = null) }
    }

    override fun onCleared() {
        cancelTick()
        super.onCleared()
    }

    private fun cancelTick() {
        tickJob?.cancel()
        tickJob = null
    }

    // Running out of time is what finishes an exercise, so the card turns green
    // and its timer goes away together.
    private suspend fun tick() {
        while (true) {
            delay(TICK_MILLIS)

            val timer = _uiState.value.timer ?: return
            val remaining = timer.remainingSeconds - 1

            if (remaining > 0) {
                _uiState.update { it.copy(timer = timer.copy(remainingSeconds = remaining)) }
            } else {
                _uiState.update {
                    it.copy(routine = it.routine.markCompleted(timer.position), timer = null)
                }
                return
            }
        }
    }

    companion object {
        private const val TICK_MILLIS = 1000L
        private const val SECONDS_PER_MINUTE = 60

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
