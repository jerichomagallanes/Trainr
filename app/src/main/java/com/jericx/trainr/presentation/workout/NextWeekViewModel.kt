package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.data.preferences.LanguageCodeProvider
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
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

    // The finished week seeds the request, so the model progresses from what
    // was actually lifted instead of restarting from the intake answers.
    fun generateNextWeek(onDone: () -> Unit) {
        viewModelScope.launch {
            _failure.value = null
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

    companion object {
        private const val DAYS_PER_WEEK = 7
    }
}
