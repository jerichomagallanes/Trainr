package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

// A lighter week, triggered rather than scheduled. The one controlled test of
// a planned mid-programme week off found it worse than training through
// (Coleman 2024), and practitioners deload reactively, by cutting volume
// (Rogerson 2024). Both are small trials, so every threshold here is meant to
// be tuned.
object DeloadCheck {

    internal const val WEEKS_BETWEEN_DELOADS = 6
    internal const val WEEKS_BETWEEN_DELOADS_OLDER = 4
    internal const val OLDER_AGE = 50
    internal const val LOW_COMPLETION = 0.60f
    internal const val BEGINNER_GRACE_WEEKS = 8
    internal const val STALLING_MOVEMENTS = 2

    // Any two of three, because each alone is noise: one bad week, one busy
    // fortnight, or simply time passing.
    fun isDue(user: UserProfile, weeks: List<WeeklyWorkoutPlan>): Boolean {
        if (weeks.isEmpty()) return false
        if (user.experienceLevel == ExperienceLevel.BEGINNER && weeks.size < BEGINNER_GRACE_WEEKS) {
            return false
        }
        val newestFirst = weeks.sortedByDescending { it.weekNumber }
        val triggers = listOf(
            isStalling(newestFirst),
            isRarelyFinished(newestFirst),
            isLongSinceALighterWeek(user, newestFirst)
        )
        return triggers.count { it } >= 2
    }

    private fun isStalling(newestFirst: List<WeeklyWorkoutPlan>): Boolean {
        val keys = newestFirst.first().workoutDays.flatMap { day -> day.exercises.map { it.exerciseKey } }
            .distinct()
        return keys.count { ExerciseHistory.from(newestFirst, it).stallCount >= 1 } >= STALLING_MOVEMENTS
    }

    private fun isRarelyFinished(newestFirst: List<WeeklyWorkoutPlan>): Boolean {
        val sets = newestFirst.take(2).flatMap { week ->
            week.workoutDays.flatMap { day -> day.exercises.flatMap { it.sets } }
        }
        if (sets.isEmpty()) return false
        return sets.count { it.isCompleted }.toFloat() / sets.size < LOW_COMPLETION
    }

    // Weeks since the load last came down on anything, which a deload or a
    // stall both do. Not "weeks of rises": under a three-rung ladder every
    // movement rises on the same week, so consecutive rises never get long.
    private fun isLongSinceALighterWeek(user: UserProfile, newestFirst: List<WeeklyWorkoutPlan>): Boolean {
        val threshold = if (user.age >= OLDER_AGE) WEEKS_BETWEEN_DELOADS_OLDER else WEEKS_BETWEEN_DELOADS
        val since = newestFirst.zipWithNext().indexOfFirst { (week, before) -> week.isLighterThan(before) }
            .let { if (it < 0) newestFirst.size else it }
        return since >= threshold
    }

    private fun WeeklyWorkoutPlan.isLighterThan(before: WeeklyWorkoutPlan): Boolean {
        val then = before.firstTargets()
        return firstTargets().any { (key, now) ->
            val previous = then[key] ?: return@any false
            val lighter = now.first != null && previous.first != null && now.first!! < previous.first!!
            lighter || now.second < previous.second
        }
    }

    // Each movement's first-set load and its set count, by key.
    private fun WeeklyWorkoutPlan.firstTargets(): Map<String, Pair<Float?, Int>> =
        workoutDays.flatMap { it.exercises }
            .associate { it.exerciseKey to (it.sets.firstOrNull()?.targetWeightKg to it.sets.size) }
}
