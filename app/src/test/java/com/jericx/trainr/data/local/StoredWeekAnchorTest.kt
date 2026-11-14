package com.jericx.trainr.data.local

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

class StoredWeekAnchorTest {

    @Test
    fun aPlanTheShippedAppStoredKeepsItsDay() {
        val stored = shippedAnchor(BERLIN, Calendar.AUGUST, 24)

        assertThat(TrainrDatabase.utcMidnightOf(stored, BERLIN))
            .isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 24))
    }

    @Test
    fun aPlanStoredWestOfGreenwichKeepsItsDay() {
        val stored = shippedAnchor(LOS_ANGELES, Calendar.AUGUST, 24)

        assertThat(TrainrDatabase.utcMidnightOf(stored, LOS_ANGELES))
            .isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 24))
    }

    // Berlin is at +01:00 on the 28th and +02:00 from the 29th, so the offset
    // in force now would move a plan anchored the week before onto the 27th.
    @Test
    fun aPlanStoredBeforeADaylightSavingChangeKeepsItsDay() {
        val stored = shippedAnchor(BERLIN, Calendar.MARCH, 28)

        assertThat(TrainrDatabase.utcMidnightOf(stored, BERLIN))
            .isEqualTo(WorkoutWeek.dateOf(YEAR, Calendar.MARCH, 28))
    }

    @Test
    fun anAnchorAlreadyWrittenAsACalendarDateIsLeftWhereItIs() {
        val migrated = WorkoutWeek.dateOf(YEAR, Calendar.AUGUST, 24)

        assertThat(TrainrDatabase.utcMidnightOf(migrated, UTC)).isEqualTo(migrated)
    }

    private fun shippedAnchor(zone: TimeZone, month: Int, day: Int): Long =
        Calendar.getInstance(zone).run {
            clear()
            set(YEAR, month, day)
            timeInMillis
        }

    private companion object {
        const val YEAR = 2026
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        val BERLIN: TimeZone = TimeZone.getTimeZone("Europe/Berlin")
        val LOS_ANGELES: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")
    }
}
