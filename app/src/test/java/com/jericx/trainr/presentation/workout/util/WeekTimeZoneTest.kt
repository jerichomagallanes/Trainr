package com.jericx.trainr.presentation.workout.util

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.WeeklyPlanViewModel
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

// The zone the phone is in is driven through the seam, never the machine's own,
// which is pinned to UTC so a week date rendering against it would show.
class WeekTimeZoneTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun aWeekMadeAnHourEastKeepsEveryWeekday() {
        val anchor = anchorMadeIn(BERLIN, Calendar.AUGUST, 24)

        assertThat(weekdaysOf(anchor)).containsExactly(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        ).inOrder()
        assertThat(anchor).isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 24))
        assertThat(WorkoutWeek.dateOfDay(anchor, 7))
            .isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 30))
    }

    @Test
    fun aWeekMadeNineteenHoursEastKeepsEveryWeekday() {
        val anchor = anchorMadeIn(TOKYO, Calendar.AUGUST, 24)

        assertThat(weekdaysOf(anchor)).containsExactly(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        ).inOrder()
        assertThat(anchor).isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 24))
    }

    @Test
    fun todayStaysOnItsOwnCardAfterMovingWest() {
        val plan = planAnchoredAt(anchorMadeIn(TOKYO, Calendar.AUGUST, 24))
        val thursdayMorning = momentIn(HONOLULU, Calendar.AUGUST, 27, hour = 7)

        val state = WeeklyPlanViewModel.stateFor(
            plan = plan,
            nowMillis = thursdayMorning,
            zone = HONOLULU
        )

        assertThat(state.days.single { it.isToday }.day.dayNumber).isEqualTo(4)
        assertThat(state.days.count { it.isPast }).isEqualTo(3)
        assertThat(state.weekHasEnded).isFalse()
    }

    @Test
    fun theLastDayOfTheWeekIsStillWritableAfterMovingWest() {
        val anchor = anchorMadeIn(TOKYO, Calendar.AUGUST, 24)
        val sundayMorning = momentIn(HONOLULU, Calendar.AUGUST, 30, hour = 8)

        assertThat(WorkoutWeek.hasEnded(anchor, sundayMorning, HONOLULU)).isFalse()
        assertThat(planAnchoredAt(anchor).isReadyForTheNextWeek(sundayMorning, HONOLULU)).isFalse()
    }

    @Test
    fun anHourWestwardLeavesTheLastDayWritable() {
        val anchor = anchorMadeIn(BERLIN, Calendar.AUGUST, 24)
        val sundayEvening = momentIn(LONDON, Calendar.AUGUST, 30, hour = 22)

        assertThat(WorkoutWeek.hasEnded(anchor, sundayEvening, LONDON)).isFalse()
    }

    @Test
    fun anEastwardMoveStillReadsTheSameWeek() {
        val anchor = anchorMadeIn(LOS_ANGELES, Calendar.AUGUST, 24)
        val wednesday = momentIn(LONDON, Calendar.AUGUST, 26, hour = 10)

        val state = WeeklyPlanViewModel.stateFor(
            plan = planAnchoredAt(anchor),
            nowMillis = wednesday,
            zone = LONDON
        )

        assertThat(weekdaysOf(anchor).first()).isEqualTo("Monday")
        assertThat(state.days.single { it.isToday }.day.dayNumber).isEqualTo(3)
        assertThat(state.weekHasEnded).isFalse()
    }

    @Test
    fun aWeekWhoseLastDayHasPassedStillReadsAsEnded() {
        val anchor = anchorMadeIn(BERLIN, Calendar.AUGUST, 24)
        val theMondayAfter = momentIn(LONDON, Calendar.AUGUST, 31, hour = 0)

        assertThat(WorkoutWeek.hasEnded(anchor, theMondayAfter, LONDON)).isTrue()
        assertThat(planAnchoredAt(anchor).isReadyForTheNextWeek(theMondayAfter, LONDON)).isTrue()
    }

    @Test
    fun aWeekWithNoAnchorStillRunsFromTheDayItWasMade() {
        val madeAt = momentIn(BERLIN, Calendar.AUGUST, 24, hour = 21)
        val plan = planAnchoredAt(null).copy(createdAt = madeAt)

        assertThat(plan.weekStartDate(BERLIN))
            .isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 24))

        val state = WeeklyPlanViewModel.stateFor(plan, nowMillis = madeAt, zone = BERLIN)

        assertThat(state.days.single { it.isToday }.day.dayNumber).isEqualTo(1)
    }

    // The hour Berlin loses falls inside this week; a day must still be a day.
    @Test
    fun aWeekAcrossADaylightSavingChangeStillHasSevenCalendarDays() {
        val anchor = anchorMadeIn(BERLIN, Calendar.MARCH, 23)

        assertThat(weekdaysOf(anchor)).containsExactly(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        ).inOrder()
        assertThat(WorkoutWeek.dateOfDay(anchor, 7))
            .isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.MARCH, 29))
    }

    private fun weekdaysOf(anchor: Long): List<String> = (1..7).map {
        WorkoutDateFormatter.formatWeekday(WorkoutWeek.dateOfDay(anchor, it), Locale.US)
    }

    // What onboarding writes: the calendar date the phone was living in.
    private fun anchorMadeIn(zone: TimeZone, month: Int, day: Int): Long =
        WorkoutWeek.startOfDay(momentIn(zone, month, day, hour = 9), zone)

    private fun momentIn(zone: TimeZone, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(zone).run {
            clear()
            set(YEAR, month, day, hour, 0)
            timeInMillis
        }

    private fun planAnchoredAt(anchor: Long?) = WeeklyWorkoutPlan(
        userId = 1,
        weekNumber = 1,
        title = "Foundation",
        startDateMillis = anchor,
        workoutDays = (1..7).map {
            WorkoutDay(
                id = it.toLong(),
                dayNumber = it,
                title = "Day $it",
                status = WorkoutStatus.NOT_STARTED,
                duration = 45,
                exerciseCount = 1,
                equipment = emptyList(),
                exercises = listOf(WorkoutExercise(name = "Movement"))
            )
        }
    )

    private companion object {
        const val YEAR = 2026
        val BERLIN: TimeZone = TimeZone.getTimeZone("Europe/Berlin")
        val LONDON: TimeZone = TimeZone.getTimeZone("Europe/London")
        val TOKYO: TimeZone = TimeZone.getTimeZone("Asia/Tokyo")
        val HONOLULU: TimeZone = TimeZone.getTimeZone("Pacific/Honolulu")
        val LOS_ANGELES: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")
    }
}
