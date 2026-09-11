package com.jericx.trainr.domain.model

data class WeeklyWorkoutPlan(
    val id: Long = 0,
    val userId: Long,
    val weekNumber: Int,
    val title: String,
    // Local midnight of the Monday the week begins; a day's date is this plus
    // dayNumber - 1 days. Null on plans stored before the column existed.
    val startDateMillis: Long? = null,
    val workoutDays: List<WorkoutDay>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class WorkoutDay(
    val id: Long = 0,
    val dayNumber: Int,
    val title: String,
    val status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    val duration: Int,
    val exerciseCount: Int,
    val equipment: List<String>,
    val exercises: List<WorkoutExercise> = emptyList(),
    val completedAt: Long? = null
)

data class WorkoutExercise(
    val id: Long = 0,
    // Canonical slug (goblet_squat) that history is matched on. The display
    // name may vary between weeks and locales; this must not.
    val exerciseKey: String = "",
    val name: String,
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    val sets: List<ExerciseSet> = emptyList(),
    val setCount: Int? = null,
    val reps: String? = null,
    val duration: String? = null,
    // Independent: ten minutes of "5 sets of 1 minute" is not five minutes, so
    // neither can be derived from the other.
    val durationMinutes: Int = 0,
    val prescription: String = "",
    val restTime: Int? = null,
    val equipment: List<String> = emptyList(),
    val instructions: String = "",
    val videoTutorialUrl: String? = null,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

data class ExerciseSet(
    val id: Long = 0,
    val setNumber: Int,
    val targetReps: Int? = null,
    val targetWeightKg: Float? = null,
    val targetSeconds: Int? = null,
    val actualReps: Int? = null,
    val actualWeightKg: Float? = null,
    val actualSeconds: Int? = null,
    val isCompleted: Boolean = false
)

enum class ExerciseMeasure {
    WEIGHT_AND_REPS,
    REPS,
    DURATION
}

enum class WorkoutStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
