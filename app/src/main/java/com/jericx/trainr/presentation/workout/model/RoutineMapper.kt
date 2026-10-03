package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.asDisplayText
import com.jericx.trainr.domain.unstuck.SessionEstimate
import com.jericx.trainr.domain.unstuck.TimeScope

// What a movement is and how it is done come from the catalog; a stored week
// only says which movement and how much. Omitted sets and the exercises left
// with none of them are today's adjustment and are not part of the routine.
// The minutes are read off the sets still planned, so a cut shows on the card.
fun WorkoutDay.toRoutineUi(
    previousByKey: Map<String, List<ExerciseSet>> = emptyMap(),
    catalog: ExerciseCatalog? = null,
    injuries: List<Injury> = emptyList(),
    user: UserProfile? = null
): RoutineUi = RoutineUi(
    title = title,
    exercises = visibleExercises.mapIndexed { index, exercise ->
        val movement = catalog?.get(exercise.exerciseKey)
        ExerciseUi(
            position = index + 1,
            exerciseId = exercise.id,
            name = exercise.name,
            description = movement?.summary.orEmpty(),
            minutes = if (user != null && catalog != null) {
                SessionEstimate.exerciseMinutes(exercise, user, TimeScope.WHOLE_SESSION, catalog)
                    ?: exercise.durationMinutes
            } else {
                exercise.durationMinutes
            },
            measure = exercise.measure,
            sets = exercise.sets.filter { it.omittedBy == null },
            omittedSetNumbers = exercise.sets.filter { it.omittedBy != null }
                .map { it.setNumber },
            previousSets = previousByKey[exercise.exerciseKey].orEmpty(),
            videoUrl = exercise.videoTutorialUrl
                ?: ExerciseVideoCatalog.urlFor(exercise.exerciseKey),
            primaryMuscle = movement?.primary?.asDisplayText().orEmpty(),
            secondaryMuscles = movement?.secondary?.map { it.asDisplayText() }.orEmpty(),
            steps = movement?.steps.orEmpty(),
            caution = movement?.let { InjuryGuard.cautionFor(it, injuries) },
            isCompleted = exercise.isCompleted
        )
    }
)
