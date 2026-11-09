package com.jericx.trainr.domain.purchases

// Whether a generation may start, kept apart from navigation so the answer
// can be tested without a screen.
class ProGate(
    private val isPro: () -> Boolean,
    // False until the store has answered. Without it a subscriber mid-read is
    // the same false as someone who has never paid, and the gate charges them
    // for a week they already own.
    private val hasRead: () -> Boolean,
    // The store was asked and gave up. Nothing more is coming, so a tap cannot
    // be left waiting for it the way an unread answer can.
    private val readFailed: () -> Boolean,
    // False when the purchases layer never came up: no key it can validate, or
    // the store refused to talk to us. Nothing can be bought in that state, so
    // asking someone to buy is asking the impossible.
    private val canSell: () -> Boolean,
    private val allowance: FreeGenerationAllowance
) {

    fun decide(): Decision = when {
        !canSell() -> Decision.ALLOWED
        // Read as the same state as a build that cannot sell: the store cannot
        // say who this is, so nobody is asked for money on a guess, and spend()
        // below takes nothing for it either.
        readFailed() -> Decision.ALLOWED
        !hasRead() -> Decision.WAIT
        isPro() -> Decision.ALLOWED
        !allowance.hasBeenUsed() -> Decision.ALLOWED
        else -> Decision.ASK
    }

    // Called once a week has actually arrived: the free week is a week of
    // training, spent exactly once and only when something can be sold.
    // Subscribers spend nothing, so their first week stays available if they
    // ever lapse, and neither does anyone using a build that cannot sell them
    // the alternative, nor anyone the store has not answered for.
    fun spend() {
        if (canSell() && hasRead() && !readFailed() && !isPro()) allowance.markUsed()
    }

    // WAIT is neither: the answer is still on its way, so the person is told to
    // try again rather than silently refused or silently let through.
    enum class Decision { ALLOWED, ASK, WAIT }
}
