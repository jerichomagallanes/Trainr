package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.model.ExerciseSet

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
                    sets = exercise.sets.map { if (it.setNumber == set.setNumber) set else it }
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
                exercise.copy(
                    sets = exercise.sets + ExerciseSet(
                        setNumber = exercise.sets.size + 1,
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
                        isCompleted = false
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
// next week's prompt read a finished day back as skipped.
private fun ExerciseUi.tickedFromItsSets(): ExerciseUi =
    copy(isCompleted = sets.isNotEmpty() && sets.all { it.isCompleted })

private fun ExerciseUi.loggedAsPrescribed(): ExerciseUi = copy(
    isCompleted = true,
    sets = sets.map {
        it.copy(
            actualReps = it.actualReps ?: it.targetReps,
            actualWeightKg = it.actualWeightKg ?: it.targetWeightKg,
            actualSeconds = it.actualSeconds ?: it.targetSeconds,
            isCompleted = true
        )
    }
)

// Un-ticking clears the marks and leaves the numbers: hand-typed logs stay.
private fun ExerciseUi.notLogged(): ExerciseUi = copy(
    isCompleted = false,
    sets = sets.map { it.copy(isCompleted = false) }
)
