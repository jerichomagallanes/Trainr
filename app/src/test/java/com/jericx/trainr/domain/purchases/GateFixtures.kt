package com.jericx.trainr.domain.purchases

class UnspentAllowance : FreeGenerationAllowance {
    var wasSpent = false
        private set

    override fun hasBeenUsed() = wasSpent

    override fun markUsed() {
        wasSpent = true
    }
}

fun gateOver(allowance: FreeGenerationAllowance) =
    ProGate(isPro = { false }, canSell = { true }, allowance = allowance)
