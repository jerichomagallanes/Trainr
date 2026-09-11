package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.WorkoutDay

// What a movement is and how it is done come from the catalog; a stored week
// only says which movement and how much. Copy a model once wrote is read only
// where the catalog has nothing to say.
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
            description = movement?.summary?.takeIf { it.isNotBlank() } ?: exercise.instructions,
            minutes = exercise.durationMinutes,
            measure = exercise.measure,
            sets = exercise.sets,
            previousSets = previousByKey[exercise.exerciseKey].orEmpty(),
            videoUrl = exercise.videoTutorialUrl
                ?: ExerciseVideoCatalog.urlFor(exercise.exerciseKey),
            primaryMuscle = movement?.primary?.asDisplayText().orEmpty(),
            secondaryMuscles = movement?.secondary?.map { it.asDisplayText() }.orEmpty(),
            steps = movement?.steps.orEmpty(),
            unilateral = movement?.unilateral == true,
            caution = movement?.let { InjuryGuard.cautionFor(it, injuries) },
            isCompleted = exercise.isCompleted
        )
    }
)

// Anatomy read off a controlled vocabulary, the same way the day's equipment
// is: LOWER_BACK is Lower Back everywhere, so there is nothing to translate
// that the enum does not already say.
private fun MuscleGroup.asDisplayText(): String = name.lowercase()
    .split('_')
    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
