package com.jericx.trainr.domain.purchases

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProGateTests {

    private class FakeAllowance(var used: Boolean = false) : FreeGenerationAllowance {
        override fun hasBeenUsed() = used
        override fun markUsed() {
            used = true
        }
    }

    private fun gate(
        isPro: Boolean,
        used: Boolean,
        canSell: Boolean = true,
        hasRead: Boolean = true,
        readFailed: Boolean = false
    ) = ProGate(
        isPro = { isPro },
        hasRead = { hasRead },
        readFailed = { readFailed },
        canSell = { canSell },
        allowance = FakeAllowance(used)
    )

    @Test
    fun `the first generation is free`() {
        assertThat(gate(isPro = false, used = false).decide())
            .isEqualTo(ProGate.Decision.ALLOWED)
    }

    @Test
    fun `the second generation asks for pro`() {
        assertThat(gate(isPro = false, used = true).decide())
            .isEqualTo(ProGate.Decision.ASK)
    }

    @Test
    fun `a subscriber is never asked`() {
        assertThat(gate(isPro = true, used = true).decide())
            .isEqualTo(ProGate.Decision.ALLOWED)
    }

    @Test
    fun `spending is what closes the free allowance`() {
        val allowance = FakeAllowance()
        val gate = gateOver(allowance)

        assertThat(gate.decide()).isEqualTo(ProGate.Decision.ALLOWED)
        gate.spend()

        assertThat(allowance.used).isTrue()
        assertThat(gate.decide()).isEqualTo(ProGate.Decision.ASK)
    }

    // A build that cannot sell must not ask. This is the state a release carries
    // while the key is still a sandbox one, and it used to strand people: the
    // free week ran out and the paywall had nothing on it.
    @Test
    fun `a build that cannot sell never asks`() {
        assertThat(gate(isPro = false, used = true, canSell = false).decide())
            .isEqualTo(ProGate.Decision.ALLOWED)
    }

    @Test
    fun `a build that cannot sell spends nothing`() {
        val allowance = FakeAllowance()
        gateOver(allowance, canSell = false).spend()

        assertThat(allowance.used).isFalse()
    }

    // A subscriber who lapses should still find the free week they never used.
    @Test
    fun `a subscriber spends nothing`() {
        val allowance = FakeAllowance()
        gateOver(allowance, isPro = true).spend()

        assertThat(allowance.used).isFalse()
    }

    // The defect this covers: a subscriber whose entitlement is still being
    // read is false here like everyone else, so the gate used to decide what
    // they are charged for from an answer the store had not given.
    @Test
    fun `a tap before the store has answered neither allows nor asks`() {
        assertThat(gate(isPro = false, used = true, hasRead = false).decide())
            .isEqualTo(ProGate.Decision.WAIT)
    }

    @Test
    fun `a subscriber is not asked while the read is still out`() {
        assertThat(gate(isPro = true, used = true, hasRead = false).decide())
            .isEqualTo(ProGate.Decision.WAIT)
    }

    // A failed read is not a pending one: nothing more is coming, so waiting
    // would strand the person. It falls in with the build that cannot sell,
    // which lets the week through rather than asking for money on a guess.
    @Test
    fun `a read that was never answered at all lets the week through`() {
        assertThat(gate(isPro = false, used = true, hasRead = false, readFailed = true).decide())
            .isEqualTo(ProGate.Decision.ALLOWED)
    }

    // The paywall re-reads, so a read can fail after one has already landed.
    // Treating that as an unknown store would throw away an answer it gave and
    // leave every gated week free for the rest of the session.
    @Test
    fun `a read that failed after an answer landed still asks`() {
        assertThat(gate(isPro = false, used = true, hasRead = true, readFailed = true).decide())
            .isEqualTo(ProGate.Decision.ASK)
    }

    @Test
    fun `a subscriber whose later read failed is still never asked`() {
        assertThat(gate(isPro = true, used = true, hasRead = true, readFailed = true).decide())
            .isEqualTo(ProGate.Decision.ALLOWED)
    }

    // The first plan is written with no gate decision at all, so the only thing
    // that can close the free allowance is this call. Skipping it while the
    // store is unreachable hands the free week back on every launch.
    @Test
    fun `the first week is charged even though the store never answered`() {
        val allowance = UnspentAllowance()
        gateOver(allowance, hasRead = false, readFailed = true).spend()

        assertThat(allowance.wasSpent).isTrue()
    }
}
