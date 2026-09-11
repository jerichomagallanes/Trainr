package com.jericx.trainr.domain.purchases

import com.jericx.trainr.domain.generation.PlanSource

// Whether a generation may start, kept apart from the navigation code so the
// answer can be tested without a screen. A route that forgets to ask spends
// real money on a model call.
class ProGate(
    private val isPro: () -> Boolean,
    // False when the purchases layer never came up: no key it can validate, or
    // the store refused to talk to us. Nothing can be bought in that state, so
    // asking someone to buy is asking the impossible.
    private val canSell: () -> Boolean,
    private val allowance: FreeGenerationAllowance
) {

    fun decide(): Decision = when {
        !canSell() -> Decision.ALLOWED
        isPro() -> Decision.ALLOWED
        !allowance.hasBeenUsed() -> Decision.ALLOWED
        else -> Decision.ASK
    }

    // Called once a week has actually arrived. Subscribers spend nothing, so
    // their first week stays available if they ever lapse, and neither does
    // anyone using a build that cannot sell them the alternative. Nor does a
    // week the app built in place of a failed answer: the client asked for the
    // coach and got a stand-in.
    fun spend(source: PlanSource) {
        if (source == PlanSource.TEMPLATE) return
        if (canSell() && !isPro()) allowance.markUsed()
    }

    enum class Decision { ALLOWED, ASK }
}
