package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.data.preferences.LanguageCodeProvider
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.withoutWeekNumber
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NextWeekViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val planGenerator: PlanGenerator,
    private val languageCode: LanguageCodeProvider
) : ViewModel() {

    private val _failure = MutableStateFlow<PlanGenerationResult.Failure?>(null)
    val failure: StateFlow<PlanGenerationResult.Failure?> = _failure.asStateFlow()

    // Running the same week again: the sessions and their loads as they were
    // written, with every log cleared. Sound coaching after a week that was not
    // finished, or one where the prescribed weights never went up — and it asks
    // nothing of the network, so it is the way through when the model cannot be
    // reached. It is offered, never substituted.
    fun repeatLastWeek(onDone: () -> Unit) {
        viewModelScope.launch {
            _failure.value = null
            val (user, latest) = nextWeekFrom() ?: return@launch onDone()
            userRepository.saveWeeklyWorkoutPlan(
                repeatedWeek(latest, latest.weekNumber + 1, startAfter(latest))
            )
            onDone()
        }
    }

    // The finished week seeds the request, so the model progresses from what
    // was actually lifted instead of restarting from the intake answers.
    fun generateNextWeek(onDone: () -> Unit) {
        viewModelScope.launch {
            _failure.value = null
            val (user, latest) = nextWeekFrom() ?: return@launch onDone()
            val nextNumber = latest.weekNumber + 1
            val start = startAfter(latest)
            val result = planGenerator.generate(
                PlanRequest(
                    user = user,
                    weekNumber = nextNumber,
                    startDateMillis = start,
                    languageCode = languageCode.current(),
                    previousWeek = latest
                )
            )

            // Repeating the finished week used to stand in here. It is the same
            // dishonesty as the sample week: the client is told next week is
            // ready when the coach never wrote it, and repeating a week is a
            // decision they should get to make.
            if (result !is PlanGenerationResult.Generated) {
                _failure.value = result as PlanGenerationResult.Failure
                return@launch
            }

            userRepository.saveWeeklyWorkoutPlan(result.plan)
            onDone()
        }
    }

    // The user and the week to build on, or nothing when there is neither —
    // and nothing to do when the week after this one already exists, so that
    // revisiting the completion screen cannot stack duplicates.
    private suspend fun nextWeekFrom(): Pair<UserProfile, WeeklyWorkoutPlan>? {
        val user = userRepository.getCurrentUser() ?: return null
        val latest = userRepository.getWeeklyWorkoutPlans(user.id).first()
            .maxByOrNull { it.weekNumber } ?: return null
        if (userRepository.getWeeklyWorkoutPlan(user.id, latest.weekNumber + 1) != null) return null
        return user to latest
    }

    // Never overlapping the week it follows, and never starting in the past:
    // someone coming back a fortnight late begins today, not on a date that has
    // already gone.
    private fun startAfter(previous: WeeklyWorkoutPlan): Long = maxOf(
        previous.startDateMillis
            ?.let { WorkoutWeek.dateOfDay(it, DAYS_PER_WEEK + 1) }
            ?: WorkoutWeek.startOfDay(),
        WorkoutWeek.startOfDay()
    )

    companion object {
        private const val DAYS_PER_WEEK = 7

        // The same week over again: nothing carried across but the plan itself.
        fun repeatedWeek(
            previous: WeeklyWorkoutPlan,
            weekNumber: Int,
            startDateMillis: Long
        ): WeeklyWorkoutPlan {
            val now = System.currentTimeMillis()
            return previous.copy(
                id = 0,
                weekNumber = weekNumber,
                // Plans stored before titles were cleaned still carry a number.
                title = previous.title.withoutWeekNumber(),
                startDateMillis = startDateMillis,
                createdAt = now,
                updatedAt = now,
                workoutDays = previous.workoutDays.map { day ->
                    day.copy(
                        id = 0,
                        status = WorkoutStatus.NOT_STARTED,
                        completedAt = null,
                        exercises = day.exercises.map { exercise ->
                            exercise.copy(
                                id = 0,
                                isCompleted = false,
                                sets = exercise.sets.map {
                                    it.copy(
                                        id = 0,
                                        actualReps = null,
                                        actualWeightKg = null,
                                        actualSeconds = null,
                                        isCompleted = false
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    }
}
