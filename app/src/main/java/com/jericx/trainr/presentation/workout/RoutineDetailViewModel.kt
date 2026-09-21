package com.jericx.trainr.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.UndoResult
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.presentation.workout.model.ExerciseTimerUi
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.AdjustedBannerUi
import com.jericx.trainr.presentation.workout.model.RoutineUi
import com.jericx.trainr.presentation.workout.model.derivedEquipment
import com.jericx.trainr.presentation.workout.model.isAdjustedToday
import com.jericx.trainr.presentation.workout.model.remainingMinutes
import com.jericx.trainr.presentation.workout.model.visibleExercises
import com.jericx.trainr.presentation.workout.model.toRoutineUi
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineDetailUiState(
    val routine: RoutineUi,
    val equipment: List<String>,
    val dateMillis: Long,
    val timer: ExerciseTimerUi? = null,
    // One at a time: each player is a WebView that lives as long as its section is open.
    val expandedVideo: Int? = null,
    val expandedHowTo: Int? = null,
    val dayNumber: Int = 1,
    val weekNumber: Int = 1,
    val completesTheWeek: Boolean = false,
    // Which units the client reads and writes; storage stays metric.
    val unitSystem: UnitSystem = UnitSystem.Default,
    // False until the stored routine has been read: nothing is drawn before then,
    // and the completion guard must not read the load as finishing the day.
    val isLoaded: Boolean = true,
    val outcome: SessionOutcome? = null,
    val isConfirmingFinishEarly: Boolean = false,
    val saveFailed: Boolean = false,
    val activeAdjustment: AppliedAdjustment? = null,
    val adjustedBanner: AdjustedBannerUi? = null,
    val showAdjustSheet: Boolean = false,
    val scrollToPosition: Int? = null,
    val undoKeptSets: Int? = null,
    // Null while the stored day is unadjusted: the header then reads the
    // planned per-exercise minutes as it always has.
    val totalMinutes: Int? = null
)

data class SessionSavedEvent(
    val dayNumber: Int,
    val performedExercises: Int,
    val plannedExercises: Int
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository,
    private val catalog: ExerciseCatalog
) : ViewModel() {

    private val requestedDayNumber: Int =
        savedStateHandle[Screen.RoutineDetail.ARG_DAY_NUMBER]
            ?: SampleWorkoutData.DEFAULT_DAY_NUMBER

    // A day opened from an earlier week must load that week's routine, not the
    // same weekday of the newest one.
    private val requestedWeekNumber: Int? =
        savedStateHandle.get<Int>(Screen.RoutineDetail.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        RoutineDetailUiState(
            routine = RoutineUi(title = "", exercises = emptyList()),
            equipment = emptyList(),
            dateMillis = 0L,
            dayNumber = requestedDayNumber,
            isLoaded = false
        )
    )
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    // Fires once per save and is never rebuilt from stored state, so a screen
    // restored after process death does not navigate a second time.
    private val _savedEvents = Channel<SessionSavedEvent>(Channel.BUFFERED)
    val savedEvents: Flow<SessionSavedEvent> = _savedEvents.receiveAsFlow()

    private var tickJob: Job? = null
    private var finishJob: Job? = null

    // Null until the stored day is read, so nothing persists rows that do not exist.
    private var storedDay: WorkoutDay? = null
    private var weeklyPlanId = 0L

    init {
        refresh()
    }

    // Re-reads the stored day without losing what the screen is doing: the
    // timer, the open tutorial and the scroll request all survive.
    fun refresh() {
        viewModelScope.launch { read() }
    }

    private suspend fun read() {
        val user = userRepository.getCurrentUser()
        val units = user?.weightUnits ?: UnitSystem.Default
        val plan = user?.let { profile ->
            val plans = userRepository.getWeeklyWorkoutPlans(profile.id).first()
            if (requestedWeekNumber == null) {
                plans.maxByOrNull { it.weekNumber }
            } else {
                plans.firstOrNull { it.weekNumber == requestedWeekNumber }
            }
        }
        val index = plan?.workoutDays
            ?.indexOfFirst { it.dayNumber == requestedDayNumber } ?: -1

        if (plan == null || index < 0) {
            _uiState.update { it.copy(isLoaded = true, unitSystem = units) }
            return
        }

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
        val adjustment = adjustmentRepository.getActiveAdjustment(day.id)

        _uiState.update {
            it.copy(
                unitSystem = units,
                routine = day.toRoutineUi(previousByKey, catalog, user?.injuries.orEmpty()),
                equipment = day.derivedEquipment(catalog),
                totalMinutes = user
                    ?.takeIf { day.isAdjustedToday }
                    ?.let { profile -> day.remainingMinutes(profile, catalog) },
                dateMillis = plan.startDateMillis
                    ?.let { start -> WorkoutWeek.dateOfDay(start, day.dayNumber) }
                    ?: SampleWorkoutData.dateOf(day.dayNumber),
                dayNumber = index + 1,
                weekNumber = plan.weekNumber,
                completesTheWeek = completesTheWeek(plan.workoutDays, index + 1),
                outcome = adjustmentRepository.getOutcome(day.id),
                activeAdjustment = adjustment,
                adjustedBanner = adjustment?.let { applied -> bannerFor(applied.proposal) },
                // The note belongs to one undo, not to whatever the day shows next.
                undoKeptSets = null,
                isLoaded = true
            )
        }
    }

    fun openAdjustSheet() {
        _uiState.update { it.copy(showAdjustSheet = true) }
    }

    fun dismissAdjustSheet() {
        _uiState.update { it.copy(showAdjustSheet = false) }
    }

    // "Show me how" is the existing tutorial on the card, not a new screen.
    fun showHowTo(position: Int) {
        val exercise = _uiState.value.routine.exercises
            .firstOrNull { it.position == position } ?: return
        val hasSteps = exercise.steps.isNotEmpty()
        _uiState.update {
            it.copy(
                showAdjustSheet = false,
                expandedHowTo = if (hasSteps) position else it.expandedHowTo,
                expandedVideo = if (hasSteps) it.expandedVideo else position,
                scrollToPosition = position
            )
        }
    }

    fun scrolled() {
        _uiState.update { it.copy(scrollToPosition = null) }
    }

    fun undoAdjustment() {
        val adjustment = _uiState.value.activeAdjustment ?: return

        viewModelScope.launch {
            val result = adjustmentRepository.undo(adjustment.id, System.currentTimeMillis())
            val kept = (result as? UndoResult.Restored)?.keptPerformedSubstituteSets ?: 0
            read()
            _uiState.update { it.copy(undoKeptSets = kept.takeIf { count -> count > 0 }) }
        }
    }

    private fun bannerFor(proposal: AdjustmentProposal): AdjustedBannerUi {
        val replaced = proposal.changes.firstOrNull { it.kind == ChangeKind.REPLACE_UNPERFORMED }
        if (replaced != null) {
            return AdjustedBannerUi(
                messageRes = R.string.adjusted_replaced_banner_format,
                fromName = catalog[replaced.before.catalogKey]?.name.orEmpty(),
                toName = replaced.after?.catalogKey?.let { catalog[it]?.name }.orEmpty()
            )
        }
        val regions = proposal.changes
            .mapNotNull { catalog[it.before.catalogKey]?.primary?.region }
            .distinct()
        val omitted = proposal.changes.any { it.kind == ChangeKind.OMIT_UNPERFORMED }
        return if (omitted || regions.isEmpty()) {
            AdjustedBannerUi(messageRes = R.string.adjusted_reduced_banner)
        } else {
            AdjustedBannerUi(
                messageRes = R.string.adjusted_time_banner_format,
                regions = regions
            )
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
        val was = completionOf(position)
        _uiState.update { it.copy(routine = it.routine.updateSet(position, set)) }
        reconcileCompletion(position, was)

        val exercise = storedExerciseAt(position) ?: return
        // The state holds the origin-stamped copy; the row only knows the numbers.
        val stamped = _uiState.value.routine.exercises.firstOrNull { it.position == position }
            ?.sets?.firstOrNull { it.setNumber == set.setNumber } ?: return
        if (stamped.id == 0L) return
        viewModelScope.launch { userRepository.updateExerciseSet(stamped, exercise.id) }
    }

    fun addSet(position: Int) {
        val was = completionOf(position)
        _uiState.update { it.copy(routine = it.routine.addSet(position)) }
        reconcileCompletion(position, was)

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

        val was = completionOf(position)
        _uiState.update { it.copy(routine = it.routine.removeSet(position, setNumber)) }
        reconcileCompletion(position, was)

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
        val completed = day.copy(
            exercises = day.exercises.map {
                if (it.isOmittedToday) it else it.copy(isCompleted = true)
            }
        )
        storedDay = completed
        viewModelScope.launch {
            completed.visibleExercises.forEach { userRepository.updateWorkoutExercise(it, day.id) }
            persistFilledSets(_uiState.value.routine.exercises.map { it.position })
            persistDayStatus()
            val planned = plannedSetCount()
            val outcome = SessionOutcome(
                workoutDayId = day.id,
                finishKind = FinishKind.FULL,
                finishedAt = System.currentTimeMillis(),
                performedSetCount = planned,
                plannedSetCount = planned
            )
            adjustmentRepository.saveOutcome(outcome)
            _uiState.update { it.copy(outcome = outcome) }
        }
    }

    fun askToFinishEarly() {
        _uiState.update { it.copy(isConfirmingFinishEarly = true, saveFailed = false) }
    }

    fun keepTraining() {
        _uiState.update { it.copy(isConfirmingFinishEarly = false, saveFailed = false) }
    }

    // Saves what was logged and nothing more: no set is filled and no exercise
    // is ticked, so the record reads back as the work actually done.
    fun finishEarly() {
        if (finishJob?.isActive == true) return
        cancelTick()
        _uiState.update { it.copy(timer = null) }

        val day = storedDay ?: return
        finishJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sets = _uiState.value.routine.exercises.flatMap { it.sets }
            val finished = day.copy(
                status = WorkoutStatus.COMPLETED,
                completedAt = day.completedAt ?: now
            )
            val outcome = SessionOutcome(
                workoutDayId = day.id,
                finishKind = FinishKind.PARTIAL,
                finishedAt = now,
                performedSetCount = sets.count { it.isCompleted },
                plannedSetCount = sets.count { it.omittedBy == null }
            )
            val saved = runCatching {
                userRepository.updateWorkoutDay(finished, weeklyPlanId)
                adjustmentRepository.saveOutcome(outcome)
            }
            if (saved.isFailure) {
                // Two writes, no transaction: put the day back so home does not
                // show a completed chip for a session that was never saved.
                runCatching { userRepository.updateWorkoutDay(day, weeklyPlanId) }
                _uiState.update { it.copy(saveFailed = true) }
                return@launch
            }

            storedDay = finished
            _uiState.update {
                it.copy(outcome = outcome, isConfirmingFinishEarly = false, saveFailed = false)
            }
            val routine = _uiState.value.routine
            _savedEvents.send(
                SessionSavedEvent(
                    dayNumber = _uiState.value.dayNumber,
                    performedExercises = routine.performedExerciseCount,
                    plannedExercises = routine.plannedExerciseCount
                )
            )
        }
    }

    fun retryFinishEarly() = finishEarly()

    // Clears the ticks and the logged numbers; the prescribed targets were
    // never overwritten, so they need no restoring.
    fun clearProgress() {
        cancelTick()
        _uiState.update { it.copy(routine = it.routine.clearProgress(), timer = null) }

        val day = storedDay ?: return
        val cleared = day.copy(exercises = day.exercises.map { it.copy(isCompleted = false) })
        storedDay = cleared
        viewModelScope.launch {
            cleared.exercises.forEach { userRepository.updateWorkoutExercise(it, day.id) }
            persistFilledSets(_uiState.value.routine.exercises.map { it.position })
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
        _uiState.update {
            it.copy(expandedVideo = if (it.expandedVideo == position) null else position)
        }
    }

    // Kept apart from the video: collapsing the section should not also lose
    // the player someone left open inside it.
    fun toggleHowTo(position: Int) {
        _uiState.update {
            it.copy(expandedHowTo = if (it.expandedHowTo == position) null else position)
        }
    }

    private fun cancelTick() {
        tickJob?.cancel()
        tickJob = null
    }

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

    private fun completionOf(position: Int): Boolean? =
        _uiState.value.routine.exercises.firstOrNull { it.position == position }?.isCompleted

    private fun reconcileCompletion(position: Int, was: Boolean?) {
        val now = completionOf(position) ?: return
        if (now != was) persistExerciseCompleted(position, now)
    }

    // A card's position counts the exercises still in today's session; the
    // stored day also holds the ones an adjustment omitted, so the two lists
    // index differently and only the visible one may be counted from.
    private fun storedIndexAt(position: Int): Int {
        val day = storedDay ?: return -1
        val target = day.visibleExercises.getOrNull(position - 1) ?: return -1
        return day.exercises.indexOfFirst { it === target }
    }

    private fun storedExerciseAt(position: Int): WorkoutExercise? =
        storedDay?.exercises?.getOrNull(storedIndexAt(position))

    private fun persistExerciseCompleted(position: Int, completed: Boolean) {
        val day = storedDay ?: return
        val storedIndex = storedIndexAt(position)
        val exercise = storedExerciseAt(position)?.copy(isCompleted = completed) ?: return

        storedDay = day.copy(
            exercises = day.exercises.mapIndexed { index, e ->
                if (index == storedIndex) exercise else e
            }
        )
        viewModelScope.launch {
            userRepository.updateWorkoutExercise(exercise, day.id)
            // Un-ticking clears the marks on the sets, which must reach the record too.
            persistFilledSets(listOf(position))
            persistDayStatus()
        }
    }

    // Completing writes the prescription onto sets never filled in, so the day
    // reads back the same way for the PREVIOUS column and next week's progression.
    private suspend fun persistFilledSets(positions: List<Int>) {
        var day = storedDay ?: return
        positions.forEach { position ->
            val storedIndex = storedIndexAt(position)
            val stored = day.exercises.getOrNull(storedIndex) ?: return@forEach
            val logged = _uiState.value.routine.exercises
                .firstOrNull { it.position == position }?.sets.orEmpty()
            val before = stored.sets.associateBy { it.id }

            logged
                .filter { it.id != 0L && before[it.id] != it }
                .forEach { userRepository.updateExerciseSet(it, stored.id) }

            day = day.copy(
                exercises = day.exercises.mapIndexed { index, exercise ->
                    if (index != storedIndex) {
                        exercise
                    } else {
                        // The omitted rows are not on screen and must survive:
                        // undo puts them back.
                        exercise.copy(
                            sets = (exercise.sets.filter { it.omittedBy != null } + logged)
                                .sortedBy { it.setNumber }
                        )
                    }
                }
            )
        }
        storedDay = day
    }

    private fun plannedSetCount(): Int =
        _uiState.value.routine.exercises.flatMap { it.sets }.count { it.omittedBy == null }

    private suspend fun persistDayStatus() {
        val day = storedDay ?: return
        // A day finished early is closed for good: correcting a number on it
        // must not reopen it as in progress.
        if (_uiState.value.outcome?.finishKind == FinishKind.PARTIAL) return
        val visible = day.visibleExercises
        val status = when (visible.count { it.isCompleted }) {
            0 -> WorkoutStatus.NOT_STARTED
            visible.size -> WorkoutStatus.COMPLETED
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
                routine = day.toRoutineUi(catalog = SampleWorkoutData.catalog),
                equipment = day.equipment,
                dateMillis = SampleWorkoutData.dateOf(day.dayNumber),
                // "Day 2", not day 3: the design counts workout days, not weekdays.
                dayNumber = index + 1,
                weekNumber = SampleWorkoutData.weekOne.weekNumber,
                completesTheWeek = completesTheWeek(days, index + 1)
            )
        }

        fun completesTheWeek(days: List<WorkoutDay>, dayNumber: Int): Boolean =
            days.filterIndexed { index, _ -> index != dayNumber - 1 }
                .all { it.status == WorkoutStatus.COMPLETED }
    }
}
