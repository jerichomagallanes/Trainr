package com.jericx.trainr.llama

import kotlin.time.Duration

// A read is bounded by the time it was actually given, not by the clock. The
// process is frozen ten seconds after it goes to the background, so a call taken
// mid-read stops the model dead while every wall-clock budget keeps draining;
// charging a slice at a time charges a freeze once instead of for its length.
class ReadBudget(private val budget: Duration, private val slice: Duration) {

    private var spent = Duration.ZERO

    val isSpent: Boolean get() = spent >= budget

    val nextSlice: Duration get() = minOf(slice, budget - spent)

    fun charge(elapsed: Duration) {
        spent += minOf(elapsed, slice)
    }
}
