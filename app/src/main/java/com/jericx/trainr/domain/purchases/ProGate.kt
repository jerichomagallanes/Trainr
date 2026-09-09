package com.jericx.trainr.domain.purchases

// Whether a generation may start, kept apart from the navigation code so the
// answer can be tested without a screen. A route that forgets to ask spends
// real money on a model call.
class ProGate(
    private val isPro: () -> Boolean,
    private val allowance: FreeGenerationAllowance
) {

    fun decide(): Decision = when {
        isPro() -> Decision.ALLOWED
        !allowance.hasBeenUsed() -> Decision.ALLOWED
        else -> Decision.ASK
    }

    // Called once a generation is actually under way. Subscribers spend
    // nothing, so their first week stays available if they ever lapse.
    fun spend() {
        if (!isPro()) allowance.markUsed()
    }

    enum class Decision { ALLOWED, ASK }
}
