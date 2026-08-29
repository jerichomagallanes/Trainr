package com.jericx.trainr.presentation.workout.sample

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.model.YouTubeVideo
import com.jericx.trainr.presentation.workout.model.toRoutineUi
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
            listOf("Yoga Mat"),
            listOf("Dumbbells", "Yoga Mat")
        ).inOrder()
        assertThat(equipment.flatten()).isNotEmpty()
    }

    // The design lists dumbbells and a treadmill for a routine that is five
    // bodyweight exercises. A day should not ask for kit it never uses.
    @Test
    fun theCardioRoutineOnlyAsksForKitItsExercisesUse() {
        val cardioAndCore = SampleWorkoutData.weekOne.workoutDays
            .first { it.title == "Cardio & Core" }

        assertThat(cardioAndCore.equipment).doesNotContain("Dumbbells")
        assertThat(cardioAndCore.equipment).doesNotContain("Treadmill")
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
    fun everyDaysDurationIsTheSumOfItsExercises() {
        SampleWorkoutData.weekOne.workoutDays.forEach { day ->
            assertThat(day.toRoutineUi().totalMinutes).isEqualTo(day.duration)
        }
    }

    @Test
    fun everyDaysExerciseCountMatchesItsRoutine() {
        SampleWorkoutData.weekOne.workoutDays.forEach { day ->
            assertThat(day.toRoutineUi().exercises).hasSize(day.exerciseCount)
        }
    }

    // A day the plan calls Completed must not open a routine with work left in
    // it, and vice versa.
    @Test
    fun everyDaysStatusAgreesWithItsRoutine() {
        SampleWorkoutData.weekOne.workoutDays.forEach { day ->
            val routine = day.toRoutineUi()

            assertThat(routine.isComplete).isEqualTo(day.status == WorkoutStatus.COMPLETED)
        }
    }

    @Test
    fun aNotStartedDayHasNothingTickedOff() {
        val notStarted = SampleWorkoutData.weekOne.workoutDays
            .first { it.status == WorkoutStatus.NOT_STARTED }

        assertThat(notStarted.toRoutineUi().completedCount).isEqualTo(0)
    }

    // Every field the routine card renders has to actually be there; a blank
    // prescription or description is an empty chip on screen.
    @Test
    fun everyExerciseIsFullyDescribed() {
        SampleWorkoutData.weekOne.workoutDays.flatMap { it.exercises }.forEach { exercise ->
            assertThat(exercise.name).isNotEmpty()
            assertThat(exercise.instructions).isNotEmpty()
            assertThat(exercise.prescription).isNotEmpty()
            assertThat(exercise.durationMinutes).isGreaterThan(0)
        }
    }

    // A tutorial the parser cannot read is a card with a dead toggle on it.
    @Test
    fun everyExerciseLinksAVideoTheParserCanRead() {
        val videos = SampleWorkoutData.dayFor(SampleWorkoutData.DEFAULT_DAY_NUMBER)
            .toRoutineUi().exercises.map { YouTubeVideo.from(it.videoUrl) }

        assertThat(videos).doesNotContain(null)
        assertThat(videos.map { it?.id }.toSet()).hasSize(videos.size)
    }

}
