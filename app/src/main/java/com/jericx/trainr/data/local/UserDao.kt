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
    suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>): List<Long>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSet(set: ExerciseSetEntity): Long

    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId = :exerciseId ORDER BY setNumber")
    suspend fun getSetsForExercise(exerciseId: Long): List<ExerciseSetEntity>

    @Update
    suspend fun updateExerciseSet(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE id = :setId")
    suspend fun deleteExerciseSet(setId: Long)

    // The most recent completed performance of the same movement, matched on
    // exerciseKey — never on the display name, which is free to drift.
    @Query(
        """
        SELECT es.* FROM exercise_sets es
        WHERE es.workoutExerciseId = (
            SELECT we.id FROM workout_exercises we
            JOIN workout_days wd ON we.workoutDayId = wd.id
            JOIN weekly_workout_plans p ON wd.weeklyPlanId = p.id
            WHERE p.userId = :userId
              AND we.exerciseKey = :exerciseKey
              AND wd.id != :excludeDayId
              AND wd.status = 'COMPLETED'
              AND wd.completedAt IS NOT NULL
              AND wd.completedAt < :beforeMillis
            ORDER BY wd.completedAt DESC, wd.id DESC
            LIMIT 1
        )
        ORDER BY es.setNumber
        """
    )
    suspend fun getPreviousExerciseSets(
        userId: Long,
        exerciseKey: String,
        excludeDayId: Long,
        beforeMillis: Long
    ): List<ExerciseSetEntity>

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutDayId = :workoutDayId")
    suspend fun getTotalExercisesForDay(workoutDayId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutDayId = :workoutDayId AND isCompleted = 1")
    suspend fun getCompletedExercisesForDay(workoutDayId: Long): Int
}
