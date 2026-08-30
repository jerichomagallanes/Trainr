package com.jericx.trainr.testing

import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyProgress
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutDayProgress
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Enough of a repository to put one stored week in front of a route. Writes go
// nowhere: these tests are about what a screen is wired to, not what it stores.
class OneWeekRepository(private val plan: WeeklyWorkoutPlan) : UserRepository {

    override suspend fun getCurrentUser(): UserProfile? = UserProfile(id = 1)

    override fun getWeeklyWorkoutPlans(userId: Long): Flow<List<WeeklyWorkoutPlan>> =
        flowOf(listOf(plan))

    override suspend fun getWeeklyWorkoutPlan(userId: Long, weekNumber: Int) =
        plan.takeIf { it.weekNumber == weekNumber }

    override suspend fun saveUser(user: UserProfile): Long = 1
    override suspend fun getUser(userId: Long): UserProfile? = null
    override suspend fun updateUser(user: UserProfile) = Unit
    override suspend fun hasUsers(): Boolean = true

    override suspend fun saveWeeklyWorkoutPlan(plan: WeeklyWorkoutPlan): Long = 1
    override suspend fun updateWeeklyWorkoutPlan(plan: WeeklyWorkoutPlan) = Unit
    override suspend fun deleteWeeklyWorkoutPlan(planId: Long) = Unit

    override suspend fun saveWorkoutDay(day: WorkoutDay, weeklyPlanId: Long): Long = 1
    override suspend fun saveWorkoutDays(days: List<WorkoutDay>, weeklyPlanId: Long) = Unit
    override suspend fun getWorkoutDaysForPlan(weeklyPlanId: Long): List<WorkoutDay> = emptyList()
    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = null
    override suspend fun updateWorkoutDay(day: WorkoutDay, weeklyPlanId: Long) = Unit

    override suspend fun saveWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long): Long = 1
    override suspend fun saveWorkoutExercises(
        exercises: List<WorkoutExercise>,
        workoutDayId: Long
    ) = Unit

    override suspend fun getExercisesForWorkoutDay(workoutDayId: Long): List<WorkoutExercise> =
        emptyList()

    override suspend fun getWorkoutExercise(exerciseId: Long): WorkoutExercise? = null
    override suspend fun updateWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long) = Unit
    override suspend fun updateExerciseSet(set: ExerciseSet, workoutExerciseId: Long) = Unit
    override suspend fun addExerciseSet(set: ExerciseSet, workoutExerciseId: Long): Long = 1
    override suspend fun deleteExerciseSet(setId: Long) = Unit

    override suspend fun getPreviousSets(
        userId: Long,
        exerciseKey: String,
        excludeDayId: Long,
        beforeMillis: Long
    ): List<ExerciseSet> = emptyList()

    override suspend fun getWeeklyProgress(userId: Long, weekNumber: Int): WeeklyProgress? = null
    override suspend fun getWorkoutDayProgress(weeklyPlanId: Long): List<WorkoutDayProgress> =
        emptyList()
}
