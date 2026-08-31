package com.jericx.trainr.common

object Constants {
    const val DATABASE_NAME = "trainr_database"

    // Workout Constants
    object Workout {
        const val DEFAULT_WORKOUT_DURATION = 45
        const val DEFAULT_WORKOUT_DAYS_PER_WEEK = 3

        // BMI Categories
        const val BMI_UNDERWEIGHT_THRESHOLD = 18.5f
        const val BMI_NORMAL_THRESHOLD = 25f
        const val BMI_OVERWEIGHT_THRESHOLD = 30f

        // The ages the plans are written for. Under 13 is not who this is for,
        // and past 100 is a typo rather than a client.
        const val MIN_AGE = 13
        const val MAX_AGE = 100

        // What a person can plausibly be. Anything outside this is a typo, and
        // it reaches the model as the body it plans around.
        const val MIN_HEIGHT_CM = 100f
        const val MAX_HEIGHT_CM = 250f
        const val MIN_WEIGHT_KG = 30f
        const val MAX_WEIGHT_KG = 300f

        // Metric/Imperial conversion factors
        const val CM_TO_INCHES = 2.54f
        const val INCHES_PER_FOOT = 12f
        const val KG_TO_LBS = 2.20462f

        // Duration options for workouts
        val DURATION_OPTIONS = listOf(30, 45, 60, 90)

        // Days per week options
        val DAYS_PER_WEEK_OPTIONS = (1..7).toList()
    }
}
