package com.jericx.trainr.presentation.unstuck

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.PreferenceKind
import com.jericx.trainr.domain.unstuck.TrainingPreference
import com.jericx.trainr.domain.unstuck.planned
import com.jericx.trainr.domain.unstuck.testDay
import com.jericx.trainr.domain.unstuck.testUser
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

internal val preferenceDay: WorkoutDay =
    testDay(planned("barbell_bench_press", sets = 3, id = 2))

internal fun planStartingToday(day: WorkoutDay = preferenceDay) = WeeklyWorkoutPlan(
    id = 1,
    userId = 0,
    weekNumber = 1,
    title = "Week 1",
    startDateMillis = WorkoutWeek.startOfDay(),
    workoutDays = listOf(day)
)

internal fun usersWith(
    plan: WeeklyWorkoutPlan = planStartingToday(),
    plannedMinutes: Int = 45
): UserRepository = mockk<UserRepository>(relaxed = true).also {
    coEvery { it.getCurrentUser() } returns testUser(minutes = plannedMinutes)
    every { it.getWeeklyWorkoutPlans(any()) } returns flowOf(listOf(plan))
    coEvery { it.getWorkoutDay(plan.workoutDays.first().id) } returns plan.workoutDays.first()
}

internal fun timeLimit(
    id: Long = 1L,
    minutes: Int = 35,
    weekday: Int = WorkoutWeek.isoWeekdayOf(WorkoutWeek.startOfDay()),
    confirmedAt: Long = 1_000L,
    updatedAt: Long = 1_000L
) = TrainingPreference(
    id = id,
    userId = 0,
    kind = PreferenceKind.TIME_LIMIT,
    minutes = minutes,
    weekday = weekday,
    sourceAdjustmentId = null,
    confirmedAt = confirmedAt,
    updatedAt = updatedAt
)
