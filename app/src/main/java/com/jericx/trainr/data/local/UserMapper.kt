package com.jericx.trainr.data.local

import com.jericx.trainr.domain.model.*

class UserMapper {

    fun mapToEntity(user: UserProfile): UserEntity {
        return UserEntity(
            id = user.id,
            age = user.age,
            gender = user.gender.name,
            height = user.height,
            weight = user.weight,
            fitnessGoal = user.fitnessGoal.name,
            experienceLevel = user.experienceLevel.name,
            workoutLocation = user.workoutLocation.name,
            availableEquipment = user.availableEquipment.map { it.name },
            workoutDaysPerWeek = user.workoutDaysPerWeek,
            workoutDuration = user.workoutDuration,
            preferredWorkoutTime = user.preferredWorkoutTime.name,
            injuries = user.injuries,
            workoutType = user.workoutType.name,
            createdAt = user.createdAt
        )
    }

    fun mapToDomain(entity: UserEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            age = entity.age,
            gender = Gender.valueOf(entity.gender),
            height = entity.height,
            weight = entity.weight,
            fitnessGoal = FitnessGoal.valueOf(entity.fitnessGoal),
            experienceLevel = ExperienceLevel.valueOf(entity.experienceLevel),
            workoutLocation = WorkoutLocation.valueOf(entity.workoutLocation),
            availableEquipment = entity.availableEquipment.mapNotNull {
                try { Equipment.valueOf(it) } catch (e: Exception) { null }
            },
            workoutDaysPerWeek = entity.workoutDaysPerWeek,
            workoutDuration = entity.workoutDuration,
            preferredWorkoutTime = WorkoutTime.valueOf(entity.preferredWorkoutTime),
            injuries = entity.injuries,
            workoutType = WorkoutType.valueOf(entity.workoutType),
            createdAt = entity.createdAt
        )
    }

    fun mapToEntity(plan: WeeklyWorkoutPlan): WeeklyWorkoutPlanEntity {
        return WeeklyWorkoutPlanEntity(
            id = plan.id,
            userId = plan.userId,
            weekNumber = plan.weekNumber,
            title = plan.title,
            createdAt = plan.createdAt,
            updatedAt = plan.updatedAt
        )
    }

    fun mapToDomain(entity: WeeklyWorkoutPlanEntity, days: List<WorkoutDay> = emptyList()): WeeklyWorkoutPlan {
        return WeeklyWorkoutPlan(
            id = entity.id,
            userId = entity.userId,
            weekNumber = entity.weekNumber,
            title = entity.title,
            workoutDays = days,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun mapToEntity(day: WorkoutDay, weeklyPlanId: Long): WorkoutDayEntity {
        return WorkoutDayEntity(
            id = day.id,
            weeklyPlanId = weeklyPlanId,
            dayNumber = day.dayNumber,
            title = day.title,
            status = day.status.name,
            duration = day.duration,
            exerciseCount = day.exerciseCount,
            equipment = day.equipment,
            completedAt = day.completedAt
        )
    }

    fun mapToDomain(entity: WorkoutDayEntity, exercises: List<WorkoutExercise> = emptyList()): WorkoutDay {
        return WorkoutDay(
            id = entity.id,
            dayNumber = entity.dayNumber,
            title = entity.title,
            status = WorkoutStatus.valueOf(entity.status),
            duration = entity.duration,
            exerciseCount = entity.exerciseCount,
            equipment = entity.equipment,
            exercises = exercises,
            completedAt = entity.completedAt
        )
    }

    fun mapToEntity(exercise: WorkoutExercise, workoutDayId: Long): WorkoutExerciseEntity {
        return WorkoutExerciseEntity(
            id = exercise.id,
            workoutDayId = workoutDayId,
            name = exercise.name,
            sets = exercise.sets,
            reps = exercise.reps,
            duration = exercise.duration,
            restTime = exercise.restTime,
            equipment = exercise.equipment,
            instructions = exercise.instructions,
            videoTutorialUrl = exercise.videoTutorialUrl,
            isCompleted = exercise.isCompleted,
            notes = exercise.notes
        )
    }

    fun mapToDomain(entity: WorkoutExerciseEntity): WorkoutExercise {
        return WorkoutExercise(
            id = entity.id,
            name = entity.name,
            sets = entity.sets,
            reps = entity.reps,
            duration = entity.duration,
            restTime = entity.restTime,
            equipment = entity.equipment,
            instructions = entity.instructions,
            videoTutorialUrl = entity.videoTutorialUrl,
            isCompleted = entity.isCompleted,
            notes = entity.notes
        )
    }
}
