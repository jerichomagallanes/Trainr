package com.jericx.trainr.domain.purchases

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdjustmentGateTest {

    private class FakeAllowance(private var included: String? = null) : AdjustmentAllowance {
        override fun includedCycleId() = included
        override fun consume(cycleId: String) {
            if (included == null) included = cycleId
        }

        override fun restore(cycleId: String) {
            if (included == cycleId) included = null
        }
    }

    private fun gate(
        isPro: Boolean = false,
        canSell: Boolean = true,
        hasRead: Boolean = true,
        readFailed: Boolean = false,
        allowance: AdjustmentAllowance = FakeAllowance()
    ) = AdjustmentGate(
        isPro = { isPro },
        hasRead = { hasRead },
        readFailed = { readFailed },
        canSell = { canSell },
        allowance = allowance
    )

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
    fun undoingTheIncludedCycleGivesItBack() {
        val allowance = FakeAllowance()
        val subject = gate(allowance = allowance)
        subject.spend("proposal-1")
        assertThat(subject.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)

        allowance.restore("proposal-1")

        assertThat(allowance.includedCycleId()).isNull()
        assertThat(subject.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    @Test
    fun undoingAnotherCycleLeavesTheIncludedOneSpent() {
        val allowance = FakeAllowance()
        val subject = gate(allowance = allowance)
        subject.spend("proposal-1")

        allowance.restore("proposal-2")

        assertThat(allowance.includedCycleId()).isEqualTo("proposal-1")
        assertThat(subject.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)
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

    // The defect this covers: a subscriber whose entitlement is still being
    // read is false here like everyone else, so the gate used to decide what
    // they are charged for from an answer the store had not given.
    @Test
    fun aRequestBeforeTheStoreHasAnsweredNeitherAllowsNorAsks() {
        val spent = gate(hasRead = false, allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.WAIT)
    }

    @Test
    fun aSubscriberIsNotAskedWhileTheReadIsStillOut() {
        val spent = gate(isPro = true, hasRead = false, allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.WAIT)
    }

    // A failed read is not a pending one: nothing more is coming, so waiting
    // would strand the person. It falls in with the build that cannot sell,
    // which lets the adjustment through rather than ask for money on a guess.
    @Test
    fun aReadThatWasNeverAnsweredAtAllLetsTheAdjustmentThrough() {
        val spent = gate(
            hasRead = false,
            readFailed = true,
            allowance = FakeAllowance("proposal-1")
        )

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    // The paywall re-reads, so a read can fail after one has already landed.
    // Treating that as an unknown store would throw away an answer it gave and
    // leave every adjustment free for the rest of the session.
    @Test
    fun aReadThatFailedAfterAnAnswerLandedStillAsks() {
        val spent = gate(readFailed = true, allowance = FakeAllowance("proposal-1"))

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)
    }

    @Test
    fun aSubscriberWhoseLaterReadFailedIsStillNeverAsked() {
        val spent = gate(
            isPro = true,
            readFailed = true,
            allowance = FakeAllowance("proposal-1")
        )

        assertThat(spent.decide("proposal-2")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    // Nothing is adjusted without this gate deciding first, so a change that
    // landed while the read was out landed on a guess. The included cycle is
    // what the person paid for, and a subscriber reads the same as everyone
    // else until the store speaks.
    @Test
    fun anAdjustmentAppliedBeforeTheStoreAnsweredSpendsNothing() {
        val allowance = FakeAllowance()
        gate(hasRead = false, allowance = allowance).spend("proposal-1")

        assertThat(allowance.includedCycleId()).isNull()
    }

    @Test
    fun anAdjustmentLetThroughByAStoreThatNeverAnsweredSpendsNothing() {
        val allowance = FakeAllowance()
        gate(hasRead = false, readFailed = true, allowance = allowance).spend("proposal-1")

        assertThat(allowance.includedCycleId()).isNull()
    }
}
