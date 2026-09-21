package com.jericx.trainr.domain.unstuck

// Derived from the session the client answered for, not the prototype's
// 25/35/45, which were drawn for a 45 minute day.
object TimePresets {

    fun forPlanned(plannedMinutes: Int): List<Int> =
        (OPTIONS - 1 downTo 0).map { plannedMinutes - it * STEP_MINUTES }
            .filter { it >= FLOOR_MINUTES }
            .ifEmpty { listOf(plannedMinutes) }

    // The schema's 1..1440 is a parser bound. This is the reviewed range, and
    // a request outside it is refused rather than clamped into a different one.
    fun isSupported(minutes: Int): Boolean = minutes in SUPPORTED_MINUTES

    private val SUPPORTED_MINUTES = 5..180
    private const val OPTIONS = 3
    private const val STEP_MINUTES = 10
    private const val FLOOR_MINUTES = 10
}
