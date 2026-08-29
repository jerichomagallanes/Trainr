package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.model.WorkoutDay

// The seam generated routines arrive through: a day as stored becomes the
// routine as shown. Position is the order the exercises come in rather than a
// stored field, so a reordered routine renumbers itself.
fun WorkoutDay.toRoutineUi(): RoutineUi = RoutineUi(
    title = title,
    exercises = exercises.mapIndexed { index, exercise ->
        ExerciseUi(
            position = index + 1,
            name = exercise.name,
            description = exercise.instructions,
            minutes = exercise.durationMinutes,
            detail = exercise.prescription,
            measure = exercise.measure,
            sets = exercise.sets,
            videoUrl = exercise.videoTutorialUrl
                ?: ExerciseVideoCatalog.urlFor(exercise.exerciseKey),
            isCompleted = exercise.isCompleted
        )
    }
)
