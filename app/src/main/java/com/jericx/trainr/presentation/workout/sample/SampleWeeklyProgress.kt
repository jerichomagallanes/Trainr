package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.presentation.workout.model.WeekProgressUi
import com.jericx.trainr.presentation.workout.model.WeekStatus
import java.util.Calendar

object SampleWeeklyProgress {

    private const val DAYS_PER_WEEK = 7

    // Computed per access so the dates follow the current default time zone.
    val weeks: List<WeekProgressUi>
        get() = listOf(
            week(1, completedDays = 3, status = WeekStatus.COMPLETED),
            week(2, completedDays = 2, status = WeekStatus.NOT_COMPLETED),
            week(3, completedDays = 0, status = WeekStatus.SKIPPED),
            week(4, completedDays = 1, status = WeekStatus.IN_PROGRESS),
            week(5, completedDays = 3, status = WeekStatus.COMPLETED),
            week(6, completedDays = 2, status = WeekStatus.NOT_COMPLETED),
            week(7, completedDays = 3, status = WeekStatus.COMPLETED),
            week(8, completedDays = 0, status = WeekStatus.UPCOMING)
        )

    private fun week(number: Int, completedDays: Int, status: WeekStatus) = WeekProgressUi(
        weekNumber = number,
        completedDays = completedDays,
        totalDays = 3,
        status = status,
        startDateMillis = shift(SampleWorkoutData.weekStartMillis, number),
        endDateMillis = shift(SampleWorkoutData.weekEndMillis, number)
    )

    private fun shift(millis: Long, weekNumber: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, (weekNumber - 1) * DAYS_PER_WEEK)
    }.timeInMillis
}
