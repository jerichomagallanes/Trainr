package com.jericx.trainr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM users LIMIT 1)")
    suspend fun hasUsers(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyWorkoutPlan(plan: WeeklyWorkoutPlanEntity): Long

    @Query("SELECT * FROM weekly_workout_plans WHERE userId = :userId ORDER BY weekNumber DESC")
    fun getWeeklyWorkoutPlans(userId: Long): Flow<List<WeeklyWorkoutPlanEntity>>

    @Query("SELECT * FROM weekly_workout_plans WHERE userId = :userId AND weekNumber = :weekNumber")
    suspend fun getWeeklyWorkoutPlan(userId: Long, weekNumber: Int): WeeklyWorkoutPlanEntity?

    @Update
    suspend fun updateWeeklyWorkoutPlan(plan: WeeklyWorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDay(day: WorkoutDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>)

    @Query("SELECT * FROM workout_days WHERE weeklyPlanId = :weeklyPlanId ORDER BY dayNumber")
    suspend fun getWorkoutDaysForPlan(weeklyPlanId: Long): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    suspend fun getWorkoutDayById(dayId: Long): WorkoutDayEntity?

    @Update
    suspend fun updateWorkoutDay(day: WorkoutDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Query("SELECT * FROM workout_exercises WHERE workoutDayId = :workoutDayId")
    suspend fun getExercisesForWorkoutDay(workoutDayId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :exerciseId")
    suspend fun getWorkoutExerciseById(exerciseId: Long): WorkoutExerciseEntity?

    @Update
    suspend fun updateWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutDayId = :workoutDayId")
    suspend fun getTotalExercisesForDay(workoutDayId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutDayId = :workoutDayId AND isCompleted = 1")
    suspend fun getCompletedExercisesForDay(workoutDayId: Long): Int
}
