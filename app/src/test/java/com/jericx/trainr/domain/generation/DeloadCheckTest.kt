package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import org.junit.Test

class DeloadCheckTest {

    private val lifter = UserProfile(age = 30, experienceLevel = ExperienceLevel.INTERMEDIATE)

    // Four sets asking for eight; `done` of them ticked, each at `reps`.
    private fun exercise(key: String, done: Int, reps: Int, kg: Float = 60f) = WorkoutExercise(
        exerciseKey = key, name = key, measure = ExerciseMeasure.WEIGHT_AND_REPS,
        sets = (1..4).map {
            ExerciseSet(
                setNumber = it, targetReps = 8, targetWeightKg = kg,
                actualReps = if (it <= done) reps else null, isCompleted = it <= done
            )
        }
    )

    private fun week(number: Int, vararg exercises: WorkoutExercise) = WeeklyWorkoutPlan(
        userId = 1, weekNumber = number, title = "Week",
        workoutDays = listOf(
            WorkoutDay(
                dayNumber = 1, title = "Day", duration = 45, exerciseCount = exercises.size,
                equipment = emptyList(), exercises = exercises.toList()
            )
        )
    )

    private fun metWeek(number: Int) = week(number, exercise("squat", 4, 8), exercise("bench", 4, 8))

    @Test
    fun noHistoryIsNeverDue() {
        assertThat(DeloadCheck.isDue(lifter, emptyList())).isFalse()
    }

    // A beginner's first two months are adaptation, not accumulated fatigue.
    @Test
    fun aBeginnerIsNeverDeloadedInTheirFirstEightWeeks() {
        val struggling = (1..3).map { week(it, exercise("squat", 2, 5), exercise("bench", 2, 5)) }

        assertThat(DeloadCheck.isDue(lifter.copy(experienceLevel = ExperienceLevel.BEGINNER), struggling))
            .isFalse()
    }

    // Stalling on two movements and finishing half the work across the last
    // fortnight: two signals.
    @Test
    fun stallingAndRarelyFinishingTogetherCallForALighterWeek() {
        val weeks = listOf(
            week(1, exercise("squat", 2, 5), exercise("bench", 2, 5)),
            week(2, exercise("squat", 2, 5), exercise("bench", 2, 5))
        )

        assertThat(DeloadCheck.isDue(lifter, weeks)).isTrue()
    }

    // One bad week on its own is noise.
    @Test
    fun stallingAloneIsNotEnough() {
        val weeks = listOf(
            metWeek(1),
            week(2, exercise("squat", 4, 6), exercise("bench", 4, 6))
        )

        assertThat(DeloadCheck.isDue(lifter, weeks)).isFalse()
    }

    // Six weeks without the load coming down on anything, plus a stall.
    @Test
    fun aLongRunWithoutALighterWeekCountsAsASignal() {
        val weeks = (1..6).map(::metWeek) +
            week(7, exercise("squat", 4, 6), exercise("bench", 4, 6))

        assertThat(DeloadCheck.isDue(lifter, weeks)).isTrue()
    }

    // Older lifters recover more slowly, so the run is shorter.
    @Test
    fun anOlderLifterGetsTheSignalSooner() {
        val weeks = (1..4).map(::metWeek) +
            week(5, exercise("squat", 4, 6), exercise("bench", 4, 6))

        assertThat(DeloadCheck.isDue(lifter, weeks)).isFalse()
        assertThat(DeloadCheck.isDue(lifter.copy(age = 55), weeks)).isTrue()
    }
}
