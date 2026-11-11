package com.jericx.trainr.data.local

import com.jericx.trainr.domain.model.*
import com.jericx.trainr.domain.unstuck.ActualOrigin

class UserMapper {

    fun mapToEntity(user: UserProfile): UserEntity {
        return UserEntity(
            id = user.id,
            firstName = user.firstName,
            age = user.age,
            gender = user.gender.name,
            height = user.height,
            weight = user.weight,
            fitnessGoal = user.fitnessGoal.name,
            experienceLevel = user.experienceLevel.name,
            availableEquipment = user.availableEquipment.map { it.name },
            workoutDaysPerWeek = user.workoutDaysPerWeek,
            workoutDuration = user.workoutDuration,
            injuries = user.injuries.map { it.name },
            bodyUnitSystem = user.bodyUnitSystem.name,
            liftingUnitSystem = user.liftingUnitSystem?.name,
            createdAt = user.createdAt
        )
    }

    fun mapToDomain(entity: UserEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            firstName = entity.firstName,
            age = entity.age,
            gender = storedEnum(entity.gender, Gender.PREFER_NOT_TO_SAY),
            height = entity.height,
            weight = entity.weight,
            fitnessGoal = storedEnum(entity.fitnessGoal, FitnessGoal.GENERAL_FITNESS),
            experienceLevel = storedEnum(entity.experienceLevel, ExperienceLevel.BEGINNER),
            availableEquipment = entity.availableEquipment.mapNotNull(::storedEquipment).distinct(),
            workoutDaysPerWeek = entity.workoutDaysPerWeek,
            workoutDuration = entity.workoutDuration,
            injuries = entity.injuries.mapNotNull { storedEnum<Injury>(it) },
            bodyUnitSystem = runCatching { UnitSystem.valueOf(entity.bodyUnitSystem) }
                .getOrDefault(UnitSystem.Default),
            liftingUnitSystem = entity.liftingUnitSystem
                ?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() },
            createdAt = entity.createdAt
        )
    }

    fun mapToEntity(plan: WeeklyWorkoutPlan): WeeklyWorkoutPlanEntity {
        return WeeklyWorkoutPlanEntity(
            id = plan.id,
            userId = plan.userId,
            weekNumber = plan.weekNumber,
            title = plan.title,
            startDateMillis = plan.startDateMillis,
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
            startDateMillis = entity.startDateMillis,
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
            status = storedEnum(entity.status, WorkoutStatus.NOT_STARTED),
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
            exerciseKey = exercise.exerciseKey,
            name = exercise.name,
            measure = exercise.measure.name,
            setCount = exercise.setCount,
            reps = exercise.reps,
            duration = exercise.duration,
            durationMinutes = exercise.durationMinutes,
            restTime = exercise.restTime,
            equipment = exercise.equipment,
            videoTutorialUrl = exercise.videoTutorialUrl,
            isCompleted = exercise.isCompleted,
            notes = exercise.notes,
            sortOrder = exercise.sortOrder,
            addedBy = exercise.addedBy
        )
    }

    fun mapToDomain(entity: WorkoutExerciseEntity): WorkoutExercise {
        return WorkoutExercise(
            id = entity.id,
            exerciseKey = entity.exerciseKey,
            name = entity.name,
            measure = runCatching { ExerciseMeasure.valueOf(entity.measure) }
                .getOrDefault(ExerciseMeasure.REPS),
            setCount = entity.setCount,
            reps = entity.reps,
            duration = entity.duration,
            durationMinutes = entity.durationMinutes,
            restTime = entity.restTime,
            equipment = entity.equipment,
            videoTutorialUrl = entity.videoTutorialUrl,
            isCompleted = entity.isCompleted,
            notes = entity.notes,
            sortOrder = entity.sortOrder,
            addedBy = entity.addedBy
        )
    }

    fun mapToEntity(set: ExerciseSet, workoutExerciseId: Long): ExerciseSetEntity {
        return ExerciseSetEntity(
            id = set.id,
            workoutExerciseId = workoutExerciseId,
            setNumber = set.setNumber,
            targetReps = set.targetReps,
            targetWeightKg = set.targetWeightKg,
            targetSeconds = set.targetSeconds,
            actualReps = set.actualReps,
            actualWeightKg = set.actualWeightKg,
            actualSeconds = set.actualSeconds,
            isCompleted = set.isCompleted,
            actualOrigin = storedOrigin(set).name,
            omittedBy = set.omittedBy
        )
    }

    private fun storedOrigin(set: ExerciseSet): ActualOrigin {
        val hasActuals = set.actualReps != null || set.actualWeightKg != null || set.actualSeconds != null
        return if (set.actualOrigin == ActualOrigin.NONE && hasActuals) ActualOrigin.LEGACY_UNKNOWN else set.actualOrigin
    }

    fun mapToDomain(entity: ExerciseSetEntity): ExerciseSet {
        return ExerciseSet(
            id = entity.id,
            setNumber = entity.setNumber,
            targetReps = entity.targetReps,
            targetWeightKg = entity.targetWeightKg,
            targetSeconds = entity.targetSeconds,
            actualReps = entity.actualReps,
            actualWeightKg = entity.actualWeightKg,
            actualSeconds = entity.actualSeconds,
            isCompleted = entity.isCompleted,
            actualOrigin = runCatching { ActualOrigin.valueOf(entity.actualOrigin) }
                .getOrDefault(ActualOrigin.LEGACY_UNKNOWN),
            omittedBy = entity.omittedBy
        )
    }
}

// A constant this build does not know is a row written by a later release, not
// corruption: the row is still theirs. Reading it back as the neutral answer
// costs one setting they can change, or a day that reads as not started until
// they open it, where throwing would put every launch into a crash loop and
// cost them their whole history. The fallbacks are the same ones iOS uses.
private inline fun <reified T : Enum<T>> storedEnum(raw: String, fallback: T): T =
    storedEnum<T>(raw) ?: fallback

private inline fun <reified T : Enum<T>> storedEnum(raw: String): T? =
    runCatching { enumValueOf<T>(raw) }.getOrNull()
