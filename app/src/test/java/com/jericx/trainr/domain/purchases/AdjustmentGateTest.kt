package com.jericx.trainr.domain.purchases

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdjustmentGateTest {

    private class FakeAllowance(private var included: String? = null) : AdjustmentAllowance {
        override fun includedCycleId() = included
        override fun consume(cycleId: String) {
            if (included == null) included = cycleId
        }
    }

    private fun gate(
        isPro: Boolean = false,
        canSell: Boolean = true,
        allowance: AdjustmentAllowance = FakeAllowance()
    ) = AdjustmentGate(isPro = { isPro }, canSell = { canSell }, allowance = allowance)

    @Test
    fun theFirstAdjustmentIsIncluded() {
        assertThat(gate().decide("proposal-1")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    @Test
    fun aSecondAdjustmentAsksForPro() {
        val spent = gate(allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)
    }

    // A request with no proposal yet is a new cycle, not the included one.
    @Test
    fun aRequestWithNoCycleYetAsksOnceTheIncludedOneIsSpent() {
        val spent = gate(allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide(null)).isEqualTo(AdjustmentGate.Decision.ASK)
    }

    @Test
    fun aSubscriberIsNeverAsked() {
        val spent = gate(isPro = true, allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    @Test
    fun applyingIsWhatClosesTheIncludedCycle() {
        val allowance = FakeAllowance()
        val subject = gate(allowance = allowance)

        assertThat(subject.decide("proposal-1")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
        subject.spend("proposal-1")

        assertThat(allowance.includedCycleId()).isEqualTo("proposal-1")
        assertThat(subject.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)
    }

    // T25: undo, reapply and the later follow-up all name the cycle that was
    // included, and none of them is a second adjustment to pay for.
    @Test
    fun theIncludedCycleStaysFreeAfterItIsSpent() {
        val subject = gate()
        subject.spend("proposal-1")

        assertThat(subject.decide("proposal-1")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    // A build that cannot sell must not ask: the free week taught us that a
    // paywall with nothing on it strands people.
    @Test
    fun aBuildThatCannotSellNeverAsks() {
        val spent = gate(canSell = false, allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    @Test
    fun aBuildThatCannotSellSpendsNothing() {
        val allowance = FakeAllowance()
        gate(canSell = false, allowance = allowance).spend("proposal-1")

        assertThat(allowance.includedCycleId()).isNull()
    }

    // A subscriber who lapses should still find the adjustment they never used.
    @Test
    fun aSubscriberSpendsNothing() {
        val allowance = FakeAllowance()
        gate(isPro = true, allowance = allowance).spend("proposal-1")

        assertThat(allowance.includedCycleId()).isNull()
    }

    @Test
    fun aLaterCycleCannotTakeOverTheIncludedOne() {
        val allowance = FakeAllowance()
        val subject = gate(allowance = allowance)

        subject.spend("proposal-1")
        subject.spend("proposal-2")

        assertThat(allowance.includedCycleId()).isEqualTo("proposal-1")
        assertThat(subject.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)
    }
}
