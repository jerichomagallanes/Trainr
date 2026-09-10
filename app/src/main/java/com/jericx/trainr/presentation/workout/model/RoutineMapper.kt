package com.jericx.trainr.presentation.workout.model

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import com.jericx.trainr.domain.model.WorkoutDay

// Prescriptions are snapped to a loadable weight here, once, rather than on the
// way to the screen: ticking an exercise off logs its target, so a rounding done
// only for display would be stored raw and read back as a different number.
fun WorkoutDay.toRoutineUi(
    previousByKey: Map<String, List<ExerciseSet>> = emptyMap(),
    units: UnitSystem = UnitSystem.Default,
    catalog: ExerciseCatalog? = null
): RoutineUi = RoutineUi(
    title = title,
    exercises = exercises.mapIndexed { index, exercise ->
        val movement = catalog?.get(exercise.exerciseKey)
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
            primaryMuscle = movement?.primary?.asDisplayText().orEmpty(),
            secondaryMuscles = movement?.secondary?.map { it.asDisplayText() }.orEmpty(),
            steps = movement?.steps.orEmpty(),
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
