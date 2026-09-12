package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import kotlin.math.max

// A session is a time budget, and rest spends most of it. Heavy strength work
// asks 3-5 minutes between sets (ACSM 2009; Schoenfeld 2016), so half an hour
// buys six working sets.
object SessionBudget {

    // Warm-up, changing, and the walk between stations.
    private const val OVERHEAD_MINUTES = 6

    // A set of 8-12 at the moderate velocity ACSM asks for, about 3 s a rep.
    private const val WORK_SECONDS_PER_SET = 40

    private const val FLOOR_SETS = 4

    fun restSeconds(goal: FitnessGoal): Int = when (goal) {
        FitnessGoal.STRENGTH -> 180
        FitnessGoal.MUSCLE_GAIN -> 120
        FitnessGoal.GENERAL_FITNESS -> 90
        FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> 45
        FitnessGoal.FLEXIBILITY -> 30
    }

    // Isolation work does not need the three minutes a heavy compound does;
    // the existing brief already asked for 90-120 on multi-joint and 60-90 on
    // isolation, and this is that, worked out rather than written out.
    fun restSeconds(goal: FitnessGoal, role: ExerciseRole): Int = when (role) {
        ExerciseRole.TIMED -> TIMED_REST
        ExerciseRole.COMPOUND -> restSeconds(goal)
        ExerciseRole.ISOLATION -> {
            val shorter = restSeconds(goal) * 3 / 4
            maxOf(shorter / REST_GRANULARITY * REST_GRANULARITY, TIMED_REST)
        }
    }

    fun maxSetsPerSession(user: UserProfile): Int {
        val usableSeconds = (user.workoutDuration - OVERHEAD_MINUTES) * 60
        val perSet = WORK_SECONDS_PER_SET + restSeconds(user.fitnessGoal)
        return max(FLOOR_SETS, usableSeconds / perSet)
    }

    // The session length is what the client answered; this is the point past
    // which the day is no longer that session. Half again as long as "about
    // 45 minutes" is not about 45 minutes.
    fun sessionCeilingMinutes(user: UserProfile): Int = user.workoutDuration * 3 / 2

    private const val TIMED_REST = 30
    private const val REST_GRANULARITY = 15
}
