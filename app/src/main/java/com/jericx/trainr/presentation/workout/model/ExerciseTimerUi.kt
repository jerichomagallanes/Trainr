package com.jericx.trainr.presentation.workout.model

data class ExerciseTimerUi(
    val position: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val totalSeconds: Int = remainingSeconds
) {
    val display: String
        get() = "${remainingSeconds / SECONDS_PER_MINUTE}:" +
            (remainingSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}
