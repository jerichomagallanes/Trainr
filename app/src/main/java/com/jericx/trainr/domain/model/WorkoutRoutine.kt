package com.jericx.trainr.domain.model

data class WeeklyWorkoutPlan(
    val id: Long = 0,
    val userId: Long,
    val weekNumber: Int,
    val title: String,
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
    val name: String,
    // How this exercise is measured, and so which columns its sets show.
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    val sets: List<ExerciseSet> = emptyList(),
    val setCount: Int? = null,
    val reps: String? = null,
    val duration: String? = null,
    // What the card shows: how long the exercise is allotted, and the
    // prescription beside it. The two are independent — ten minutes of "5 sets
    // of 1 minute" is not five minutes — so neither can be derived.
    val durationMinutes: Int = 0,
    val prescription: String = "",
    val restTime: Int? = null,
    val equipment: List<String> = emptyList(),
    val instructions: String = "",
    val videoTutorialUrl: String? = null,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

// A prescription is what the plan asks for; a log is what you did. Both live on
// the same row so the card can show the target and record the result beside it.
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

data class WeeklyProgress(
    val weekNumber: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val completionPercentage: Float,
    val workoutDays: List<WorkoutDayProgress>
)

data class WorkoutDayProgress(
    val dayNumber: Int,
    val title: String,
    val status: WorkoutStatus,
    val completionPercentage: Float,
    val completedExercises: Int,
    val totalExercises: Int
)

enum class WorkoutStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
