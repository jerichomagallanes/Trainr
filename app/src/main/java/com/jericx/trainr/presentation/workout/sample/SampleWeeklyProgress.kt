package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.presentation.workout.model.WeekProgressUi
import com.jericx.trainr.presentation.workout.model.WeekStatus
import java.util.Calendar

object SampleWeeklyProgress {

    private const val DAYS_PER_WEEK = 7

    val weeks: List<WeekProgressUi> = listOf(
        WeekProgressUi(1, completedDays = 3, totalDays = 3, status = WeekStatus.COMPLETED),
        WeekProgressUi(2, completedDays = 2, totalDays = 3, status = WeekStatus.NOT_COMPLETED),
        WeekProgressUi(3, completedDays = 0, totalDays = 3, status = WeekStatus.SKIPPED),
        WeekProgressUi(4, completedDays = 1, totalDays = 3, status = WeekStatus.IN_PROGRESS),
        WeekProgressUi(5, completedDays = 3, totalDays = 3, status = WeekStatus.COMPLETED),
        WeekProgressUi(6, completedDays = 2, totalDays = 3, status = WeekStatus.NOT_COMPLETED),
        WeekProgressUi(7, completedDays = 3, totalDays = 3, status = WeekStatus.COMPLETED)
    )

    fun weekStartMillis(weekNumber: Int): Long = shift(SampleWorkoutData.weekStartMillis, weekNumber)

    fun weekEndMillis(weekNumber: Int): Long = shift(SampleWorkoutData.weekEndMillis, weekNumber)

    private fun shift(millis: Long, weekNumber: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, (weekNumber - 1) * DAYS_PER_WEEK)
    }.timeInMillis
}
