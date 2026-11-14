package com.jericx.trainr.presentation.workout.model

// A countdown that knows when it ends rather than counting ticks: a late tick,
// or a spell in the background, otherwise loses time for good.
data class ExerciseTimerUi(
    val position: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val totalSeconds: Int = remainingSeconds,
    // The row the countdown was started on: a reload may leave a different
    // movement at that position, and the hold must not follow it there.
    val exerciseId: Long = 0,
    // Null while paused, when remainingSeconds is the truth instead.
    val endsAtMillis: Long? = null,
    val isFinished: Boolean = false
) {
    val display: String
        get() = "${remainingSeconds / SECONDS_PER_MINUTE}:" +
            (remainingSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')

    // Whole seconds, rounded up: zero only once the interval has truly ended.
    fun remaining(nowMillis: Long): Int {
        val endsAt = endsAtMillis ?: return remainingSeconds
        val left = endsAt - nowMillis
        return ((left + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(0).toInt()
    }

    fun advanced(nowMillis: Long) = copy(remainingSeconds = remaining(nowMillis))

    fun paused(nowMillis: Long) = copy(
        remainingSeconds = remaining(nowMillis),
        isRunning = false,
        endsAtMillis = null
    )

    fun resumed(nowMillis: Long) = copy(
        isRunning = true,
        endsAtMillis = nowMillis + remainingSeconds * MILLIS_PER_SECOND
    )

    fun finished() = copy(
        remainingSeconds = 0,
        isRunning = false,
        endsAtMillis = null,
        isFinished = true
    )

    fun reset() = copy(
        remainingSeconds = totalSeconds,
        isRunning = false,
        endsAtMillis = null,
        isFinished = false
    )

    companion object {
        fun running(position: Int, exerciseId: Long, totalSeconds: Int, nowMillis: Long) =
            ExerciseTimerUi(
                position = position,
                remainingSeconds = totalSeconds,
                isRunning = true,
                totalSeconds = totalSeconds,
                exerciseId = exerciseId,
                endsAtMillis = nowMillis + totalSeconds * MILLIS_PER_SECOND
            )

        private const val SECONDS_PER_MINUTE = 60
        private const val MILLIS_PER_SECOND = 1000L
    }
}
