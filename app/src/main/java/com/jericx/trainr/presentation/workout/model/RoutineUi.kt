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
                ).tickedFromItsSets()
            }
        }
    )

    // The remaining sets renumber so the table never shows 1, 3. Matching is by
    // set number rather than instance: a reload replaces every instance with an
    // equal-looking one, and the row that reports the swipe may be holding the
    // old one.
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

    // Back to a session nobody has started. The logged numbers go and the
    // prescription stays, which costs nothing to do because the two were never
    // the same field: logging only ever wrote to the actuals.
    //
    // Sets added or deleted by hand are left as they are. Restoring those would
    // be undo, which is a different promise than this one makes.
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

    // Whether there is anything to clear. A session nobody has touched must not
    // offer to undo work that does not exist.
    val hasProgress: Boolean
        get() = exercises.any { exercise ->
            exercise.isCompleted || exercise.sets.any {
                it.isCompleted || it.actualReps != null ||
                    it.actualWeightKg != null || it.actualSeconds != null
            }
        }
}

// Ticking an exercise off says its prescription was done, so a set left blank
// records what was asked for. Without this a finished day is stored with
// nothing on its sets: the PREVIOUS column has nothing to show, and next
// week's prompt reads the whole session back as "did: skipped".
// An exercise is done when its sets are: ticking off the last one finishes it
// there and then, and adding a set that has not been done reopens it. Kept
// beside the edits themselves so no later one can leave the two disagreeing —
// which is what left a finished exercise looking untouched.
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

// Un-ticking clears the marks and leaves the numbers: they are logs, and
// hand-typed ones would be thrown away with them.
private fun ExerciseUi.notLogged(): ExerciseUi = copy(
    isCompleted = false,
    sets = sets.map { it.copy(isCompleted = false) }
)
