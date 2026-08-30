package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.presentation.workout.util.isReadyForTheNextWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyPlanDay(
    val day: WorkoutDay,
    val dateMillis: Long,
    val isToday: Boolean = false,
    val isPast: Boolean = false
) {
    // Missed is not a state a day enters, it is what an unfinished day in the
    // past IS. Deriving it means a session moved to a later day stops being
    // missed on its own, with no flag to correct.
    val isMissed: Boolean get() = isPast && day.status != WorkoutStatus.COMPLETED

    // The past is a record: what happened on that date, or what did not.
    val isFrozen: Boolean get() = isPast || day.status == WorkoutStatus.COMPLETED
}

data class WeeklyPlanUiState(
    val plan: WeeklyWorkoutPlan = SampleWorkoutData.weekOne,
    val days: List<WeeklyPlanDay> = emptyList(),
    val weekStartMillis: Long = SampleWorkoutData.weekStartMillis,
    val weekEndMillis: Long = SampleWorkoutData.weekEndMillis,
    // Nothing is drawn until the stored plan has been looked for, so an empty
    // plan and a plan not read yet are never mistaken for one another.
    val hasLoaded: Boolean = false,
    val hasPlan: Boolean = false,
    // The newest week is the one being trained; the ones behind it are records.
    // Which of the two a week is belongs to the week, not to the door it was
    // opened through — the same week was live on home and frozen one tap away.
    val isCurrentWeek: Boolean = false,
    // Next week is offered once this one is finished or its dates have run
    // out; a missed day must not strand the plan on the same week forever.
    val canStartNextWeek: Boolean = false,
    val canAddWeek: Boolean = false
) {
    // Today's session when there is one, otherwise the next one still to come.
    // A day that has already passed is never the target: opening it under a
    // button that says "today's workout" would be a lie, and catching up is a
    // tap on the day itself.
    // Null once every session is done: falling back to the first day handed
    // back a workout already finished and called it the next one. A week with
    // nothing left in it leads to the next week instead.
    val nextWorkout: WeeklyPlanDay?
        get() = days.firstOrNull { !it.isPast && it.day.status != WorkoutStatus.COMPLETED }
            ?: days.firstOrNull { it.day.status != WorkoutStatus.COMPLETED }

    val nextWorkoutIsToday: Boolean get() = nextWorkout?.isToday == true

    val todaysDay: WorkoutDay? get() = nextWorkout?.day
}

@HiltViewModel
class WeeklyPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    // Absent on home, which always shows the newest week; set when one
    // particular week was opened from Weekly Progress.
    private val requestedWeekNumber: Int? =
        savedStateHandle.get<Int>(Screen.WeekPlan.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    // Nothing until the plan has been read: the screen shows a plan, or says
    // there is none, and never a stand-in dressed as either.
    private val _uiState = MutableStateFlow(WeeklyPlanUiState())
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val plans = userRepository.getCurrentUser()
                ?.let { userRepository.getWeeklyWorkoutPlans(it.id).first() }
                .orEmpty()
            val newest = plans.maxByOrNull { it.weekNumber }
            val stored = if (requestedWeekNumber == null) {
                newest
            } else {
                plans.firstOrNull { it.weekNumber == requestedWeekNumber }
            }

            _uiState.value = if (stored == null) {
                WeeklyPlanUiState(hasLoaded = true, hasPlan = false)
            } else {
                stateFor(
                    plan = stored,
                    isCurrentWeek = stored.weekNumber == newest?.weekNumber,
                    // Read off the newest week, not the one being looked at: an
                    // old week is always finished, and that says nothing about
                    // whether the plan is ready for another.
                    canAddWeek = newest?.isReadyForTheNextWeek() ?: false
                )
            }
        }
    }

    // Dragging a session onto another weekday swaps the two around; the slots
    // themselves never move, so the week keeps the shape it was generated with.
    fun moveDay(from: Int, to: Int) {
        val state = _uiState.value
        if (!state.hasPlan) return

        val plan = state.plan
        val reordered = reorderedDays(plan.workoutDays, from, to)
        if (reordered == plan.workoutDays) return

        _uiState.value = stateFor(
            plan = plan.copy(workoutDays = reordered.sortedBy { it.dayNumber }),
            canAddWeek = state.canAddWeek,
            isCurrentWeek = state.isCurrentWeek
        )

        viewModelScope.launch {
            val before = plan.workoutDays.associateBy { it.id }
            reordered
                .filter { before[it.id]?.dayNumber != it.dayNumber }
                .forEach { userRepository.updateWorkoutDay(it, plan.id) }
        }
    }

    companion object {
        private const val LAST_ISO_DAY = 7

        // A finished session is the record of a date it was actually done on,
        // so it stays put and nothing may be dragged across it.
        fun reorderedDays(days: List<WorkoutDay>, from: Int, to: Int): List<WorkoutDay> {
            if (from == to || from !in days.indices || to !in days.indices) return days
            val crossed = if (from < to) from..to else to..from
            if (crossed.any { days[it].status == WorkoutStatus.COMPLETED }) return days

            val slots = days.map { it.dayNumber }
            val moved = days.toMutableList().apply { add(to, removeAt(from)) }
            return moved.mapIndexed { index, day -> day.copy(dayNumber = slots[index]) }
        }

        // Plans stored before startDateMillis existed fall back to the sample week.
        fun stateFor(
            plan: WeeklyWorkoutPlan,
            isSample: Boolean = false,
            isCurrentWeek: Boolean = true,
            // Whether the plan can take another week, which is a fact about the
            // newest week however old the one being read is. Repeating an old
            // week appends to the end like any other week, so it has to wait for
            // the same moment: adding one while a week is still being trained
            // would move home onto the copy and strand the week in progress.
            canAddWeek: Boolean? = null,
            nowMillis: Long = System.currentTimeMillis()
        ): WeeklyPlanUiState {
            val start = plan.startDateMillis ?: SampleWorkoutData.weekStartMillis
            val readyForTheNext = plan.isReadyForTheNextWeek(nowMillis)

            val today = WorkoutWeek.startOfDay(nowMillis)

            return WeeklyPlanUiState(
                plan = plan,
                days = plan.workoutDays.map {
                    val date = WorkoutWeek.dateOfDay(start, it.dayNumber)
                    WeeklyPlanDay(
                        day = it,
                        dateMillis = date,
                        isToday = WorkoutWeek.startOfDay(date) == today,
                        isPast = WorkoutWeek.startOfDay(date) < today
                    )
                },
                weekStartMillis = start,
                weekEndMillis = WorkoutWeek.dateOfDay(start, LAST_ISO_DAY),
                hasLoaded = true,
                hasPlan = !isSample,
                isCurrentWeek = isCurrentWeek,
                canStartNextWeek = !isSample && readyForTheNext,
                canAddWeek = canAddWeek ?: (!isSample && readyForTheNext)
            )
        }
    }
}
