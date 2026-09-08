package com.jericx.trainr.domain.generation

// The free allowance is counted per model per day, so a model that refused this
// morning refuses all afternoon: re-asking costs a round trip for a certain no.
interface SpentModels {

    fun spentToday(): Set<String>

    fun markSpent(model: String)
}
