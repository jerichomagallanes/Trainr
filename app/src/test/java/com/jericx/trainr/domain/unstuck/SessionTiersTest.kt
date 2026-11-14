package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.SlotTier
import org.junit.Test

class SessionTiersTest {

    private val day = testDay(
        planned("warm_up", sets = 1, id = 1),
        planned("barbell_bench_press", sets = 3, id = 2),
        planned("barbell_squat", sets = 3, id = 3),
        planned("barbell_bent_over_row", sets = 3, id = 4),
        planned("dumbbell_bicep_curl", sets = 3, id = 5),
        planned("bicycle_crunch", sets = 3, id = 6)
    )

    @Test
    fun theSessionOrderIsReadBackOffTheStoredDay() {
        val tiers = SessionTiers.assign(day, testCatalog)

        assertThat(tiers).containsExactly(
            1L, SlotTier.WARM_UP,
            2L, SlotTier.PRIMARY_COMPOUND,
            3L, SlotTier.SECONDARY_COMPOUND,
            4L, SlotTier.ACCESSORY,
            5L, SlotTier.ISOLATION,
            6L, SlotTier.CORE
        )
    }

    @Test
    fun aConfirmedPriorityTakesThePrimarySlotAndDemotesTheNaturalOne() {
        val tiers = SessionTiers.assign(day, testCatalog, GoalPriority("barbell_squat"))

        assertThat(tiers[3L]).isEqualTo(SlotTier.PRIMARY_COMPOUND)
        assertThat(tiers[2L]).isEqualTo(SlotTier.SECONDARY_COMPOUND)
    }

    @Test
    fun aMovementTheCatalogDoesNotKnowIsAnAccessory() {
        val tiers = SessionTiers.assign(
            testDay(planned("not_a_movement", sets = 3, id = 9)),
            testCatalog
        )

        assertThat(tiers[9L]).isEqualTo(SlotTier.ACCESSORY)
    }
}
