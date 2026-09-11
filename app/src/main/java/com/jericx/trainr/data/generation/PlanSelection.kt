package com.jericx.trainr.data.generation

// One movement per open slot and a title per day, keyed by the skeleton's ids.
data class PlanSelection(val days: Map<String, DaySelection> = emptyMap())

data class DaySelection(
    val slots: Map<String, String> = emptyMap(),
    val title: String = ""
)
