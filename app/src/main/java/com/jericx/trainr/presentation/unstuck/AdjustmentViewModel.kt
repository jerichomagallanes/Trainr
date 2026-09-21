package com.jericx.trainr.presentation.unstuck

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.R
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentConstraint
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AdjustmentSnapshot
import com.jericx.trainr.domain.unstuck.ApplyResult
import com.jericx.trainr.domain.unstuck.PolicyDecision
import com.jericx.trainr.domain.unstuck.SessionEstimate
import com.jericx.trainr.domain.unstuck.TimePresets
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.domain.unstuck.UnstuckPolicy
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.domain.unstuck.intent.IntentInterpreter
import com.jericx.trainr.domain.unstuck.intent.IntentValidation
import com.jericx.trainr.domain.unstuck.intent.IntentRouting
import com.jericx.trainr.domain.unstuck.intent.InterpreterAvailability
import com.jericx.trainr.domain.unstuck.intent.InterpreterResult
import com.jericx.trainr.domain.unstuck.intent.UnstuckRoute
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseChoice(val id: Long, val name: String)

enum class ApplyErrorUi { STALE_REBUILT, NOT_APPLIED }

data class AdjustmentUiState(
    val isLoaded: Boolean = false,
    val day: WorkoutDay? = null,
    val dayTitle: String = "",
    val plannedMinutes: Int = 0,
    val weekdayName: String? = null,
    @StringRes val goalLabelRes: Int = R.string.general_fitness_goal,
    val reason: DirectReason = DirectReason.OTHER,
    val hasPerformedWork: Boolean = false,
    val presets: List<Int> = emptyList(),
    val selectedMinutes: Int? = null,
    val customMinutesText: String = "",
    val minutesError: Boolean = false,
    val exerciseChoices: List<ExerciseChoice> = emptyList(),
    val selectedExerciseId: Long? = null,
    val enteredWithExercise: Boolean = false,
    val availableEquipment: Set<Equipment> = emptySet(),
    val note: String = "",
    val decision: PolicyDecision? = null,
    val review: ReviewUi? = null,
    val isApplying: Boolean = false,
    val applyError: ApplyErrorUi? = null
) {
    // Whole-session and remaining are different questions; answering one with
    // the other subtracts time nobody measured.
    val scope: TimeScope
        get() = if (hasPerformedWork) TimeScope.REMAINING else TimeScope.WHOLE_SESSION

    val isPresetSelected: Boolean get() = customMinutesText.isEmpty() && selectedMinutes != null

    val canShowRecommendation: Boolean
        get() = when (reason) {
            DirectReason.LESS_TIME ->
                selectedMinutes != null && TimePresets.isSupported(selectedMinutes)

            DirectReason.EQUIPMENT -> selectedExerciseId != null && availableEquipment.isNotEmpty()
            else -> false
        }
}

@HiltViewModel
class AdjustmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val adjustmentRepository: AdjustmentRepository,
    private val catalog: ExerciseCatalog,
    private val interpreter: IntentInterpreter,
    private val breadcrumbs: Breadcrumbs
) : ViewModel() {

    private val policy = UnstuckPolicy(catalog)

    private val requestedDayNumber: Int = savedStateHandle[Screen.Adjust.ARG_DAY_NUMBER] ?: 1

    private val requestedWeekNumber: Int? =
        savedStateHandle.get<Int>(Screen.Adjust.ARG_WEEK_NUMBER)?.takeIf { it > 0 }

    private val requestedReason: DirectReason = savedStateHandle
        .get<String>(Screen.Adjust.ARG_REASON)
        ?.let { name -> DirectReason.entries.firstOrNull { it.name == name } }
        ?: DirectReason.OTHER

    private val requestedExerciseId: Long? =
        savedStateHandle.get<Long>(Screen.Adjust.ARG_EXERCISE_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        AdjustmentUiState(
            reason = requestedReason,
            selectedExerciseId = requestedExerciseId,
            enteredWithExercise = requestedExerciseId != null
        )
    )
    val uiState: StateFlow<AdjustmentUiState> = _uiState.asStateFlow()

    private val _appliedEvents = Channel<String>(Channel.BUFFERED)
    val appliedEvents: Flow<String> = _appliedEvents.receiveAsFlow()

    private val _routeEvents = Channel<UnstuckRoute>(Channel.BUFFERED)
    val routeEvents: Flow<UnstuckRoute> = _routeEvents.receiveAsFlow()

    private var user: UserProfile? = null

    init {
        viewModelScope.launch { load() }
    }

    fun selectMinutes(minutes: Int) {
        _uiState.update {
            it.copy(selectedMinutes = minutes, customMinutesText = "", minutesError = false)
        }
    }

    // An unsupported number is refused, never clamped into a different request.
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

    fun selectExercise(id: Long) {
        _uiState.update { it.copy(selectedExerciseId = id) }
    }

    fun toggleEquipment(equipment: Equipment) {
        _uiState.update { state ->
            val available = state.availableEquipment
            state.copy(
                availableEquipment = if (equipment in available) {
                    available - equipment
                } else {
                    available + equipment
                }
            )
        }
    }

    fun typeNote(text: String) {
        _uiState.update { it.copy(note = text) }
    }

    fun chooseFromContext(reason: DirectReason) {
        _uiState.update { it.copy(reason = reason) }
        viewModelScope.launch {
            val validation = interpretNote()
            _routeEvents.send(IntentRouting.routeFor(reason, validation))
        }
    }

    // Returns the proposal that a gate may be asked about; null when nothing
    // was proposed, which is a result and not a failure.
    fun showRecommendation(): String? {
        val state = _uiState.value
        val day = state.day ?: return null
        val profile = user ?: return null
        val constraint = constraintFor(state) ?: return null

        val decision = policy.decide(
            snapshot = AdjustmentSnapshot(day = day, user = profile),
            constraint = constraint,
            requestId = "day:${day.id}"
        )
        _uiState.update {
            it.copy(
                decision = decision,
                review = decision.toReviewUi(
                    day = day,
                    catalog = catalog,
                    goalLabelRes = it.goalLabelRes,
                    hasPerformedWork = it.hasPerformedWork,
                    plannedMinutes = it.plannedMinutes
                ),
                applyError = null
            )
        }
        return (decision as? PolicyDecision.Proposed)?.proposal?.proposalId
    }

    fun apply() {
        val state = _uiState.value
        val proposed = state.decision as? PolicyDecision.Proposed ?: return
        val day = state.day ?: return
        if (state.isApplying) return

        _uiState.update { it.copy(isApplying = true, applyError = null) }
        viewModelScope.launch {
            val result = adjustmentRepository.apply(
                proposal = proposed.proposal,
                dayId = day.id,
                reason = state.reason.asAdjustmentReason(),
                nowMillis = System.currentTimeMillis()
            )
            when (result) {
                is ApplyResult.Applied -> applied(proposed.proposal.proposalId)
                is ApplyResult.AlreadyApplied -> applied(proposed.proposal.proposalId)
                is ApplyResult.Stale, is ApplyResult.Rejected -> rebuild()
                is ApplyResult.Failed -> {
                    breadcrumbs.record("adjust_apply_failed")
                    _uiState.update {
                        it.copy(isApplying = false, applyError = ApplyErrorUi.NOT_APPLIED)
                    }
                }
            }
        }
    }

    fun retryApply() = apply()

    private suspend fun applied(proposalId: String) {
        _uiState.update { it.copy(isApplying = false, applyError = null) }
        _appliedEvents.send(proposalId)
    }

    // The plan moved under the preview, so the recommendation is built again
    // from what is stored now and nothing is applied.
    private suspend fun rebuild() {
        val day = _uiState.value.day?.let { userRepository.getWorkoutDay(it.id) }
        val profile = user
        _uiState.update {
            val rebuilt = if (day != null && profile != null) it.readFrom(day, profile) else it
            rebuilt.copy(isApplying = false)
        }
        showRecommendation()
        _uiState.update { it.copy(applyError = ApplyErrorUi.STALE_REBUILT) }
    }

    private suspend fun interpretNote(): IntentValidation? {
        if (interpreter.availability != InterpreterAvailability.READY) return null
        val note = _uiState.value.note
        if (note.isBlank()) return null
        val result = interpreter.interpret(note, Locale.getDefault(), DirectReason.OTHER)
        return (result as? InterpreterResult.Interpreted)?.validation
    }

    private fun constraintFor(state: AdjustmentUiState): AdjustmentConstraint? = when (state.reason) {
        DirectReason.LESS_TIME -> state.selectedMinutes
            ?.let { AdjustmentConstraint.LessTime(it, state.scope) }

        DirectReason.EQUIPMENT -> state.selectedExerciseId
            ?.takeIf { state.availableEquipment.isNotEmpty() }
            ?.let { AdjustmentConstraint.EquipmentUnavailable(it, state.availableEquipment) }

        else -> null
    }

    private suspend fun load() {
        val profile = userRepository.getCurrentUser()
        user = profile
        val plan = profile?.let {
            val plans = userRepository.getWeeklyWorkoutPlans(it.id).first()
            if (requestedWeekNumber == null) {
                plans.maxByOrNull { stored -> stored.weekNumber }
            } else {
                plans.firstOrNull { stored -> stored.weekNumber == requestedWeekNumber }
            }
        }
        val day = plan?.workoutDays?.firstOrNull { it.dayNumber == requestedDayNumber }
        if (profile == null || day == null) {
            _uiState.update { it.copy(isLoaded = true) }
            return
        }

        _uiState.update {
            it.readFrom(day, profile).copy(
                isLoaded = true,
                weekdayName = plan.startDateMillis?.let { start ->
                    WorkoutDateFormatter.formatWeekday(
                        WorkoutWeek.dateOfDay(start, day.dayNumber),
                        Locale.getDefault()
                    )
                },
                goalLabelRes = profile.fitnessGoal.labelRes
            )
        }
    }

    // Everything the request is built from moves with the day: a session that
    // has been worked on since is a different question, not the same one again.
    private fun AdjustmentUiState.readFrom(
        day: WorkoutDay,
        profile: UserProfile
    ): AdjustmentUiState {
        val performed = day.exercises.any { exercise -> exercise.sets.any { it.isCompleted } }
        val scope = if (performed) TimeScope.REMAINING else TimeScope.WHOLE_SESSION
        val planned = SessionEstimate.minutes(day, profile, scope, catalog)

        return copy(
            day = day,
            dayTitle = day.title,
            plannedMinutes = planned,
            hasPerformedWork = performed,
            presets = TimePresets.forPlanned(planned).filter(TimePresets::isSupported),
            exerciseChoices = day.exercises
                .filter { exercise ->
                    exercise.sets.any { set -> set.omittedBy == null && !set.isCompleted }
                }
                .map { exercise -> ExerciseChoice(exercise.id, exercise.name) }
        )
    }
}

private fun DirectReason.asAdjustmentReason(): AdjustmentReason = when (this) {
    DirectReason.EQUIPMENT -> AdjustmentReason.EQUIPMENT_UNAVAILABLE
    else -> AdjustmentReason.LESS_TIME
}

@get:StringRes
val FitnessGoal.labelRes: Int
    get() = when (this) {
        FitnessGoal.WEIGHT_LOSS -> R.string.lose_weight_goal
        FitnessGoal.MUSCLE_GAIN -> R.string.build_muscle_goal
        FitnessGoal.STRENGTH -> R.string.get_stronger_goal
        FitnessGoal.ENDURANCE -> R.string.improve_endurance_goal
        FitnessGoal.GENERAL_FITNESS -> R.string.general_fitness_goal
        FitnessGoal.FLEXIBILITY -> R.string.flexibility_mobility_goal
    }
