package com.jericx.trainr.data.generation

import kotlinx.serialization.Serializable

// Everything a model is now asked for: a title per day and one movement per
// open slot, keyed by the skeleton's own ids. Every field defaults, because a
// half-written answer is worth completing and a rejected one costs a whole
// round trip. An empty selection is a complete answer: the top of every list.
@Serializable
data class PlanSelection(val days: Map<String, DaySelection> = emptyMap())

@Serializable
data class DaySelection(
    val slots: Map<String, String> = emptyMap(),
    val title: String = ""
)
