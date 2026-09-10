package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import org.junit.Test

class LoadStepTest {

    private fun movement(
        equipment: Equipment,
        measure: ExerciseMeasure = ExerciseMeasure.WEIGHT_AND_REPS
    ) = CatalogExercise(
        key = "movement",
        name = "Movement",
        primary = MuscleGroup.CHEST,
        secondary = emptyList(),
        equipment = equipment,
        measure = measure,
        pattern = MovementPattern.HORIZONTAL_PUSH,
        staple = true
    )

    private fun pounds(kg: Float) = WeightUnit.forDisplay(kg, UnitSystem.IMPERIAL)

    // A bar is 20 kg before a plate goes on it, and plates go on in pairs, so
    // every loadable weight is the bar plus a multiple of the smallest pair.
    @Test
    fun aBarbellIsLoadedFromAnEmptyBarInPlatePairs() {
        val bar = movement(Equipment.BARBELL)

        assertThat(LoadStep.snap(22.5f, bar, UnitSystem.METRIC)).isEqualTo(22.5f)
        assertThat(LoadStep.snap(23.4f, bar, UnitSystem.METRIC)).isEqualTo(22.5f)
        assertThat(LoadStep.snap(5f, bar, UnitSystem.METRIC)).isEqualTo(20f)
    }

    // Never below the empty bar: there is no such thing as a 10 kg barbell
    // bench press.
    @Test
    fun aBarbellIsNeverSnappedBelowTheEmptyBar() {
        val bar = movement(Equipment.BARBELL)

        assertThat(LoadStep.snap(0f, bar, UnitSystem.METRIC)).isEqualTo(20f)
        assertThat(LoadStep.lightest(bar, UnitSystem.METRIC)).isEqualTo(20f)
    }

    // An imperial gym stocks five-pound plates, not converted kilograms, so
    // the rungs have to be whole pounds.
    @Test
    fun animperialGymGetsWholePoundsRatherThanConvertedKilograms() {
        val bar = movement(Equipment.BARBELL)

        val snapped = LoadStep.snap(60f, bar, UnitSystem.IMPERIAL)

        assertThat(pounds(snapped) % 5f).isWithin(0.01f).of(0f)
    }

    // The number is what is stamped on the one bell the client picks up.
    @Test
    fun aDumbbellClimbsInWhatIsStampedOnOneBell() {
        val bell = movement(Equipment.DUMBBELL)

        assertThat(LoadStep.snap(11f, bell, UnitSystem.METRIC)).isEqualTo(10f)
        assertThat(LoadStep.snap(11.5f, bell, UnitSystem.METRIC)).isEqualTo(12.5f)
    }

    // Bells are cast in sizes, so 18 kg is not a kettlebell however neatly it
    // divides.
    @Test
    fun aKettlebellClimbsTheSizesItIsCastIn() {
        val bell = movement(Equipment.KETTLEBELL)

        assertThat(LoadStep.snap(18f, bell, UnitSystem.METRIC)).isEqualTo(16f)
        assertThat(LoadStep.snap(30f, bell, UnitSystem.METRIC)).isEqualTo(28f)
        assertThat(LoadStep.nextUp(16f, bell, UnitSystem.METRIC)).isEqualTo(20f)
    }

    // A week that adds "at least one increment" must never round back onto the
    // weight it started from.
    @Test
    fun theNextWeightUpIsAlwaysStrictlyHeavier() {
        listOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE, Equipment.KETTLEBELL)
            .forEach { kit ->
                val movement = movement(kit)
                listOf(UnitSystem.METRIC, UnitSystem.IMPERIAL).forEach { units ->
                    var weight = LoadStep.lightest(movement, units)
                    repeat(8) {
                        val next = LoadStep.nextUp(weight, movement, units)
                        assertThat(next).isGreaterThan(weight)
                        weight = next
                    }
                }
            }
    }

    // Nothing is loadable below the lightest thing on the rack.
    @Test
    fun steppingDownStopsAtTheLightestLoadThereIs() {
        val bell = movement(Equipment.DUMBBELL)

        val floor = LoadStep.lightest(bell, UnitSystem.METRIC)

        assertThat(LoadStep.nextDown(floor, bell, UnitSystem.METRIC)).isEqualTo(floor)
    }

    // A movement with no weight to choose is left exactly as it was.
    @Test
    fun aMovementWithNoLoadIsLeftAlone() {
        val pushUp = movement(Equipment.NONE, ExerciseMeasure.REPS)

        assertThat(LoadStep.snap(37f, pushUp, UnitSystem.METRIC)).isEqualTo(37f)
        assertThat(LoadStep.stepFractionOf(37f, pushUp, UnitSystem.METRIC)).isEqualTo(0f)
    }

    // A five-kilo jump on a light cable is most of the load again, which is
    // what tells the progression rules to add reps instead.
    @Test
    fun aLightMachineReportsItsIncrementAsALargeShareOfTheLoad() {
        val cable = movement(Equipment.MACHINE)

        assertThat(LoadStep.stepFractionOf(10f, cable, UnitSystem.METRIC)).isGreaterThan(0.1f)
        assertThat(LoadStep.stepFractionOf(100f, cable, UnitSystem.METRIC)).isLessThan(0.1f)
    }

    // No plan may ask for a weight the kit does not go up to.
    @Test
    fun noSnappedWeightExceedsWhatTheKitGoesUpTo() {
        Equipment.entries.forEach { kit ->
            val movement = movement(kit)
            val snapped = LoadStep.snap(9_000f, movement, UnitSystem.METRIC)

            assertThat(snapped).isAtMost(LoadStep.ceilingKg(kit))
        }
    }
}
