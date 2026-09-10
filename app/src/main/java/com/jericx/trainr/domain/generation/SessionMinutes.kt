package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.ExerciseMeasure
import kotlin.math.ceil

// How long the work actually takes. The skeleton budgets against this and the
// parser checks the finished plan against it, so both have to be the same
// arithmetic or a plan can be built that its own validator rejects.
object SessionMinutes {

    // A rep at the moderate velocity ACSM asks for.
    const val SECONDS_PER_REP = 3

    // Walking to the next station, changing the pin, finding a bench.
    const val TRANSITION_SECONDS = 60

    fun forExercise(
        measure: ExerciseMeasure,
        perSet: List<Int>,
        restSeconds: Int,
        unilateral: Boolean = false
    ): Int {
        val work = when (measure) {
            ExerciseMeasure.DURATION -> perSet.sum()
            else -> perSet.sum() * SECONDS_PER_REP
        }
        // Reps done on one side are done again on the other. A hold is already
        // the whole set, so only counted work doubles.
        val sides = if (unilateral && measure != ExerciseMeasure.DURATION) 2 else 1
        val rest = restSeconds * (perSet.size - 1).coerceAtLeast(0)
        return ceil((work * sides + rest) / 60.0).toInt().coerceAtLeast(1)
    }

    // The day is its exercises plus the walk between them.
    fun forDay(exerciseMinutes: List<Int>): Int {
        if (exerciseMinutes.isEmpty()) return 0
        val transitions = (exerciseMinutes.size - 1) * TRANSITION_SECONDS
        return exerciseMinutes.sum() + ceil(transitions / 60.0).toInt()
    }
}
