package com.jericx.trainr.domain.unstuck

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.generation.RepWindow
import com.jericx.trainr.domain.generation.SessionBudget
import com.jericx.trainr.domain.generation.SessionMinutes
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise

// The same arithmetic the generator budgeted the day with, read back off the
// stored day. A second estimator would let a plan be built that this one calls
// too long.
object SessionEstimate {

    fun minutes(
        day: WorkoutDay,
        user: UserProfile,
        scope: TimeScope,
        catalog: ExerciseCatalog
    ): Int = SessionMinutes.forDay(
        day.exercises.mapNotNull { exercise ->
            val counted = exercise.counted(scope)
            if (counted.isEmpty()) return@mapNotNull null
            val entry = catalog[exercise.exerciseKey]
            SessionMinutes.forExercise(
                measure = exercise.measure,
                perSet = counted.map { it.seconds(exercise.measure, user, entry) },
                restSeconds = exercise.restTime ?: restFor(user, entry),
                unilateral = entry?.unilateral == true
            )
        }
    )

    private fun WorkoutExercise.counted(scope: TimeScope): List<ExerciseSet> =
        sets.filter { it.omittedBy == null && (scope == TimeScope.WHOLE_SESSION || !it.isCompleted) }

    private fun ExerciseSet.seconds(
        measure: ExerciseMeasure,
        user: UserProfile,
        entry: CatalogExercise?
    ): Int = if (measure == ExerciseMeasure.DURATION) {
        targetSeconds ?: 0
    } else {
        targetReps ?: entry?.let { RepWindow.forExercise(user, it).first } ?: 0
    }

    private fun restFor(user: UserProfile, entry: CatalogExercise?): Int =
        entry?.let { SessionBudget.restSeconds(user.fitnessGoal, it.role) }
            ?: SessionBudget.restSeconds(user.fitnessGoal)
}
