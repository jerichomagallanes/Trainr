package com.jericx.trainr.domain.time

// Time since the device booted, not the wall clock: a countdown measures an
// interval, and a clock the system or the person can set would cut a hold short
// and log it as held in full.
fun interface ElapsedClock {
    fun elapsedMillis(): Long
}
