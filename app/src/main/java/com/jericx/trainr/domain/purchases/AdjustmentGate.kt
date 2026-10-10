package com.jericx.trainr.domain.purchases

// Whether a changed proposal may be offered, kept apart from navigation so the
// answer can be tested without a screen.
class AdjustmentGate(
    private val isPro: () -> Boolean,
    // False when the purchases layer never came up: no key it can validate, or
    // the store refused to talk to us. Nothing can be bought in that state, so
    // asking someone to buy is asking the impossible.
    private val canSell: () -> Boolean,
    private val allowance: AdjustmentAllowance
) {

    fun decide(cycleId: String?): Decision = when {
        !canSell() -> Decision.ALLOWED
        isPro() -> Decision.ALLOWED
        allowance.includedCycleId() == null -> Decision.ALLOWED
        // The cycle that spent the allowance is the one that was paid for: its
        // undo, its reapply and its follow-up are not a second adjustment.
        allowance.includedCycleId() == cycleId -> Decision.ALLOWED
        else -> Decision.ASK
    }

    // Called once a change has actually been applied: the included cycle is a
    // cycle of training, spent exactly once and only when something can be
    // sold. Subscribers spend nothing, so the cycle is still there if they ever
    // lapse, and neither does anyone using a build that cannot sell them the
    // alternative.
    fun spend(cycleId: String) {
        if (canSell() && !isPro()) allowance.consume(cycleId)
    }

    enum class Decision { ALLOWED, ASK }
}
