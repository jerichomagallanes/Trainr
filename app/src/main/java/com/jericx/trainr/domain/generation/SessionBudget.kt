package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import kotlin.math.max

// A session is a time budget, and rest spends most of it. Heavy strength work
// asks 3-5 minutes between sets (ACSM 2009; Schoenfeld 2016), so half an hour
// buys far fewer sets than a circuit does.
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

    // A bound on volume, not the measure of the day. Rest falls between the
    // sets of a movement, not after its last one, where the walk to the next
    // station replaces it; counted after every set this stops being a bound
    // and becomes the limit a day halts at, well short of the answer.
    fun maxSetsPerSession(user: UserProfile): Int {
        val movements = SessionShape.forGoal(user.fitnessGoal).slotCount
        val rest = restSeconds(user.fitnessGoal, ExerciseRole.ISOLATION)
        val usableSeconds = (user.workoutDuration - OVERHEAD_MINUTES) * 60 -
            (movements - 1) * SessionMinutes.TRANSITION_SECONDS + movements * rest
        return max(FLOOR_SETS, usableSeconds / (WORK_SECONDS_PER_SET + rest))
    }

    // The session length is what the client answered; this is the point past
    // which the day is no longer that session. Half again as long as "about
    // 45 minutes" is not about 45 minutes.
    fun sessionCeilingMinutes(user: UserProfile): Int = user.workoutDuration * 3 / 2

    private const val TIMED_REST = 30
    private const val REST_GRANULARITY = 15
}
