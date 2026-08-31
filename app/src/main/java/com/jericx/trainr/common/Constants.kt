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

        // 13 is a legal floor rather than a claim about who can train: it is the
        // line COPPA and Play's Families policy draw around collecting personal
        // data, and this app asks for an age, a height and a weight.
        const val MIN_AGE = 13

        // Above the oldest person ever verified, who reached 122.
        const val MAX_AGE = 125

        // Set outside every human on record, so the check refuses typos and
        // never a person. The earlier bounds of 100-250 cm and 30-300 kg each
        // excluded people who exist: adults with dwarfism, the tallest man
        // alive at 251 cm, and anyone above 300 kg, who are exactly the people
        // a fitness app should not be turning away.
        //
        // Verified extremes: tallest ever 272 cm, shortest ever measured
        // 54.6 cm, heaviest ever 635 kg.
        const val MIN_HEIGHT_CM = 50f
        const val MAX_HEIGHT_CM = 275f
        const val MIN_WEIGHT_KG = 20f
        const val MAX_WEIGHT_KG = 650f

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
