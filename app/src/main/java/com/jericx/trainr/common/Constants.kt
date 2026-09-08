package com.jericx.trainr.common

object Constants {
    const val DATABASE_NAME = "trainr_database"

    object Workout {
        const val DEFAULT_WORKOUT_DURATION = 45
        const val DEFAULT_WORKOUT_DAYS_PER_WEEK = 3

        const val BMI_UNDERWEIGHT_THRESHOLD = 18.5f
        const val BMI_NORMAL_THRESHOLD = 25f
        const val BMI_OVERWEIGHT_THRESHOLD = 30f

        // 13 is the legal floor COPPA and Play's Families policy draw around
        // collecting personal data, not a claim about who can train.
        const val MIN_AGE = 13

        // Past the oldest human ever verified, who reached 122.
        const val MAX_AGE = 125

        // Set outside every human on record (tallest 272 cm, shortest 54.6 cm,
        // heaviest 635 kg) so validation refuses typos, never people.
        const val MIN_HEIGHT_CM = 50f
        const val MAX_HEIGHT_CM = 275f
        const val MIN_WEIGHT_KG = 20f
        const val MAX_WEIGHT_KG = 650f

        const val CM_TO_INCHES = 2.54f
        const val INCHES_PER_FOOT = 12f
        const val KG_TO_LBS = 2.20462f

        val DURATION_OPTIONS = listOf(30, 45, 60, 90)

        val DAYS_PER_WEEK_OPTIONS = (1..7).toList()
    }
}
