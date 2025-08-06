package com.jericx.trainr.domain.model

import com.jericx.trainr.common.Constants

data class UserProfile(
    val id: Long = 0,
    val firstName: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val height: Float = 0f,
    val weight: Float = 0f,
    val fitnessGoal: FitnessGoal = FitnessGoal.GENERAL_FITNESS,
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val workoutLocation: WorkoutLocation = WorkoutLocation.HOME,
    val availableEquipment: List<Equipment> = emptyList(),
    val workoutDaysPerWeek: Int = Constants.Workout.DEFAULT_WORKOUT_DAYS_PER_WEEK,
    val workoutDuration: Int = Constants.Workout.DEFAULT_WORKOUT_DURATION,
    val preferredWorkoutTime: WorkoutTime = WorkoutTime.ANYTIME,
    val injuries: List<String> = emptyList(),
    val workoutType: WorkoutType = WorkoutType.MIXED,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Gender {
    MALE,
    FEMALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY
}

enum class FitnessGoal {
    WEIGHT_LOSS,
    MUSCLE_GAIN,
    STRENGTH,
    ENDURANCE,
    GENERAL_FITNESS,
    FLEXIBILITY
}

enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class WorkoutLocation {
    HOME,
    GYM,
    BOTH
}

enum class Equipment {
    NONE,
    DUMBBELLS,
    BARBELL,
    BENCH,
    RESISTANCE_BANDS,
    PULL_UP_BAR,
    KETTLEBELLS,
    SQUAT_RACK,
    CABLE_MACHINE,
    CARDIO_MACHINES,
    OTHERS
}

enum class WorkoutType {
    STRENGTH,
    CARDIO,
    HIIT,
    YOGA,
    MIXED
}

enum class WorkoutTime {
    EARLY_MORNING,
    MORNING,
    AFTERNOON,
    EVENING,
    ANYTIME
}
