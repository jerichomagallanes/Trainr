package com.jericx.trainr.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val age: Int,
    val gender: String,
    val height: Float,
    val weight: Float,
    val fitnessGoal: String,
    val experienceLevel: String,
    val workoutLocation: String,
    val availableEquipment: List<String>,
    val workoutDaysPerWeek: Int,
    val workoutDuration: Int,
    val preferredWorkoutTime: String,
    val injuries: List<String>,
    val workoutType: String,
    val unitSystem: String = "METRIC",
    val createdAt: Long
)

@Entity(
    tableName = "weekly_workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // A client has one week three. Enforced here because the check that used to
    // stand for it — read, then generate for half a minute, then write — leaves
    // room for a second generation to pass the same check before the first one
    // saves.
    indices = [
        Index("userId"),
        Index(value = ["userId", "weekNumber"], unique = true)
    ]
)
data class WeeklyWorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val weekNumber: Int,
    val title: String,
    val startDateMillis: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyWorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["weeklyPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weeklyPlanId")]
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weeklyPlanId: Long,
    val dayNumber: Int,
    val title: String,
    val status: String,
    val duration: Int,
    val exerciseCount: Int,
    val equipment: List<String>,
    val completedAt: Long?
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutDayId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val exerciseKey: String,
    val name: String,
    val measure: String,
    // The column predates the set table and still holds the prescribed count.
    @ColumnInfo(name = "sets")
    val setCount: Int?,
    val reps: String?,
    val duration: String?,
    val durationMinutes: Int,
    val prescription: String,
    val restTime: Int?,
    val equipment: List<String>,
    val instructions: String,
    val videoTutorialUrl: String?,
    val isCompleted: Boolean,
    val notes: String
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val targetReps: Int?,
    val targetWeightKg: Float?,
    val targetSeconds: Int?,
    val actualReps: Int?,
    val actualWeightKg: Float?,
    val actualSeconds: Int?,
    val isCompleted: Boolean
)
