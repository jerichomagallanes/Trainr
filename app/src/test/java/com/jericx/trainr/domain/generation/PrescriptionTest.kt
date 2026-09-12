package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import org.junit.Test

class PrescriptionTest {

    private fun reps(vararg counts: Int) =
        counts.mapIndexed { i, r -> ExerciseSet(setNumber = i + 1, targetReps = r) }

    private fun seconds(vararg counts: Int) =
        counts.mapIndexed { i, s -> ExerciseSet(setNumber = i + 1, targetSeconds = s) }

    @Test
    fun setsThatAllAskForTheSameThingReadAsOneNumber() {
        val chip = Prescription.of(reps(10, 10, 10), ExerciseMeasure.WEIGHT_AND_REPS)

        assertThat(chip).isEqualTo(
            Prescription.Fixed(3, PrescriptionUnit.REPS, 10, perSide = false)
        )
    }

    // A week that drops a rep as the weight climbs still has to describe
    // itself in one chip.
    @Test
    fun setsThatDifferReadAsARange() {
        val chip = Prescription.of(reps(12, 10, 8), ExerciseMeasure.WEIGHT_AND_REPS)

        assertThat(chip).isEqualTo(
            Prescription.Spread(3, PrescriptionUnit.REPS, 8, 12, perSide = false)
        )
    }

    @Test
    fun aMovementWorkedOneSideAtATimeSaysSo() {
        val chip = Prescription.of(
            reps(10, 10), ExerciseMeasure.WEIGHT_AND_REPS, unilateral = true
        )

        assertThat((chip as Prescription.Fixed).perSide).isTrue()
    }

    // A hold is the whole set however many sides it is held on, so the chip
    // must not double it.
    @Test
    fun aTimedHoldIsNeverPerSide() {
        val chip = Prescription.of(seconds(45, 45), ExerciseMeasure.DURATION, unilateral = true)

        assertThat((chip as Prescription.Fixed).perSide).isFalse()
    }

    @Test
    fun ashortHoldIsCountedInSeconds() {
        val chip = Prescription.of(seconds(45), ExerciseMeasure.DURATION)

        assertThat(chip).isEqualTo(
            Prescription.Fixed(1, PrescriptionUnit.SECONDS, 45, perSide = false)
        )
    }

    // Five minutes of walking is five minutes, not three hundred seconds.
    @Test
    fun aLongWholeMinuteEffortIsCountedInMinutes() {
        val chip = Prescription.of(seconds(300), ExerciseMeasure.DURATION)

        assertThat(chip).isEqualTo(
            Prescription.Fixed(1, PrescriptionUnit.MINUTES, 5, perSide = false)
        )
    }

    // Ninety seconds is not a minute and a half on a chip.
    @Test
    fun aLongEffortThatIsNotWholeMinutesStaysInSeconds() {
        val chip = Prescription.of(seconds(150), ExerciseMeasure.DURATION)

        assertThat((chip as Prescription.Fixed).unit).isEqualTo(PrescriptionUnit.SECONDS)
    }

    @Test
    fun anExerciseWithNoSetsHasNothingToSay() {
        assertThat(Prescription.of(emptyList(), ExerciseMeasure.REPS))
            .isEqualTo(Prescription.None)
    }

    // A set row with no target yet is not a prescription of zero.
    @Test
    fun setsCarryingNoTargetHaveNothingToSay() {
        val blank = listOf(ExerciseSet(setNumber = 1))

        assertThat(Prescription.of(blank, ExerciseMeasure.WEIGHT_AND_REPS))
            .isEqualTo(Prescription.None)
    }

    @Test
    fun bodyweightRepsReadTheSameWayAsWeightedOnes() {
        val chip = Prescription.of(reps(15, 15), ExerciseMeasure.REPS)

        assertThat(chip).isEqualTo(
            Prescription.Fixed(2, PrescriptionUnit.REPS, 15, perSide = false)
        )
    }
}
