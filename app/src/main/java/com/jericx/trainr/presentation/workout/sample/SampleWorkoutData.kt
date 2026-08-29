package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import java.util.Calendar

// Placeholder content until plan generation exists. Kept in the presentation
// layer so it needs no Room migration. dayNumber is the ISO day of week.
object SampleWorkoutData {

    val weekStartMillis: Long get() = dateOf(1)

    val weekEndMillis: Long get() = dateOf(7)

    // Read per call rather than cached: the default time zone can be changed
    // after this object is first touched, which would otherwise freeze a stale date.
    fun dateOf(dayNumber: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2025, Calendar.JULY, 21)
        add(Calendar.DAY_OF_YEAR, dayNumber - 1)
    }.timeInMillis

    val weekOne: WeeklyWorkoutPlan
        get() = WeeklyWorkoutPlan(
            id = 1,
            userId = 1,
            weekNumber = 1,
            title = "Week 1",
            workoutDays = listOf(
                WorkoutDay(
                    id = 1,
                    dayNumber = 1,
                    title = "Full Body Strength",
                    status = WorkoutStatus.COMPLETED,
                    duration = 45,
                    exerciseCount = 6,
                    equipment = listOf("Dumbbells", "Yoga Mat"),
                    completedAt = dateOf(1)
                ),
                WorkoutDay(
                    id = 2,
                    dayNumber = 3,
                    title = "Cardio & Core",
                    status = WorkoutStatus.IN_PROGRESS,
                    duration = 28,
                    exerciseCount = 5,
                    equipment = listOf("Dumbbells", "Yoga Mat")
                ),
                WorkoutDay(
                    id = 3,
                    dayNumber = 5,
                    title = "Lower Body Power",
                    status = WorkoutStatus.NOT_STARTED,
                    duration = 40,
                    exerciseCount = 4,
                    equipment = listOf("Dumbbells", "Yoga Mat")
                )
            ),
            createdAt = weekStartMillis,
            updatedAt = weekStartMillis
        )
}
