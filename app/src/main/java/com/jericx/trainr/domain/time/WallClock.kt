package com.jericx.trainr.domain.time

// The calendar date, not an interval: a screen open across midnight has to be
// able to read the day it is on now rather than the one it was opened on.
fun interface WallClock {
    fun nowMillis(): Long
}
