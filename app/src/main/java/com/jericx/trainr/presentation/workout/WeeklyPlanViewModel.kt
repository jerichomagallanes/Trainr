package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.PreferenceKind
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.TrainingPreference
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.unstuck.TodayAdjustmentKind
import com.jericx.trainr.presentation.workout.model.derivedEquipment
import com.jericx.trainr.presentation.workout.model.derivedExerciseCount
import com.jericx.trainr.presentation.workout.model.isAdjustedToday
import com.jericx.trainr.presentation.workout.model.remainingMinutes
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
    val isPast: Boolean = false,
    val finishKind: FinishKind? = null,
    // Derived from the sets that remain: an adjusted day's stored duration,
    // exercise count and equipment still describe the plan as generated.
    val minutes: Int = day.duration,
    val exerciseCount: Int = day.exerciseCount,
    val equipment: List<String> = day.equipment
) {
    // Derived, not stored: a session moved to a later day stops being missed
    // on its own, with no flag to correct.
    val isMissed: Boolean get() = isPast && day.status != WorkoutStatus.COMPLETED

    val isFrozen: Boolean get() = isPast || day.status == WorkoutStatus.COMPLETED
}

data class WeeklyPlanUiState(
    val plan: WeeklyWorkoutPlan = SampleWorkoutData.weekOne,
    val days: List<WeeklyPlanDay> = emptyList(),
    val weekStartMillis: Long = SampleWorkoutData.weekStartMillis,
    val weekEndMillis: Long = SampleWorkoutData.weekEndMillis,
    // Keeps an empty plan and a plan not read yet from being mistaken for one another.
    val hasLoaded: Boolean = false,
    val hasPlan: Boolean = false,
    // A property of the week itself, not of the door it was opened through.
    val isCurrentWeek: Boolean = false,
    // Dates running out counts as finished, so a missed day cannot strand the plan.
    val canStartNextWeek: Boolean = false,
    val canAddWeek: Boolean = false,
    val todayAdjustment: TodayAdjustmentKind? = null,
    val todayPreference: TrainingPreference? = null,
    val hasMemory: Boolean = false
) {
    // Never a completed day, so nothing finished can be offered as the next
    // session; null once the week is done, which leads to the next week.
    val nextWorkout: WeeklyPlanDay?
        get() = days.firstOrNull { !it.isPast && it.day.status != WorkoutStatus.COMPLETED }
            ?: days.firstOrNull { it.day.status != WorkoutStatus.COMPLETED }

    val nextWorkoutIsToday: Boolean get() = nextWorkout?.isToday == true
}

@HiltViewModel
class WeeklyPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository,
    private val catalog: ExerciseCatalog
) : ViewModel() {

    // Absent on home, which shows the newest week; set when opened from Weekly Progress.
    private val requestedWeekNumber: Int? =
        savedStateHandle.get<Int>(Screen.WeekPlan.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(WeeklyPlanUiState())
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    private var outcomes: Map<Long, SessionOutcome> = emptyMap()
    private var user: UserProfile? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            user = userRepository.getCurrentUser()
            val plans = user
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
                outcomes = adjustmentRepository.getOutcomes(stored.workoutDays.map { it.id })
                    .associateBy { it.workoutDayId }
                stateFor(
                    plan = stored,
                    isCurrentWeek = stored.weekNumber == newest?.weekNumber,
                    // Read off the newest week, not the one being looked at: an old
                    // week is always finished and says nothing about the plan.
                    canAddWeek = newest?.isReadyForTheNextWeek() ?: false,
                    outcomes = outcomes,
                    user = user,
                    catalog = catalog
                ).withMemory(user)
            }
        }
    }

    // Only ever about today, and only while today is still to be trained: a
    // card about a session that is over has nothing to offer.
    private suspend fun WeeklyPlanUiState.withMemory(
        profile: UserProfile?
    ): WeeklyPlanUiState {
        if (profile == null || !isCurrentWeek) return this

        val hasMemory = adjustmentRepository.getPreferences(profile.id).isNotEmpty() ||
            adjustmentRepository.getNotes(profile.id).isNotEmpty()
        val today = days.firstOrNull { it.isToday && !it.isFrozen && outcomes[it.day.id] == null }
            ?: return copy(hasMemory = hasMemory)

        val adjustment = when (adjustmentRepository.getActiveAdjustment(today.day.id)?.reason) {
            AdjustmentReason.LESS_TIME -> TodayAdjustmentKind.SHORTER
            AdjustmentReason.EQUIPMENT_UNAVAILABLE -> TodayAdjustmentKind.ALTERNATIVE
            null -> null
        }

        return copy(
            hasMemory = hasMemory,
            todayAdjustment = adjustment,
            todayPreference = if (adjustment == null) {
                adjustmentRepository.getPreference(
                    userId = profile.id,
                    kind = PreferenceKind.TIME_LIMIT,
                    weekday = WorkoutWeek.isoWeekdayOf(today.dateMillis)
                )
            } else {
                null
            }
        )
    }

    // Sessions swap; the weekday slots themselves never move.
    fun moveDay(from: Int, to: Int) {
        val state = _uiState.value
        if (!state.hasPlan) return

        val plan = state.plan
        val reordered = reorderedDays(plan.workoutDays, from, to)
        if (reordered == plan.workoutDays) return

        _uiState.value = stateFor(
            plan = plan.copy(workoutDays = reordered.sortedBy { it.dayNumber }),
            canAddWeek = state.canAddWeek,
            isCurrentWeek = state.isCurrentWeek,
            outcomes = outcomes,
            user = user,
            catalog = catalog
        ).copy(hasMemory = state.hasMemory)

        viewModelScope.launch {
            val before = plan.workoutDays.associateBy { it.id }
            reordered
                .filter { before[it.id]?.dayNumber != it.dayNumber }
                .forEach { userRepository.updateWorkoutDay(it, plan.id) }
            // The cards describe the session sitting in today's slot, which the
            // drag may have changed.
            _uiState.value = _uiState.value.withMemory(user)
        }
    }

    companion object {
        private const val LAST_ISO_DAY = 7

        // A finished session stays put, and nothing may be dragged across it.
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
            isCurrentWeek: Boolean = true,
            // A fact about the newest week, whatever week is being read: appending
            // while one is still being trained would move home onto the copy.
            canAddWeek: Boolean? = null,
            nowMillis: Long = System.currentTimeMillis(),
            outcomes: Map<Long, SessionOutcome> = emptyMap(),
            user: UserProfile? = null,
            catalog: ExerciseCatalog? = null
        ): WeeklyPlanUiState {
            val start = plan.startDateMillis ?: SampleWorkoutData.weekStartMillis
            val readyForTheNext = plan.isReadyForTheNextWeek(nowMillis)

            val today = WorkoutWeek.startOfDay(nowMillis)

            return WeeklyPlanUiState(
                plan = plan,
                days = plan.workoutDays.map {
                    val date = WorkoutWeek.dateOfDay(start, it.dayNumber)
                    val adjusted = it.isAdjustedToday
                    WeeklyPlanDay(
                        day = it,
                        dateMillis = date,
                        isToday = WorkoutWeek.startOfDay(date) == today,
                        isPast = WorkoutWeek.startOfDay(date) < today,
                        finishKind = outcomes[it.id]?.finishKind,
                        minutes = if (adjusted && user != null && catalog != null) {
                            it.remainingMinutes(user, catalog)
                        } else {
                            it.duration
                        },
                        exerciseCount = it.derivedExerciseCount(),
                        equipment = catalog?.let { entries -> it.derivedEquipment(entries) }
                            ?: it.equipment
                    )
                },
                weekStartMillis = start,
                weekEndMillis = WorkoutWeek.dateOfDay(start, LAST_ISO_DAY),
                hasLoaded = true,
                hasPlan = true,
                isCurrentWeek = isCurrentWeek,
                canStartNextWeek = readyForTheNext,
                canAddWeek = canAddWeek ?: readyForTheNext
            )
        }
    }
}
