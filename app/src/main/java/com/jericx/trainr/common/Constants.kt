package com.jericx.trainr.common

object Constants {
    const val KEY_SHARED_PREF = "trainr_preferences"
    const val USER_PROFILE_KEY = "user_profile"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val DATABASE_NAME = "trainr_database"
    const val DARK_MODE = "dark_mode"
    
    // Workout Constants
    object Workout {
        const val DEFAULT_WORKOUT_DURATION = 45
        const val DEFAULT_WORKOUT_DAYS_PER_WEEK = 3
        
        // BMI Categories
        const val BMI_UNDERWEIGHT_THRESHOLD = 18.5f
        const val BMI_NORMAL_THRESHOLD = 25f
        const val BMI_OVERWEIGHT_THRESHOLD = 30f
        
        // Metric/Imperial conversion factors
        const val CM_TO_INCHES = 2.54f
        const val INCHES_PER_FOOT = 12f
        const val KG_TO_LBS = 2.20462f
        
        // Duration options for workouts
        val DURATION_OPTIONS = listOf(30, 45, 60, 90)
        
        // Days per week options
        val DAYS_PER_WEEK_OPTIONS = (1..7).toList()
        
        // Common injury types
        val COMMON_INJURIES = listOf(
            "Lower Back Pain",
            "Knee Problems", 
            "Shoulder Injury",
            "Wrist Pain",
            "Ankle Issues",
            "Hip Problems",
            "Neck Pain",
            "None"
        )
    }
}
