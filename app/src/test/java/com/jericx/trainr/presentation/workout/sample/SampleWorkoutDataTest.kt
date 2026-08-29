package com.jericx.trainr.presentation.workout.sample

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.sample.SampleRoutine
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class SampleWorkoutDataTest {

    private lateinit var originalTimeZone: TimeZone

    // Pinned so assertions don't depend on the machine's zone.
    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun weekdayOf(millis: Long) =
        WorkoutDateFormatter.formatWeekday(millis, Locale.US)

    @Test
    fun weekStartsOnAMonday() {
        assertThat(weekdayOf(SampleWorkoutData.weekStartMillis)).isEqualTo("Monday")
    }

    @Test
    fun weekEndsOnASunday() {
        assertThat(weekdayOf(SampleWorkoutData.weekEndMillis)).isEqualTo("Sunday")
    }

    @Test
    fun dayNumbersMapToTheWeekdaysInTheDesign() {
        val weekdays = SampleWorkoutData.weekOne.workoutDays.map {
            weekdayOf(SampleWorkoutData.dateOf(it.dayNumber))
        }

        assertThat(weekdays).containsExactly("Monday", "Wednesday", "Friday").inOrder()
    }

    @Test
    fun weekRangeRendersAsShownInTheDesign() {
        val range = WorkoutDateFormatter.formatWeekRange(
            startMillis = SampleWorkoutData.weekStartMillis,
            endMillis = SampleWorkoutData.weekEndMillis,
            locale = Locale.US
        )

        assertThat(range).isEqualTo("July 21 – 27, 2025")
    }

    @Test
    fun coversEveryWorkoutStatusSoScreensCanBeSeenInAllStates() {
        val statuses = SampleWorkoutData.weekOne.workoutDays.map { it.status }

        assertThat(statuses).containsExactlyElementsIn(WorkoutStatus.entries)
    }

    @Test
    fun onlyTheCompletedDayHasACompletionTimestamp() {
        val days = SampleWorkoutData.weekOne.workoutDays

        assertThat(days.filter { it.completedAt != null }.map { it.status })
            .containsExactly(WorkoutStatus.COMPLETED)
    }

    @Test
    fun everyDayListsEquipment() {
        val equipment = SampleWorkoutData.weekOne.workoutDays.map { it.equipment }

        assertThat(equipment).containsExactly(
            listOf("Dumbbells", "Yoga Mat"),
            listOf("Dumbbells", "Yoga Mat"),
            listOf("Dumbbells", "Yoga Mat")
        ).inOrder()
    }

    @Test
    fun equipmentIsSpelledCorrectly() {
        val names = SampleWorkoutData.weekOne.workoutDays.flatMap { it.equipment }

        assertThat(names).doesNotContain("Dumbells")
    }

    @Test
    fun datesFollowTheCurrentTimeZoneRatherThanTheOneAtFirstUse() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        val tokyoStart = SampleWorkoutData.weekStartMillis

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val utcStart = SampleWorkoutData.weekStartMillis

        assertThat(tokyoStart).isNotEqualTo(utcStart)
        assertThat(weekdayOf(utcStart)).isEqualTo("Monday")
    }

    // The routine screen lists each exercise's minutes while the plan card shows a
    // total. If they disagree the app contradicts itself on screen, and a user
    // adding up the parts gets a different answer.
    @Test
    fun theRoutineDurationIsTheSumOfItsExercises() {
        val cardioAndCore = SampleWorkoutData.weekOne.workoutDays.first { it.title == "Cardio & Core" }

        assertThat(cardioAndCore.duration).isEqualTo(SampleRoutine.cardioAndCore.totalMinutes)
    }

    @Test
    fun theRoutineExerciseCountMatchesItsExercises() {
        val cardioAndCore = SampleWorkoutData.weekOne.workoutDays.first { it.title == "Cardio & Core" }

        assertThat(cardioAndCore.exerciseCount).isEqualTo(SampleRoutine.cardioAndCore.exercises.size)
    }
}
