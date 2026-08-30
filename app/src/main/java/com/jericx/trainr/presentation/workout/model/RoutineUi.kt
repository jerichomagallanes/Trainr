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
            if (it.position == position) it.copy(isCompleted = !it.isCompleted) else it
        }
    )

    fun markCompleted(position: Int): RoutineUi = copy(
        exercises = exercises.map {
            if (it.position == position) it.copy(isCompleted = true) else it
        }
    )

    fun updateSet(position: Int, set: ExerciseSet): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            if (exercise.position != position) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets.map { if (it.setNumber == set.setNumber) set else it }
                )
            }
        }
    )

    // A new set repeats the last one's target: the most likely next thing to do
    // is what you just did.
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
                )
            }
        }
    )

    // The remaining sets renumber so the table never shows 1, 3. Matching is by
    // instance, not value: a swipe can report twice, and after the renumbering
    // a number — or an equal-looking set — would point at an innocent
    // neighbour, while the stale instance matches nothing. The last set cannot
    // be deleted: an empty table has no target left to grow back from.
    fun removeSet(position: Int, set: ExerciseSet): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            if (exercise.position != position || exercise.sets.size <= 1) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets
                        .filter { it !== set }
                        .mapIndexed { index, kept -> kept.copy(setNumber = index + 1) }
                )
            }
        }
    )

    fun completeAll(): RoutineUi = copy(
        exercises = exercises.map { it.copy(isCompleted = true) }
    )
}
