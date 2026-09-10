package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import kotlin.math.max

// A session is a time budget, and rest spends most of it. Heavy strength work
// asks 3-5 minutes between sets (ACSM 2009; Schoenfeld 2016), so half an hour
// buys six working sets, not the dozen a model will happily write. Working the
// count out here turns "sum close to the session length" - three numbers the
// model has to keep in agreement - into one number it is handed.
object SessionBudget {

    // Warm-up, changing, and the walk between stations.
    private const val OVERHEAD_MINUTES = 6

    // A set of 8-12 at the moderate velocity ACSM asks for, about 3 s a rep.
    private const val WORK_SECONDS_PER_SET = 40

    private const val FLOOR_SETS = 4

    // The nine regions volume is counted over, and what a set is worth across
    // them: one for the muscle the movement trains and half for each it
    // assists, which the catalog names. Averaged over the catalog that is
    // about three halves a set.
    private const val TRAINABLE_REGIONS = 9
    private const val REGION_SETS_PER_SET_HALVES = 3

    // The minimum effective dose (Iversen 2021). A week that cannot pay for
    // it is a real answer, not a number to round up to.
    const val MINIMUM_WEEKLY_SETS = 4

    fun restSeconds(goal: FitnessGoal): Int = when (goal) {
        FitnessGoal.STRENGTH -> 180
        FitnessGoal.MUSCLE_GAIN -> 120
        FitnessGoal.GENERAL_FITNESS -> 90
        FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> 45
        FitnessGoal.FLEXIBILITY -> 30
    }

    fun maxSetsPerSession(user: UserProfile): Int {
        val usableSeconds = (user.workoutDuration - OVERHEAD_MINUTES) * 60
        val perSet = WORK_SECONDS_PER_SET + restSeconds(user.fitnessGoal)
        return max(FLOOR_SETS, usableSeconds / perSet)
    }

    // The floor worth programming is 4 hard sets per muscle group per week
    // (Iversen 2021); growth keeps improving up to 10 and beyond (Schoenfeld
    // 2017), which only fits once there are days to spread it over.
    //
    // Capped by what the sessions can actually hold. Asked for ten where the
    // week pays for five, a model has to break either this or the session cap,
    // and only one of the two is checked - so the target became the rule that
    // was always quietly dropped.
    fun weeklySetsPerMuscle(user: UserProfile): Int {
        val ideal = when {
            user.fitnessGoal == FitnessGoal.MUSCLE_GAIN ||
                user.fitnessGoal == FitnessGoal.STRENGTH ->
                if (user.workoutDaysPerWeek >= 3) 10 else 6
            else -> 6
        }
        val setsInTheWeek = maxSetsPerSession(user) * user.workoutDaysPerWeek
        val affordable =
            setsInTheWeek * REGION_SETS_PER_SET_HALVES / 2 / TRAINABLE_REGIONS
        return affordable.coerceIn(1, ideal)
    }

    // Where even the minimum dose does not fit, the honest instruction is to
    // spend the week on movements that cover the most ground, not to chase a
    // target the client has no time for.
    fun coversEveryRegion(user: UserProfile): Boolean =
        weeklySetsPerMuscle(user) >= MINIMUM_WEEKLY_SETS

    // The session length is what the client answered; this is the point past
    // which the day is no longer that session. Half again as long as "about
    // 45 minutes" is not about 45 minutes.
    fun sessionCeilingMinutes(user: UserProfile): Int = user.workoutDuration * 3 / 2
}
