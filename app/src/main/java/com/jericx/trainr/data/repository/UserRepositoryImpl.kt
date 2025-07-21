package com.jericx.trainr.data.repository

import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyProgress
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutDayProgress
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val mapper: UserMapper
) : UserRepository {

    override suspend fun saveUser(user: UserProfile): Long {
        return userDao.insertUser(mapper.mapToEntity(user))
    }

    override suspend fun getUser(userId: Long): UserProfile? {
        return userDao.getUserById(userId)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun getCurrentUser(): UserProfile? {
        return userDao.getCurrentUser()?.let { mapper.mapToDomain(it) }
    }

    override suspend fun updateUser(user: UserProfile) {
        userDao.updateUser(mapper.mapToEntity(user))
    }

    override suspend fun hasUsers(): Boolean {
        return userDao.hasUsers()
    }

    override suspend fun saveWeeklyWorkoutPlan(plan: WeeklyWorkoutPlan): Long {
        val planId = userDao.insertWeeklyWorkoutPlan(mapper.mapToEntity(plan))

        val dayEntities = plan.workoutDays.map { day ->
            mapper.mapToEntity(day, planId)
        }
        userDao.insertWorkoutDays(dayEntities)

        plan.workoutDays.forEachIndexed { dayIndex, day ->
            val dayId = dayEntities[dayIndex].id
            val exerciseEntities = day.exercises.map { exercise ->
                mapper.mapToEntity(exercise, dayId)
            }
            if (exerciseEntities.isNotEmpty()) {
                userDao.insertWorkoutExercises(exerciseEntities)
            }
        }
        
        return planId
    }

    override fun getWeeklyWorkoutPlans(userId: Long): Flow<List<WeeklyWorkoutPlan>> {
        return userDao.getWeeklyWorkoutPlans(userId).map { planEntities ->
            planEntities.map { planEntity ->
                val days = getWorkoutDaysForPlan(planEntity.id)
                mapper.mapToDomain(planEntity, days)
            }
        }
    }

    override suspend fun getWeeklyWorkoutPlan(userId: Long, weekNumber: Int): WeeklyWorkoutPlan? {
        val planEntity = userDao.getWeeklyWorkoutPlan(userId, weekNumber) ?: return null
        val days = getWorkoutDaysForPlan(planEntity.id)
        return mapper.mapToDomain(planEntity, days)
    }

    override suspend fun updateWeeklyWorkoutPlan(plan: WeeklyWorkoutPlan) {
        userDao.updateWeeklyWorkoutPlan(mapper.mapToEntity(plan))
    }

    override suspend fun saveWorkoutDay(day: WorkoutDay, weeklyPlanId: Long): Long {
        val dayId = userDao.insertWorkoutDay(mapper.mapToEntity(day, weeklyPlanId))

        val exerciseEntities = day.exercises.map { exercise ->
            mapper.mapToEntity(exercise, dayId)
        }
        if (exerciseEntities.isNotEmpty()) {
            userDao.insertWorkoutExercises(exerciseEntities)
        }
        
        return dayId
    }

    override suspend fun saveWorkoutDays(days: List<WorkoutDay>, weeklyPlanId: Long) {
        val dayEntities = days.map { day ->
            mapper.mapToEntity(day, weeklyPlanId)
        }
        userDao.insertWorkoutDays(dayEntities)

        days.forEachIndexed { dayIndex, day ->
            val dayId = dayEntities[dayIndex].id
            val exerciseEntities = day.exercises.map { exercise ->
                mapper.mapToEntity(exercise, dayId)
            }
            if (exerciseEntities.isNotEmpty()) {
                userDao.insertWorkoutExercises(exerciseEntities)
            }
        }
    }

    override suspend fun getWorkoutDaysForPlan(weeklyPlanId: Long): List<WorkoutDay> {
        val dayEntities = userDao.getWorkoutDaysForPlan(weeklyPlanId)
        return dayEntities.map { dayEntity ->
            val exercises = getExercisesForWorkoutDay(dayEntity.id)
            mapper.mapToDomain(dayEntity, exercises)
        }
    }

    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? {
        val dayEntity = userDao.getWorkoutDayById(dayId) ?: return null
        val exercises = getExercisesForWorkoutDay(dayId)
        return mapper.mapToDomain(dayEntity, exercises)
    }

    override suspend fun updateWorkoutDay(day: WorkoutDay, weeklyPlanId: Long) {
        userDao.updateWorkoutDay(mapper.mapToEntity(day, weeklyPlanId))
    }

    override suspend fun saveWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long): Long {
        return userDao.insertWorkoutExercise(mapper.mapToEntity(exercise, workoutDayId))
    }

    override suspend fun saveWorkoutExercises(exercises: List<WorkoutExercise>, workoutDayId: Long) {
        val exerciseEntities = exercises.map { exercise ->
            mapper.mapToEntity(exercise, workoutDayId)
        }
        userDao.insertWorkoutExercises(exerciseEntities)
    }

    override suspend fun getExercisesForWorkoutDay(workoutDayId: Long): List<WorkoutExercise> {
        val exerciseEntities = userDao.getExercisesForWorkoutDay(workoutDayId)
        return exerciseEntities.map { mapper.mapToDomain(it) }
    }

    override suspend fun getWorkoutExercise(exerciseId: Long): WorkoutExercise? {
        return userDao.getWorkoutExerciseById(exerciseId)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun updateWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long) {
        userDao.updateWorkoutExercise(mapper.mapToEntity(exercise, workoutDayId))
    }

    override suspend fun getWeeklyProgress(userId: Long, weekNumber: Int): WeeklyProgress? {
        val plan = getWeeklyWorkoutPlan(userId, weekNumber) ?: return null
        val dayProgressList = getWorkoutDayProgress(plan.id)
        
        val completedWorkouts = dayProgressList.count { it.status == WorkoutStatus.COMPLETED }
        val totalWorkouts = dayProgressList.size
        val completionPercentage = if (totalWorkouts > 0) {
            (completedWorkouts.toFloat() / totalWorkouts.toFloat()) * 100f
        } else 0f
        
        return WeeklyProgress(
            weekNumber = weekNumber,
            completedWorkouts = completedWorkouts,
            totalWorkouts = totalWorkouts,
            completionPercentage = completionPercentage,
            workoutDays = dayProgressList
        )
    }

    override suspend fun getWorkoutDayProgress(weeklyPlanId: Long): List<WorkoutDayProgress> {
        val dayEntities = userDao.getWorkoutDaysForPlan(weeklyPlanId)
        return dayEntities.map { dayEntity ->
            val totalExercises = userDao.getTotalExercisesForDay(dayEntity.id)
            val completedExercises = userDao.getCompletedExercisesForDay(dayEntity.id)
            val completionPercentage = if (totalExercises > 0) {
                (completedExercises.toFloat() / totalExercises.toFloat()) * 100f
            } else 0f
            
            WorkoutDayProgress(
                dayNumber = dayEntity.dayNumber,
                title = dayEntity.title,
                status = WorkoutStatus.valueOf(dayEntity.status),
                completionPercentage = completionPercentage,
                completedExercises = completedExercises,
                totalExercises = totalExercises
            )
        }
    }
}