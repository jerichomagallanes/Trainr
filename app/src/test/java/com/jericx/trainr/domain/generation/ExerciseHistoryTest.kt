package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import org.junit.Test

class ExerciseHistoryTest {

    private val day = 86_400_000L

    private fun week(number: Int, start: Long?, vararg days: WorkoutDay) = WeeklyWorkoutPlan(
        userId = 1, weekNumber = number, title = "Week", startDateMillis = start,
        workoutDays = days.toList()
    )

    private fun session(dayNumber: Int, key: String, reps: Int, completedAt: Long? = null) = WorkoutDay(
        dayNumber = dayNumber, title = "Day", duration = 30, exerciseCount = 1,
        equipment = emptyList(), completedAt = completedAt,
        exercises = listOf(
            WorkoutExercise(
                exerciseKey = key, name = key, measure = ExerciseMeasure.WEIGHT_AND_REPS,
                sets = listOf(
                    ExerciseSet(setNumber = 1, targetReps = reps, targetWeightKg = 50f, isCompleted = true)
                )
            )
        )
    )

    @Test
    fun theNewestSessionComesFirst() {
        val history = ExerciseHistory.from(
            listOf(week(1, 0L, session(1, "squat", 6)), week(2, 7 * day, session(1, "squat", 7))),
            "squat"
        )

        assertThat(history.sessions.map { it.targetAmount }).containsExactly(7, 6).inOrder()
    }

    // A day is dated by when it was finished, or failing that by where it
    // sits in its week.
    @Test
    fun aSessionIsDatedByWhenItWasFinishedOrWhereItSits() {
        val finished = ExerciseHistory.from(listOf(week(1, 0L, session(3, "squat", 6, completedAt = 999L))), "squat")
        val placed = ExerciseHistory.from(listOf(week(1, 10 * day, session(3, "squat", 6))), "squat")

        assertThat(finished.sessions.single().performedAtMillis).isEqualTo(999L)
        assertThat(placed.sessions.single().performedAtMillis).isEqualTo(12 * day)
    }

    // A plan stored before start dates existed has no date, and a gap nobody
    // can measure is not a gap.
    @Test
    fun aWeekWithNoStartDateGivesAnUndatedSession() {
        val history = ExerciseHistory.from(listOf(week(1, null, session(1, "squat", 6))), "squat")

        assertThat(history.sessions.single().performedAtMillis).isNull()
    }

    @Test
    fun onlyTheMostRecentFewSessionsAreKept() {
        val weeks = (1..8).map { week(it, it * 7 * day, session(1, "squat", it)) }

        assertThat(ExerciseHistory.from(weeks, "squat").sessions).hasSize(ExerciseHistory.HISTORY_DEPTH)
    }

    @Test
    fun aMovementNeverPrescribedHasNoHistory() {
        assertThat(ExerciseHistory.from(listOf(week(1, 0L, session(1, "squat", 6))), "bench").sessions).isEmpty()
    }

    // Logged as reps and measured in seconds today, the numbers describe a
    // different exercise.
    @Test
    fun aSessionLoggedInAnotherMeasureIsNotUsable() {
        val session = ExerciseHistory.from(listOf(week(1, 0L, session(1, "squat", 6))), "squat").sessions.single()

        assertThat(session.isUsableFor(ExerciseMeasure.WEIGHT_AND_REPS)).isTrue()
        assertThat(session.isUsableFor(ExerciseMeasure.DURATION)).isFalse()
    }
}
