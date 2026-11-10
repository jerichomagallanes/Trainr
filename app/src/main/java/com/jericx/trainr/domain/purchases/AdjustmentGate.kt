package com.jericx.trainr.domain.purchases

// Whether a changed proposal may be offered, kept apart from navigation so the
// answer can be tested without a screen.
class AdjustmentGate(
    private val isPro: () -> Boolean,
    // False until the store has answered. Without it a subscriber mid-read is
    // the same false as someone who has never paid, and the gate charges them
    // for an adjustment they already own.
    private val hasRead: () -> Boolean,
    // The store was asked and gave up. Nothing more is coming, so a tap cannot
    // be left waiting for it the way an unread answer can.
    private val readFailed: () -> Boolean,
    // False when the purchases layer never came up: no key it can validate, or
    // the store refused to talk to us. Nothing can be bought in that state, so
    // asking someone to buy is asking the impossible.
    private val canSell: () -> Boolean,
    private val allowance: AdjustmentAllowance
) {

    fun decide(cycleId: String?): Decision = when {
        !canSell() -> Decision.ALLOWED
        // Read as the same state as a build that cannot sell, but only while the
        // store has never answered at all: it cannot say who this is, so nobody
        // is asked for money on a guess. A failure after an answer landed is a
        // different thing, and throwing that answer away would unlock every
        // adjustment for the rest of the session.
        readFailed() && !hasRead() -> Decision.ALLOWED
        !hasRead() -> Decision.WAIT
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
    // alternative. Nor does an unanswered store: nothing is adjusted without
    // this gate deciding first, so a change that got through while the read was
    // out got through on a guess, and the cycle is what the person paid for.
    fun spend(cycleId: String) {
        if (canSell() && hasRead() && !isPro()) allowance.consume(cycleId)
    }

    // WAIT is neither: the answer is still on its way, so the person is told to
    // try again rather than silently refused or silently let through.
    enum class Decision { ALLOWED, ASK, WAIT }
}
