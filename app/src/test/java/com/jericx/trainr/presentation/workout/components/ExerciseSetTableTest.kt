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
    fun aTimedSetReadsAsItsSeconds() {
        assertThat(previousCellText(ExerciseMeasure.DURATION, set(seconds = 45))).isEqualTo("45")
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
