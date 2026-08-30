package com.jericx.trainr.presentation.workout.components

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import org.junit.Test

class ExerciseSetTableTest {

    private fun set(reps: Int? = null, weightKg: Float? = null, seconds: Int? = null) =
        ExerciseSet(
            setNumber = 1,
            actualReps = reps,
            actualWeightKg = weightKg,
            actualSeconds = seconds
        )

    @Test
    fun aLoadedSetReadsAsWeightTimesReps() {
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, set(reps = 12, weightKg = 20f)))
            .isEqualTo("20kg × 12")
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, set(reps = 10, weightKg = 22.5f)))
            .isEqualTo("22.5kg × 10")
    }

    @Test
    fun aBodyweightSetReadsAsThePlainCount() {
        assertThat(previousCellText(ExerciseMeasure.REPS, set(reps = 15))).isEqualTo("15")
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, set(reps = 12)))
            .isEqualTo("12")
    }

    @Test
    fun aTimedSetReadsInTheTimersClockLanguage() {
        assertThat(previousCellText(ExerciseMeasure.DURATION, set(seconds = 45))).isEqualTo("0:45")
        assertThat(previousCellText(ExerciseMeasure.DURATION, set(seconds = 300))).isEqualTo("5:00")
    }

    @Test
    fun secondsFormatAsMinutesAndSeconds() {
        assertThat(formatSeconds(45)).isEqualTo("0:45")
        assertThat(formatSeconds(300)).isEqualTo("5:00")
        assertThat(formatSeconds(605)).isEqualTo("10:05")
    }

    // Digits fill in from the seconds end, the way a microwave timer is typed.
    @Test
    fun typedDigitsReadAsMinutesThenSeconds() {
        assertThat(secondsFromDigits("5")).isEqualTo(5)
        assertThat(secondsFromDigits("45")).isEqualTo(45)
        assertThat(secondsFromDigits("500")).isEqualTo(300)
        assertThat(secondsFromDigits("1230")).isEqualTo(750)
        assertThat(secondsFromDigits("")).isNull()
    }

    @Test
    fun storedSecondsRoundTripThroughTheirDigits() {
        for (seconds in listOf(5, 45, 90, 300, 750)) {
            assertThat(secondsFromDigits(durationDigits(seconds))).isEqualTo(seconds)
        }
    }

    // A set that was prescribed but never logged must show a dash, not its
    // target — the column reports what happened, not what was asked.
    @Test
    fun anUnloggedSetShowsADash() {
        assertThat(previousCellText(ExerciseMeasure.WEIGHT_AND_REPS, null)).isEqualTo("—")
        assertThat(
            previousCellText(
                ExerciseMeasure.WEIGHT_AND_REPS,
                set().copy(targetReps = 12, targetWeightKg = 20f)
            )
        ).isEqualTo("—")
        assertThat(previousCellText(ExerciseMeasure.REPS, set())).isEqualTo("—")
        assertThat(previousCellText(ExerciseMeasure.DURATION, set())).isEqualTo("—")
    }
}
