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
                // Un-ticking leaves the numbers on the rows: they are logs now,
                // and clearing them would throw away hand-typed ones too.
                it.isCompleted -> it.copy(isCompleted = false)
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
    fun removeSet(position: Int, setNumber: Int): RoutineUi = copy(
        exercises = exercises.map { exercise ->
            if (exercise.position != position) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets
                        .filter { it.setNumber != setNumber }
                        .mapIndexed { index, kept -> kept.copy(setNumber = index + 1) }
                )
            }
        }
    )

    fun completeAll(): RoutineUi = copy(
        exercises = exercises.map { it.loggedAsPrescribed() }
    )
}

// Ticking an exercise off says its prescription was done, so a set left blank
// records what was asked for. Without this a finished day is stored with
// nothing on its sets: the PREVIOUS column has nothing to show, and next
// week's prompt reads the whole session back as "did: skipped".
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
