package com.jericx.trainr.presentation.unstuck

import com.jericx.trainr.domain.model.DayOutline
import com.jericx.trainr.domain.model.WeekOutline
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
import io.mockk.mockk

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
    it.storing(plan)
}

internal fun WeeklyWorkoutPlan.outline() = WeekOutline(
    id = id,
    weekNumber = weekNumber,
    startDateMillis = startDateMillis,
    days = workoutDays.map { DayOutline(it.id, it.dayNumber) }
)

// Answers the targeted lookups the way the database does: by week number or
// the newest week, by the day a record points at, and one day at a time.
internal fun UserRepository.storing(vararg plans: WeeklyWorkoutPlan) {
    coEvery { getWeekOutline(any(), any()) } answers {
        val week = secondArg<Int?>()
        val plan = if (week == null) {
            plans.maxByOrNull { it.weekNumber }
        } else {
            plans.firstOrNull { it.weekNumber == week }
        }
        plan?.outline()
    }
    coEvery { getWeekOutlineOf(any()) } answers {
        val dayId = firstArg<Long>()
        plans.firstOrNull { plan -> plan.workoutDays.any { it.id == dayId } }?.outline()
    }
    coEvery { getWorkoutDay(any()) } answers {
        val dayId = firstArg<Long>()
        plans.flatMap { it.workoutDays }.firstOrNull { it.id == dayId }
    }
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
