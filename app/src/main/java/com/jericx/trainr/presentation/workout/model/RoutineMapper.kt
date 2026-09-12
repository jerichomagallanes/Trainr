package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.asDisplayText

// What a movement is and how it is done come from the catalog; a stored week
// only says which movement and how much.
fun WorkoutDay.toRoutineUi(
    previousByKey: Map<String, List<ExerciseSet>> = emptyMap(),
    catalog: ExerciseCatalog? = null,
    injuries: List<Injury> = emptyList()
): RoutineUi = RoutineUi(
    title = title,
    exercises = exercises.mapIndexed { index, exercise ->
        val movement = catalog?.get(exercise.exerciseKey)
        ExerciseUi(
            position = index + 1,
            name = exercise.name,
            description = movement?.summary.orEmpty(),
            minutes = exercise.durationMinutes,
            measure = exercise.measure,
            sets = exercise.sets,
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
