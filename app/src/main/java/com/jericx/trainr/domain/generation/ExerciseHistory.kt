package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

// One performance of one movement. A plan stored before startDateMillis
// existed has no date, and a gap nobody can measure is not a gap.
data class LoggedSession(
    val performedAtMillis: Long? = null,
    val prescribedSets: Int = 0,
    val sets: List<ExerciseSet> = emptyList(),
    val measure: ExerciseMeasure = ExerciseMeasure.REPS
) {
    // A ticked set with no number at all is not a performance of anything.
    val completedSets: List<ExerciseSet>
        get() = sets.filter { it.isCompleted && it.amountDone() != null }

    // Logged in a different measure than the movement has today, the numbers
    // describe some other exercise and cannot be progressed from.
    fun isUsableFor(measureNow: ExerciseMeasure): Boolean =
        completedSets.isNotEmpty() && measure == measureNow

    val targetLoadKg: Float? get() = sets.firstNotNullOfOrNull { it.targetWeightKg }

    val targetAmount: Int? get() = sets.firstNotNullOfOrNull { it.targetAmount() }

    val targetAmountRange: IntRange?
        get() = sets.mapNotNull { it.targetAmount() }
            .takeIf { it.isNotEmpty() }
            ?.let { it.min()..it.max() }

    val minDone: Int get() = completedSets.mapNotNull { it.amountDone() }.minOrNull() ?: 0

    // Half the work done is enough to judge; less than that and the week was
    // interrupted, not failed.
    val hasQuorum: Boolean
        get() = prescribedSets > 0 && completedSets.size * 2 >= prescribedSets

    val metInFull: Boolean
        get() = prescribedSets > 0 && completedSets.size >= prescribedSets &&
            completedSets.all { it.met() }

    val isShortByALittle: Boolean
        get() {
            if (completedSets.size < prescribedSets) return false
            val short = completedSets.filterNot { it.met() }
            if (short.size != 1) return false
            val set = short.single()
            return ((set.targetAmount() ?: 0) - (set.amountDone() ?: 0)) in 1..2
        }

    private fun ExerciseSet.targetAmount(): Int? =
        if (measure == ExerciseMeasure.DURATION) targetSeconds else targetReps

    // Ticking a set logs its prescription, so an untyped set reads as met.
    private fun ExerciseSet.amountDone(): Int? =
        if (measure == ExerciseMeasure.DURATION) actualSeconds ?: targetSeconds
        else actualReps ?: targetReps

    // The reps count only at the weight asked for: a client who typed a
    // lighter load did not earn the next one.
    private fun ExerciseSet.met(): Boolean {
        val target = targetAmount() ?: return true
        val done = amountDone() ?: return false
        val asked = targetWeightKg
        val held = actualWeightKg ?: asked
        val weightHeld = asked == null || held == null || held >= asked - WEIGHT_TOLERANCE_KG
        return done >= target && weightHeld
    }

    private companion object {
        const val WEIGHT_TOLERANCE_KG = 0.01f
    }
}

// Newest first.
data class ExerciseHistory(val sessions: List<LoggedSession> = emptyList()) {

    // Consecutive most recent sessions attempted in earnest that still fell
    // short of what they asked. Judged against their own stored targets, so a
    // profile edit between weeks cannot rewrite it.
    val stallCount: Int
        get() = sessions.takeWhile { it.hasQuorum && !it.metInFull }.size

    companion object {
        // The deepest any rule reaches.
        const val HISTORY_DEPTH = 4
        val None = ExerciseHistory()

        private const val DAY_MILLIS = 86_400_000L

        fun from(weeks: List<WeeklyWorkoutPlan>, exerciseKey: String): ExerciseHistory =
            ExerciseHistory(
                weeks.sortedByDescending { it.weekNumber }
                    .flatMap { week ->
                        week.workoutDays.sortedByDescending { it.dayNumber }.mapNotNull { day ->
                            val exercise = day.exercises
                                .firstOrNull { it.exerciseKey == exerciseKey }
                                ?: return@mapNotNull null
                            LoggedSession(
                                performedAtMillis = day.completedAt
                                    ?: week.startDateMillis?.plus((day.dayNumber - 1) * DAY_MILLIS),
                                prescribedSets = exercise.sets.size,
                                sets = exercise.sets,
                                measure = exercise.measure
                            )
                        }
                    }
                    .take(HISTORY_DEPTH)
            )
    }
}
