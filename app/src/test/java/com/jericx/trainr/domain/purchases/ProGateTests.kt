package com.jericx.trainr.domain.purchases

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanSource
import org.junit.Test

class ProGateTests {

    private class FakeAllowance(var used: Boolean = false) : FreeGenerationAllowance {
        override fun hasBeenUsed() = used
        override fun markUsed() {
            used = true
        }
    }

    private fun gate(isPro: Boolean, used: Boolean, canSell: Boolean = true) =
        ProGate(isPro = { isPro }, canSell = { canSell }, allowance = FakeAllowance(used))

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
        val gate = ProGate(isPro = { false }, canSell = { true }, allowance = allowance)

        assertThat(gate.decide()).isEqualTo(ProGate.Decision.ALLOWED)
        gate.spend(PlanSource.COACH)

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
        ProGate(isPro = { false }, canSell = { false }, allowance = allowance).spend(PlanSource.COACH)

        assertThat(allowance.used).isFalse()
    }

    // A subscriber who lapses should still find the free week they never used.
    @Test
    fun `a subscriber spends nothing`() {
        val allowance = FakeAllowance()
        ProGate(isPro = { true }, canSell = { true }, allowance = allowance).spend(PlanSource.COACH)

        assertThat(allowance.used).isFalse()
    }

    @Test
    fun `a week built in place of the coach's spends nothing`() {
        val allowance = FakeAllowance()
        ProGate(isPro = { false }, canSell = { true }, allowance = allowance).spend(PlanSource.TEMPLATE)

        assertThat(allowance.used).isFalse()
    }

    @Test
    fun `a week carried forward spends the free week like a coached one`() {
        val allowance = FakeAllowance()
        ProGate(isPro = { false }, canSell = { true }, allowance = allowance).spend(PlanSource.PROGRESSED)

        assertThat(allowance.used).isTrue()
    }
}
