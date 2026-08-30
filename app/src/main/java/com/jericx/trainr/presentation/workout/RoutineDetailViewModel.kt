package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.model.ExerciseTimerUi
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.RoutineUi
import com.jericx.trainr.presentation.workout.model.toRoutineUi
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineDetailUiState(
    val routine: RoutineUi,
    val equipment: List<String>,
    val dateMillis: Long,
    val timer: ExerciseTimerUi? = null,
    val expandedVideos: Set<Int> = emptySet(),
    val playingVideo: Int? = null,
    val dayNumber: Int = 1,
    val weekNumber: Int = 1,
    val completesTheWeek: Boolean = false,
    // False only while the stored routine may still replace the sample one;
    // the completion guard must not treat that swap as finishing the day.
    val isLoaded: Boolean = true
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    private val requestedDayNumber: Int =
        savedStateHandle[Screen.RoutineDetail.ARG_DAY_NUMBER]
            ?: SampleWorkoutData.DEFAULT_DAY_NUMBER

    private val _uiState = MutableStateFlow(
        sampleState(requestedDayNumber).copy(isLoaded = false)
    )
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    // Non-null once the routine came from storage; the sample fallback keeps
    // it null so nothing tries to persist rows that do not exist.
    private var storedDay: WorkoutDay? = null
    private var weeklyPlanId = 0L

    init {
        viewModelScope.launch {
            // Days open out of the plan the home surface shows, which is
            // always the newest stored week.
            val plan = userRepository.getCurrentUser()
                ?.let { user ->
                    userRepository.getWeeklyWorkoutPlans(user.id).first()
                        .maxByOrNull { it.weekNumber }
                }
            val index = plan?.workoutDays
                ?.indexOfFirst { it.dayNumber == requestedDayNumber } ?: -1

            if (plan == null || index < 0) {
                _uiState.update { it.copy(isLoaded = true) }
            } else {
                val day = plan.workoutDays[index]
                storedDay = day
                weeklyPlanId = plan.id
                // History stops at this day's own completion, so a finished day
                // reviewed later still shows what "previous" meant at the time.
                val before = day.completedAt ?: Long.MAX_VALUE
                val previousByKey = day.exercises
                    .filter { it.exerciseKey.isNotBlank() }
                    .associate {
                        it.exerciseKey to userRepository.getPreviousSets(
                            plan.userId, it.exerciseKey, day.id, before
                        )
                    }
                    .filterValues { sets -> sets.isNotEmpty() }
                _uiState.value = RoutineDetailUiState(
                    routine = day.toRoutineUi(previousByKey),
                    equipment = day.equipment,
                    dateMillis = plan.startDateMillis
                        ?.let { WorkoutWeek.dateOfDay(it, day.dayNumber) }
                        ?: SampleWorkoutData.dateOf(day.dayNumber),
                    dayNumber = index + 1,
                    weekNumber = plan.weekNumber,
                    completesTheWeek = completesTheWeek(plan.workoutDays, index + 1)
                )
            }
        }
    }

    fun toggleExercise(position: Int) {
        val state = _uiState.value
        val routine = state.routine.toggleCompleted(position)
        val nowCompleted = routine.exercises.any { it.position == position && it.isCompleted }
        val clearsTimer = nowCompleted && state.timer?.position == position

        if (clearsTimer) cancelTick()
        _uiState.update {
            it.copy(routine = routine, timer = if (clearsTimer) null else it.timer)
        }
        persistExerciseCompleted(position, nowCompleted)
    }

    fun updateSet(position: Int, set: ExerciseSet) {
        _uiState.update { it.copy(routine = it.routine.updateSet(position, set)) }

        val exercise = storedExerciseAt(position) ?: return
        if (set.id == 0L) return
        viewModelScope.launch { userRepository.updateExerciseSet(set, exercise.id) }
    }

    fun addSet(position: Int) {
        _uiState.update { it.copy(routine = it.routine.addSet(position)) }

        val exercise = storedExerciseAt(position) ?: return
        val added = _uiState.value.routine.exercises
            .first { it.position == position }.sets.last()
        viewModelScope.launch {
            val id = userRepository.addExerciseSet(added, exercise.id)
            // Patch only the id: the user may already be typing into the row.
            _uiState.update { state ->
                val current = state.routine.exercises.first { it.position == position }
                    .sets.first { it.setNumber == added.setNumber }
                state.copy(routine = state.routine.updateSet(position, current.copy(id = id)))
            }
        }
    }

    // Deletion is keyed by set number, not instance: the row that reports the
    // swipe may hold a set from before a reload replaced every instance.
    fun deleteSet(position: Int, setNumber: Int) {
        val sets = _uiState.value.routine.exercises
            .firstOrNull { it.position == position }?.sets ?: return
        val set = sets.firstOrNull { it.setNumber == setNumber } ?: return
        if (sets.size <= 1) return

        _uiState.update { it.copy(routine = it.routine.removeSet(position, setNumber)) }

        val exercise = storedExerciseAt(position) ?: return
        if (set.id == 0L) return
        viewModelScope.launch {
            userRepository.deleteExerciseSet(set.id)
            // The renumbered rows are written back, so order survives a reload.
            _uiState.value.routine.exercises.first { it.position == position }
                .sets.filter { it.id != 0L }
                .forEach { userRepository.updateExerciseSet(it, exercise.id) }
        }
    }

    fun completeRoutine() {
        cancelTick()
        _uiState.update { it.copy(routine = it.routine.completeAll(), timer = null) }

        val day = storedDay ?: return
        val completed = day.copy(exercises = day.exercises.map { it.copy(isCompleted = true) })
        storedDay = completed
        viewModelScope.launch {
            completed.exercises.forEach { userRepository.updateWorkoutExercise(it, day.id) }
            persistDayStatus()
        }
    }

    // One timer at a time: starting an exercise replaces whatever was running.
    fun startTimer(exercise: ExerciseUi) {
        cancelTick()
        _uiState.update {
            it.copy(
                timer = ExerciseTimerUi(
                    position = exercise.position,
                    remainingSeconds = exercise.minutes * SECONDS_PER_MINUTE,
                    isRunning = true,
                    totalSeconds = exercise.minutes * SECONDS_PER_MINUTE
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

    // Back to the top of the interval, held there: resetting is preparing to go
    // again, not going again.
    fun resetTimer() {
        cancelTick()
        _uiState.update { state ->
            state.copy(
                timer = state.timer?.let {
                    it.copy(remainingSeconds = it.totalSeconds, isRunning = false)
                }
            )
        }
    }

    fun toggleVideo(position: Int) {
        _uiState.update { state ->
            val collapsing = position in state.expandedVideos

            state.copy(
                expandedVideos = if (collapsing) state.expandedVideos - position
                else state.expandedVideos + position,
                playingVideo = if (collapsing && state.playingVideo == position) {
                    null
                } else {
                    state.playingVideo
                }
            )
        }
    }

    // One WebView at a time: playing a tutorial stops whichever was open.
    fun playVideo(position: Int) {
        _uiState.update { it.copy(playingVideo = position) }
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
                persistExerciseCompleted(timer.position, completed = true)
                return
            }
        }
    }

    private fun storedExerciseAt(position: Int): WorkoutExercise? =
        storedDay?.exercises?.getOrNull(position - 1)

    private fun persistExerciseCompleted(position: Int, completed: Boolean) {
        val day = storedDay ?: return
        val exercise = storedExerciseAt(position)?.copy(isCompleted = completed) ?: return

        storedDay = day.copy(
            exercises = day.exercises.mapIndexed { index, e ->
                if (index == position - 1) exercise else e
            }
        )
        viewModelScope.launch {
            userRepository.updateWorkoutExercise(exercise, day.id)
            persistDayStatus()
        }
    }

    private suspend fun persistDayStatus() {
        val day = storedDay ?: return
        val completedCount = day.exercises.count { it.isCompleted }
        val status = when (completedCount) {
            0 -> WorkoutStatus.NOT_STARTED
            day.exercises.size -> WorkoutStatus.COMPLETED
            else -> WorkoutStatus.IN_PROGRESS
        }
        val updated = day.copy(
            status = status,
            completedAt = if (status == WorkoutStatus.COMPLETED) {
                day.completedAt ?: System.currentTimeMillis()
            } else {
                null
            }
        )

        storedDay = updated
        userRepository.updateWorkoutDay(updated, weeklyPlanId)
    }

    companion object {
        private const val TICK_MILLIS = 1000L
        private const val SECONDS_PER_MINUTE = 60
        private const val FIRST_WEEK = 1

        fun sampleState(dayNumber: Int = SampleWorkoutData.DEFAULT_DAY_NUMBER): RoutineDetailUiState {
            val days = SampleWorkoutData.weekOne.workoutDays
            val index = days.indexOfFirst { it.dayNumber == dayNumber }.coerceAtLeast(0)
            val day = days[index]

            return RoutineDetailUiState(
                routine = day.toRoutineUi(),
                equipment = day.equipment,
                dateMillis = SampleWorkoutData.dateOf(day.dayNumber),
                // "Day 2", not day 3: the design counts workout days, not weekdays.
                dayNumber = index + 1,
                weekNumber = SampleWorkoutData.weekOne.weekNumber,
                completesTheWeek = completesTheWeek(days, index + 1)
            )
        }

        // Finishing the last outstanding day of the week ends the week, not just
        // the day — so the routine has to know which of the two it is.
        fun completesTheWeek(days: List<WorkoutDay>, dayNumber: Int): Boolean =
            days.filterIndexed { index, _ -> index != dayNumber - 1 }
                .all { it.status == WorkoutStatus.COMPLETED }
    }
}
