package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanRevisionTest {

    private val day = testDay(
        planned("warm_up", sets = 1, id = 1),
        planned("barbell_bench_press", sets = 3, id = 2),
        planned("dumbbell_bicep_curl", sets = 3, id = 3)
    )

    @Test
    fun loggingASetChangesTheRevision() {
        val logged = day.copy(
            exercises = day.exercises.map {
                if (it.id == 2L) it.logged(setNumber = 1, reps = 8) else it
            }
        )

        assertThat(PlanRevision.of(logged)).isNotEqualTo(PlanRevision.of(day))
    }

    @Test
    fun reorderingTheSameRowsLeavesTheRevisionWhereItWas() {
        val shuffled = day.copy(exercises = day.exercises.reversed())

        assertThat(PlanRevision.of(shuffled)).isEqualTo(PlanRevision.of(day))
    }

    @Test
    fun aRevisionIsSixteenBytesOfHex() {
        assertThat(PlanRevision.of(day)).matches("[0-9a-f]{32}")
    }
}
