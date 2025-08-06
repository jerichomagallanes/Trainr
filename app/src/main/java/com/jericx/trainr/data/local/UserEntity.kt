package com.jericx.trainr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

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
    @TypeConverters(Converters::class)
    val availableEquipment: List<String>,
    val workoutDaysPerWeek: Int,
    val workoutDuration: Int,
    val preferredWorkoutTime: String,
    @TypeConverters(Converters::class)
    val injuries: List<String>,
    val workoutType: String,
    val createdAt: Long
)

@Entity(tableName = "weekly_workout_plans")
data class WeeklyWorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val weekNumber: Int,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "workout_days")
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weeklyPlanId: Long,
    val dayNumber: Int,
    val title: String,
    val status: String,
    val duration: Int,
    val exerciseCount: Int,
    @TypeConverters(Converters::class)
    val equipment: List<String>,
    val completedAt: Long?
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val name: String,
    val sets: Int?,
    val reps: String?,
    val duration: String?,
    val restTime: Int?,
    @TypeConverters(Converters::class)
    val equipment: List<String>,
    val instructions: String,
    val videoTutorialUrl: String?,
    val isCompleted: Boolean,
    val notes: String
)
