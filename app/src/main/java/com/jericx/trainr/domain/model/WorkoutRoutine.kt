package com.jericx.trainr.domain.model

data class WeeklyWorkoutPlan(
    val id: Long = 0,
    val userId: Long,
    val weekNumber: Int,
    val title: String = "YOUR WEEKLY WORKOUT PLAN",
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
    val sets: Int? = null,
    val reps: String? = null,
    val duration: String? = null,
    val restTime: Int? = null,
    val equipment: List<String> = emptyList(),
    val instructions: String = "",
    val videoTutorialUrl: String? = null,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

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
