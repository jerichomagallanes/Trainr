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

// The equipment vocabulary the exercise catalog is categorised by, and the
// only vocabulary the setup screen asks about. One tag per movement: what a
// bench press is done with is the bar, and the bench is part of doing it.
enum class Equipment {
    NONE,
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    MACHINE,
    PLATE,
    RESISTANCE_BAND,
    SUSPENSION_BAND,
    OTHER
}

// Asked in the catalog's own order. "No equipment" is an answer only where
// there might be none; at a gym it is not a thing anyone means.
val EquipmentChoices = listOf(
    Equipment.NONE,
    Equipment.BARBELL,
    Equipment.DUMBBELL,
    Equipment.KETTLEBELL,
    Equipment.MACHINE,
    Equipment.PLATE,
    Equipment.RESISTANCE_BAND,
    Equipment.SUSPENSION_BAND,
    Equipment.OTHER
)

// A profile saved before the catalog settled on nine categories still names
// the old finer-grained kit. Dropping those would quietly empty someone's
// equipment and hand them a bodyweight plan without saying why.
private val LegacyEquipment = mapOf(
    "DUMBBELLS" to Equipment.DUMBBELL,
    "KETTLEBELLS" to Equipment.KETTLEBELL,
    "RESISTANCE_BANDS" to Equipment.RESISTANCE_BAND,
    "MACHINES" to Equipment.MACHINE,
    "CABLE_MACHINE" to Equipment.MACHINE,
    "CARDIO_MACHINES" to Equipment.MACHINE,
    "PULL_UP_BAR" to Equipment.MACHINE,
    "SQUAT_RACK" to Equipment.BARBELL,
    "BENCH" to Equipment.OTHER,
    "JUMP_ROPE" to Equipment.OTHER,
    "OTHERS" to Equipment.OTHER,
    "MAT" to Equipment.NONE
)

fun storedEquipment(raw: String): Equipment? =
    runCatching { Equipment.valueOf(raw) }.getOrNull() ?: LegacyEquipment[raw]

// A chip the catalog cannot serve is a lie: the client ticks it, the shortlist
// comes back empty, and the plan is built from nothing. What is offered is
// what there are movements for.
fun equipmentFor(
    location: WorkoutLocation,
    stocked: Set<Equipment> = EquipmentChoices.toSet()
): List<Equipment> = EquipmentChoices
    .filter { it in stocked }
    .filterNot { it == Equipment.NONE && location != WorkoutLocation.HOME }

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
