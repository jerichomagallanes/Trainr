package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import com.jericx.trainr.domain.model.WorkoutDay

// Prescriptions are snapped to a loadable weight here, once, rather than on the
// way to the screen: ticking an exercise off logs its target, so a rounding done
// only for display would be stored raw and read back as a different number.
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
