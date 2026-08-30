package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.data.preferences.LanguageCodeProvider
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NextWeekViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val planGenerator: PlanGenerator,
    private val languageCode: LanguageCodeProvider
) : ViewModel() {

    // The finished week seeds the request, so the model progresses from what
    // was actually lifted instead of restarting from the intake answers.
    fun generateNextWeek(onDone: () -> Unit) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch onDone()
            val latest = userRepository.getWeeklyWorkoutPlans(user.id).first()
                .maxByOrNull { it.weekNumber } ?: return@launch onDone()

            // Revisiting the completion screen must not stack duplicate weeks.
            val nextNumber = latest.weekNumber + 1
            if (userRepository.getWeeklyWorkoutPlan(user.id, nextNumber) != null) {
                return@launch onDone()
            }

            // Never overlapping the week it follows, and never starting in the
            // past: someone coming back a fortnight late begins today, not on a
            // date that has already gone.
            val start = maxOf(
                latest.startDateMillis
                    ?.let { WorkoutWeek.dateOfDay(it, DAYS_PER_WEEK + 1) }
                    ?: WorkoutWeek.startOfDay(),
                WorkoutWeek.startOfDay()
            )
            val plan = planGenerator.generate(
                PlanRequest(
                    user = user,
                    weekNumber = nextNumber,
                    startDateMillis = start,
                    languageCode = languageCode.current(),
                    previousWeek = latest
                )
            ) ?: repeatWeek(latest, nextNumber, start)

            userRepository.saveWeeklyWorkoutPlan(plan)
            onDone()
        }
    }

    companion object {
        private const val DAYS_PER_WEEK = 7

        // The offline fallback runs the finished week again with the logs
        // cleared: repeating a week is sound coaching, inventing progress is not.
        fun repeatWeek(
            previous: WeeklyWorkoutPlan,
            weekNumber: Int,
            startDateMillis: Long
        ): WeeklyWorkoutPlan {
            val now = System.currentTimeMillis()
            return previous.copy(
                id = 0,
                weekNumber = weekNumber,
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
