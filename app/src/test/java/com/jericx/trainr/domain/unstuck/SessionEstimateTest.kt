package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionEstimateTest {

    private val user = testUser()

    @Test
    fun remainingScopeCountsOnlyUnperformedWork() {
        val day = testDay(
            planned("barbell_bench_press", sets = 4, id = 1, performed = 2)
        )

        val whole = SessionEstimate.minutes(day, user, TimeScope.WHOLE_SESSION, testCatalog)
        val remaining = SessionEstimate.minutes(day, user, TimeScope.REMAINING, testCatalog)

        assertThat(remaining).isLessThan(whole)
        assertThat(remaining).isEqualTo(
            SessionEstimate.minutes(
                testDay(planned("barbell_bench_press", sets = 2, id = 1)),
                user,
                TimeScope.WHOLE_SESSION,
                testCatalog
            )
        )
    }

    @Test
    fun anExerciseWithNothingLeftCostsNothingAndNoTransition() {
        val one = testDay(planned("barbell_bench_press", sets = 3, id = 1))
        val plusFinished = testDay(
            planned("barbell_bench_press", sets = 3, id = 1),
            planned("dumbbell_bicep_curl", sets = 3, id = 2, performed = 3)
        )

        assertThat(SessionEstimate.minutes(plusFinished, user, TimeScope.REMAINING, testCatalog))
            .isEqualTo(SessionEstimate.minutes(one, user, TimeScope.REMAINING, testCatalog))
    }

    @Test
    fun omittedSetsAreNotPlannedWork() {
        val whole = testDay(planned("dumbbell_bicep_curl", sets = 4, id = 1))
        val partlyOmitted = testDay(planned("dumbbell_bicep_curl", sets = 4, id = 1, omitted = 2))

        assertThat(SessionEstimate.minutes(partlyOmitted, user, TimeScope.WHOLE_SESSION, testCatalog))
            .isLessThan(SessionEstimate.minutes(whole, user, TimeScope.WHOLE_SESSION, testCatalog))
    }
}
