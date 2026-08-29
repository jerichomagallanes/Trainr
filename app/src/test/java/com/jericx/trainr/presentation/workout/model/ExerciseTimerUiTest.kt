package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseTimerUiTest {

    private fun display(seconds: Int) =
        ExerciseTimerUi(position = 1, remainingSeconds = seconds, isRunning = true).display

    @Test
    fun readsAsMinutesAndPaddedSeconds() {
        assertThat(display(58)).isEqualTo("0:58")
        assertThat(display(600)).isEqualTo("10:00")
    }

    @Test
    fun padsASingleDigitOfSeconds() {
        assertThat(display(5)).isEqualTo("0:05")
        assertThat(display(65)).isEqualTo("1:05")
    }

    @Test
    fun anExhaustedTimerReadsZero() {
        assertThat(display(0)).isEqualTo("0:00")
    }

    @Test
    fun minutesAreNotWrappedIntoHours() {
        assertThat(display(3600)).isEqualTo("60:00")
    }
}
