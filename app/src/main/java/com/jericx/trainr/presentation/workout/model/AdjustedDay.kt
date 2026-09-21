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

fun WorkoutDay.derivedEquipment(catalog: ExerciseCatalog): List<String> =
    if (!isAdjustedToday) {
        equipment
    } else {
        visibleExercises
            .mapNotNull { catalog[it.exerciseKey]?.equipment }
            .filterNot { it == Equipment.NONE }
            .distinct()
            .map { it.asDisplayText() }
    }

fun WorkoutDay.remainingMinutes(user: UserProfile, catalog: ExerciseCatalog): Int =
    SessionEstimate.minutes(this, user, TimeScope.WHOLE_SESSION, catalog)
