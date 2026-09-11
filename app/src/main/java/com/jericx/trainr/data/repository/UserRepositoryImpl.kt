package com.jericx.trainr.data.repository

import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val mapper: UserMapper
) : UserRepository {

    override suspend fun saveUser(user: UserProfile): Long {
        return userDao.insertUser(mapper.mapToEntity(user))
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
        saveWorkoutDays(plan.workoutDays, planId)
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

        saveWorkoutExercises(day.exercises, dayId)

        return dayId
    }

    override suspend fun saveWorkoutDays(days: List<WorkoutDay>, weeklyPlanId: Long) {
        val dayEntities = days.map { day ->
            mapper.mapToEntity(day, weeklyPlanId)
        }
        val dayIds = userDao.insertWorkoutDays(dayEntities)

        // One exercise at a time: the sets need the generated exercise id,
        // which a bulk insert does not hand back.
        days.forEachIndexed { dayIndex, day ->
            saveWorkoutExercises(day.exercises, dayIds[dayIndex])
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

    override suspend fun deleteWeeklyWorkoutPlan(planId: Long) {
        userDao.deleteWeeklyWorkoutPlan(planId)
    }

    override suspend fun updateWorkoutDay(day: WorkoutDay, weeklyPlanId: Long) {
        userDao.updateWorkoutDay(mapper.mapToEntity(day, weeklyPlanId))
    }

    override suspend fun saveWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long): Long {
        val exerciseId = userDao.insertWorkoutExercise(mapper.mapToEntity(exercise, workoutDayId))
        saveSetsFor(exercise, exerciseId)
        return exerciseId
    }

    override suspend fun saveWorkoutExercises(exercises: List<WorkoutExercise>, workoutDayId: Long) {
        exercises.forEach { saveWorkoutExercise(it, workoutDayId) }
    }

    override suspend fun getExercisesForWorkoutDay(workoutDayId: Long): List<WorkoutExercise> {
        val exerciseEntities = userDao.getExercisesForWorkoutDay(workoutDayId)
        return exerciseEntities.map { entity ->
            mapper.mapToDomain(entity).copy(sets = setsFor(entity.id))
        }
    }

    override suspend fun getWorkoutExercise(exerciseId: Long): WorkoutExercise? {
        return userDao.getWorkoutExerciseById(exerciseId)?.let {
            mapper.mapToDomain(it).copy(sets = setsFor(it.id))
        }
    }

    override suspend fun updateExerciseSet(set: ExerciseSet, workoutExerciseId: Long) {
        userDao.updateExerciseSet(mapper.mapToEntity(set, workoutExerciseId))
    }

    override suspend fun addExerciseSet(set: ExerciseSet, workoutExerciseId: Long): Long {
        return userDao.insertExerciseSet(mapper.mapToEntity(set, workoutExerciseId))
    }

    override suspend fun deleteExerciseSet(setId: Long) {
        userDao.deleteExerciseSet(setId)
    }

    override suspend fun getPreviousSets(
        userId: Long,
        exerciseKey: String,
        excludeDayId: Long,
        beforeMillis: Long
    ): List<ExerciseSet> {
        return userDao.getPreviousExerciseSets(userId, exerciseKey, excludeDayId, beforeMillis)
            .map { mapper.mapToDomain(it) }
    }

    private suspend fun saveSetsFor(exercise: WorkoutExercise, exerciseId: Long) {
        if (exercise.sets.isEmpty()) return

        userDao.insertExerciseSets(exercise.sets.map { mapper.mapToEntity(it, exerciseId) })
    }

    private suspend fun setsFor(exerciseId: Long): List<ExerciseSet> =
        userDao.getSetsForExercise(exerciseId).map { mapper.mapToDomain(it) }

    override suspend fun updateWorkoutExercise(exercise: WorkoutExercise, workoutDayId: Long) {
        userDao.updateWorkoutExercise(mapper.mapToEntity(exercise, workoutDayId))
    }
}
