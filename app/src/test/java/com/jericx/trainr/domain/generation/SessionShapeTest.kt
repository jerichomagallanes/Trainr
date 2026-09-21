package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.domain.model.FitnessGoal
import org.junit.Test

class SessionShapeTest {

    @Test
    fun everyGoalNamesASlotCountItsOwnDropOrderCanReach() {
        FitnessGoal.entries.forEach { goal ->
            val shape = SessionShape.forGoal(goal)

            assertWithMessage("$goal").that(shape.slotCount).isGreaterThan(0)
            assertWithMessage("$goal").that(shape.dropOrder).isNotEmpty()
            assertWithMessage("$goal").that(shape.dropOrder).containsNoDuplicates()
        }
    }

    @Test
    fun theDropOrderReadsBackAsTiersKeepingTheRepeatedOnes() {
        assertThat(SessionShape.forGoal(FitnessGoal.STRENGTH).dropTiers()).containsExactly(
            SlotTier.ISOLATION, SlotTier.CONDITIONING, SlotTier.MOBILITY, SlotTier.ACCESSORY,
            SlotTier.CORE, SlotTier.ISOLATION
        ).inOrder()
        assertThat(SessionShape.forGoal(FitnessGoal.MUSCLE_GAIN).dropTiers()).containsExactly(
            SlotTier.MOBILITY, SlotTier.CONDITIONING, SlotTier.ISOLATION, SlotTier.CORE,
            SlotTier.ACCESSORY, SlotTier.ISOLATION
        ).inOrder()
    }

    @Test
    fun aWeightLossDayWillShedASecondaryCompoundWhereAStrengthDayWillNot() {
        assertThat(SessionShape.forGoal(FitnessGoal.WEIGHT_LOSS).dropTiers())
            .contains(SlotTier.SECONDARY_COMPOUND)
        assertThat(SessionShape.forGoal(FitnessGoal.STRENGTH).dropTiers())
            .doesNotContain(SlotTier.SECONDARY_COMPOUND)
        assertThat(SessionShape.forGoal(FitnessGoal.WEIGHT_LOSS).conditioningFillsTheSession).isTrue()
    }
}
