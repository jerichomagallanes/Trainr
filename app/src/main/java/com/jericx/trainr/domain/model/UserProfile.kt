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
    val injuries: List<Injury> = emptyList(),
    val workoutType: WorkoutType = WorkoutType.MIXED,
    // How the client reads their own body; storage stays metric either way.
    val bodyUnitSystem: UnitSystem = UnitSystem.Default,
    // What the plates in their gym are marked in, a separate question from the
    // above. Null until there is loaded equipment to ask about.
    val liftingUnitSystem: UnitSystem? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val weightUnits: UnitSystem get() = liftingUnitSystem ?: bodyUnitSystem
}

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
    MACHINES,
    CARDIO_MACHINES,
    MAT,
    JUMP_ROPE
}

// Everything a gym has that a home might not, and the other way round. A
// client who trains in both places has both, which is why BOTH is the union
// and not the gym list.
val HomeEquipment = listOf(
    Equipment.NONE,
    Equipment.DUMBBELLS,
    Equipment.KETTLEBELLS,
    Equipment.RESISTANCE_BANDS,
    Equipment.PULL_UP_BAR,
    Equipment.BENCH,
    Equipment.MAT,
    Equipment.JUMP_ROPE,
    Equipment.BARBELL,
    Equipment.SQUAT_RACK,
    Equipment.CARDIO_MACHINES
)

val GymEquipment = listOf(
    Equipment.BARBELL,
    Equipment.SQUAT_RACK,
    Equipment.BENCH,
    Equipment.DUMBBELLS,
    Equipment.KETTLEBELLS,
    Equipment.CABLE_MACHINE,
    Equipment.MACHINES,
    Equipment.PULL_UP_BAR,
    Equipment.RESISTANCE_BANDS,
    Equipment.CARDIO_MACHINES,
    Equipment.MAT
)

fun equipmentFor(location: WorkoutLocation): List<Equipment> = when (location) {
    WorkoutLocation.HOME -> HomeEquipment
    WorkoutLocation.GYM -> GymEquipment
    WorkoutLocation.BOTH -> HomeEquipment.filterNot { it == Equipment.NONE } +
        GymEquipment.filterNot { it in HomeEquipment }
}

enum class WorkoutType {
    STRENGTH,
    CARDIO,
    HIIT,
    YOGA,
    MIXED
}

// Stored and sent as these constants, never as the words on the chip: a
// profile filled in Japanese used to reach the model as Japanese injury names,
// and stopped matching its own chips the moment the phone changed language.
enum class Injury {
    LOWER_BACK,
    KNEE,
    SHOULDER,
    WRIST,
    ANKLE,
    HIP,
    NECK
}

enum class WorkoutTime {
    EARLY_MORNING,
    MORNING,
    AFTERNOON,
    EVENING,
    ANYTIME
}
