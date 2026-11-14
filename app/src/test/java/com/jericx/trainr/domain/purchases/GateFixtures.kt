package com.jericx.trainr.domain.purchases

class UnspentAllowance : FreeGenerationAllowance {
    var wasSpent = false
        private set

    override fun hasBeenUsed() = wasSpent

    override fun markUsed() {
        wasSpent = true
    }
}

fun gateOver(
    allowance: FreeGenerationAllowance,
    isPro: Boolean = false,
    hasRead: Boolean = true,
    readFailed: Boolean = false,
    canSell: Boolean = true
) = ProGate(
    isPro = { isPro },
    hasRead = { hasRead },
    readFailed = { readFailed },
    canSell = { canSell },
    allowance = allowance
)
