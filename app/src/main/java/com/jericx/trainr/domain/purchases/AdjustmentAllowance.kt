package com.jericx.trainr.domain.purchases

// One complete adjustment cycle is included, recorded as the proposal whose
// application spent it so that undoing, reapplying and answering the follow-up
// of that same cycle stay inside what was already given away. Held apart from
// the free week: neither allowance may spend or reset the other.
interface AdjustmentAllowance {
    fun includedCycleId(): String?
    fun consume(cycleId: String)
}
