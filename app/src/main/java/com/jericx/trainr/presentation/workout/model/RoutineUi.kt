package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.unstuck.ActualOrigin

import kotlin.math.roundToInt

data class RoutineUi(
    val title: String,
    val exercises: List<ExerciseUi>
) {
    val completedCount: Int get() = exercises.count { it.isCompleted }

    val totalMinutes: Int get() = exercises.sumOf { it.minutes }

    val completionPercentage: Int
        get() = if (exercises.isEmpty()) 0 else (completedCount * 100.0 / exercises.size).roundToInt()

    val isComplete: Boolean get() = exercises.isNotEmpty() && completedCount == exercises.size

    val plannedExerciseCount: Int get() = exercises.count { !it.isOmitted }

    val performedExerciseCount: Int get() = exercises.count { !it.isOmitted && it.isPerformed }

    fun toggleCompleted(position: Int): RoutineUi = copy(
        exercises = exercises.map {
            when {
                it.position != position -> it
                it.isCompleted -> it.notLogged()
                else -> it.loggedAsPrescribed()
            }
        }
    )

    fun markCompleted(position: Int): RoutineUi = copy(
        exercises = exercises.map {
            if (it.position == position) it.loggedAsPrescribed() else it
        }
    )

    fun updateSet(position: Int, set: ExerciseSet): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            if (exercise.position != position) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets.map {
                        if (it.setNumber == set.setNumber) set.withOriginAfter(it) else it
                    }
                ).tickedFromItsSets()
            }
        }
    )

    fun addSet(position: Int): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            if (exercise.position != position) {
                exercise
            } else {
                val last = exercise.sets.lastOrNull()
                val taken = exercise.sets.map { it.setNumber } + exercise.omittedSetNumbers
                exercise.copy(
                    sets = exercise.sets + ExerciseSet(
                        setNumber = (taken.maxOrNull() ?: 0) + 1,
                        targetReps = last?.targetReps,
                        targetWeightKg = last?.targetWeightKg,
                        targetSeconds = last?.targetSeconds
                    )
                ).tickedFromItsSets()
            }
        }
    )

    // Matched by set number, not instance: a reload replaces instances with
    // equal-looking copies, so the row reporting the swipe may hold the old one.
    fun removeSet(position: Int, setNumber: Int): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            if (exercise.position != position) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets
                        .filter { it.setNumber != setNumber }
                        .mapIndexed { index, kept -> kept.copy(setNumber = index + 1) }
                ).tickedFromItsSets()
            }
        }
    )

    fun completeAll(): RoutineUi = copy(
        exercises = exercises.map { it.loggedAsPrescribed() }
    )

    // Clears the actuals only — logging never wrote to the prescription — and
    // leaves hand-added or deleted sets alone, which would be undo instead.
    fun clearProgress(): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            exercise.copy(
                isCompleted = false,
                sets = exercise.sets.map {
                    it.copy(
                        actualReps = null,
                        actualWeightKg = null,
                        actualSeconds = null,
                        isCompleted = false,
                        actualOrigin = ActualOrigin.NONE
                    )
                }
            )
        }
    )

    val hasProgress: Boolean
        get() = exercises.any { exercise ->
            exercise.isCompleted || exercise.sets.any {
                it.isCompleted || it.actualReps != null ||
                    it.actualWeightKg != null || it.actualSeconds != null
            }
        }
}

// An exercise is done when its sets are, applied beside every set edit so the
// two cannot disagree; a blank set logs its prescription below, or PREVIOUS and
// next week's progression read a finished day back as skipped.
private fun ExerciseUi.tickedFromItsSets(): ExerciseUi =
    copy(isCompleted = sets.isNotEmpty() && sets.all { it.isCompleted })

private fun ExerciseUi.loggedAsPrescribed(): ExerciseUi = copy(
    isCompleted = true,
    sets = sets.map {
        it.copy(
            actualReps = it.actualReps ?: it.targetReps,
            actualWeightKg = it.actualWeightKg ?: it.targetWeightKg,
            actualSeconds = it.actualSeconds ?: it.targetSeconds,
            isCompleted = true,
            actualOrigin = if (it.hasActuals) it.actualOrigin else ActualOrigin.CONFIRMED_TARGET
        )
    }
)

// Un-ticking clears the marks and leaves the numbers with their origin: hand-typed logs stay.
private fun ExerciseUi.notLogged(): ExerciseUi = copy(
    isCompleted = false,
    sets = sets.map { it.copy(isCompleted = false) }
)

private val ExerciseUi.isOmitted: Boolean
    get() = sets.isNotEmpty() && sets.all { it.omittedBy != null }

private val ExerciseUi.isPerformed: Boolean
    get() {
        val planned = sets.filter { it.omittedBy == null }
        return if (planned.isEmpty()) isCompleted else planned.all { it.isCompleted }
    }

private val ExerciseSet.hasActuals: Boolean
    get() = actualReps != null || actualWeightKg != null || actualSeconds != null

private fun ExerciseSet.withOriginAfter(stored: ExerciseSet): ExerciseSet {
    val changed = actualReps != stored.actualReps ||
        actualWeightKg != stored.actualWeightKg ||
        actualSeconds != stored.actualSeconds
    return copy(
        actualOrigin = when {
            !hasActuals -> ActualOrigin.NONE
            changed -> ActualOrigin.TYPED
            else -> stored.actualOrigin
        }
    )
}
