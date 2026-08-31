package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import com.jericx.trainr.domain.model.WorkoutDay

// The seam generated routines arrive through: a day as stored becomes the
// routine as shown. Position is the order the exercises come in rather than a
// stored field, so a reordered routine renumbers itself.
// Prescriptions are snapped to a weight the client can load here, once, rather
// than on the way to the screen: ticking an exercise off logs its target, so a
// target the display had rounded on its own would be stored as the raw number
// and read back as a different one.
fun WorkoutDay.toRoutineUi(
    previousByKey: Map<String, List<ExerciseSet>> = emptyMap(),
    units: UnitSystem = UnitSystem.Default
): RoutineUi = RoutineUi(
    title = title,
    exercises = exercises.mapIndexed { index, exercise ->
        ExerciseUi(
            position = index + 1,
            name = exercise.name,
            description = exercise.instructions,
            minutes = exercise.durationMinutes,
            detail = exercise.prescription,
            measure = exercise.measure,
            sets = exercise.sets.map { set ->
                set.targetWeightKg
                    ?.let { set.copy(targetWeightKg = WeightUnit.loadable(it, units)) }
                    ?: set
            },
            previousSets = previousByKey[exercise.exerciseKey].orEmpty(),
            videoUrl = exercise.videoTutorialUrl
                ?: ExerciseVideoCatalog.urlFor(exercise.exerciseKey),
            isCompleted = exercise.isCompleted
        )
    }
)
