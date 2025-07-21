package com.jericx.trainr.domain.repository

import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyProgress
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutDayProgress
import com.jericx.trainr.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun saveUser(user: UserProfile): Long
    suspend fun getUser(userId: Long): UserProfile?
    suspend fun getCurrentUser(): UserProfile?
    suspend fun updateUser(user: UserProfile)
    suspend fun hasUsers(): Boolean

    suspend fun saveWeeklyWorkoutPlan(plan: WeeklyWorkoutPlan): Long
    fun getWeeklyWorkoutPlans(userId: Long): Flow<List<WeeklyWorkoutPlan>>
    suspend fun getWeeklyWorkoutPlan(userId: Long, weekNumber: Int): WeeklyWorkoutPlan?
    suspend fun updateWeeklyWorkoutPlan(plan: WeeklyWorkoutPlan)

    suspend fun saveWorkoutDay(day: WorkoutDay, weeklyPlanId: Long): Long
    suspend fun saveWorkoutDays(days: List<WorkoutDay>, weeklyPlanId: Long)
    suspend fun getWorkoutDaysForPlan(weeklyPlanId: Long): List<WorkoutDay>
    suspend fun getWorkoutDay(dayId: Long): WorkoutDay?
    suspend fun updateWorkoutDay(day: WorkoutDay, weeklyPlanId: Long)

    suspend fun saveWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long): Long
    suspend fun saveWorkoutExercises(exercises: List<WorkoutExercise>, workoutDayId: Long)
    suspend fun getExercisesForWorkoutDay(workoutDayId: Long): List<WorkoutExercise>
    suspend fun getWorkoutExercise(exerciseId: Long): WorkoutExercise?
    suspend fun updateWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long)

    suspend fun getWeeklyProgress(userId: Long, weekNumber: Int): WeeklyProgress?
    suspend fun getWorkoutDayProgress(weeklyPlanId: Long): List<WorkoutDayProgress>
}