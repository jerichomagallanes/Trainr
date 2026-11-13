package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.asDisplayText
import com.jericx.trainr.domain.unstuck.SessionEstimate
import com.jericx.trainr.domain.unstuck.TimeScope

// An omitted set stays in the record so undo can put it back, but it is not
// part of today's session and nothing may count it.
val WorkoutDay.visibleExercises: List<WorkoutExercise>
    get() = exercises.filterNot { it.isOmittedToday }

val WorkoutDay.isAdjustedToday: Boolean
    get() = exercises.any { exercise ->
        exercise.addedBy != null || exercise.sets.any { it.omittedBy != null }
    }

// duration, exerciseCount and equipment are generator outputs that applying an
// adjustment deliberately leaves alone, so undo can restore the day exactly.
// Everything shown is therefore derived from the sets that are still planned.
fun WorkoutDay.derivedExerciseCount(): Int =
    if (isAdjustedToday) visibleExercises.size else exerciseCount

// Starts from the stored line, which names kit the catalog does not know: a
// substitute adds its own, an omitted exercise's goes once nothing visible needs it.
fun WorkoutDay.derivedEquipment(catalog: ExerciseCatalog): List<String> {
    if (!isAdjustedToday) return equipment
    val visible = visibleExercises
    val needed = visible.kit(catalog).toSet()
    val dropped = exercises.filter { it.isOmittedToday }.kit(catalog).filterNot { it in needed }
    val kept = equipment.filterNot { name -> dropped.any { name.describes(it) } }
    val added = visible.filter { it.addedBy != null }.kit(catalog)
        .distinct()
        .filterNot { kit -> kept.any { it.describes(kit) } }
        .map { it.asDisplayText() }
    return (kept + added).ifEmpty { equipment }
}

private fun List<WorkoutExercise>.kit(catalog: ExerciseCatalog): List<Equipment> =
    mapNotNull { catalog[it.exerciseKey]?.equipment }.filterNot { it == Equipment.NONE }

private fun String.describes(equipment: Equipment): Boolean =
    startsWith(equipment.asDisplayText(), ignoreCase = true)

fun WorkoutDay.remainingMinutes(user: UserProfile, catalog: ExerciseCatalog): Int =
    SessionEstimate.minutes(this, user, TimeScope.WHOLE_SESSION, catalog)
