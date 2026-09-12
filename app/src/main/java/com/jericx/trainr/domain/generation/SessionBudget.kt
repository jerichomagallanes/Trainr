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
    fun weeklySetsPerMuscle(user: UserProfile): Int = when {
        user.fitnessGoal == FitnessGoal.MUSCLE_GAIN ||
            user.fitnessGoal == FitnessGoal.STRENGTH -> if (user.workoutDaysPerWeek >= 3) 10 else 6
        else -> 6
    }
}
