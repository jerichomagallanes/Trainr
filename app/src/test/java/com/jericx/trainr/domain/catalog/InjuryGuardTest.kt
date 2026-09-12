package com.jericx.trainr.domain.catalog

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.Injury
import org.junit.Test

class InjuryGuardTest {

    private fun movement(
        key: String,
        pattern: MovementPattern,
        equipment: Equipment = Equipment.NONE,
        primary: MuscleGroup = MuscleGroup.CHEST
    ) = CatalogExercise(
        key = key, name = key, primary = primary, secondary = emptyList(),
        equipment = equipment, measure = ExerciseMeasure.REPS, pattern = pattern, staple = false
    )

    private val squat = movement("bodyweight_squat", MovementPattern.SQUAT)
    private val bench = movement("barbell_bench_press", MovementPattern.HORIZONTAL_PUSH, Equipment.BARBELL)
    private val press = movement("overhead_press", MovementPattern.VERTICAL_PUSH, Equipment.BARBELL)
    private val deadlift = movement("deadlift", MovementPattern.HINGE, Equipment.BARBELL)

    @Test
    fun noInjuryRulesNothingOutAndCautionsNothing() {
        listOf(squat, bench, press, deadlift).forEach {
            assertThat(InjuryGuard.excludes(it, emptyList())).isFalse()
            assertThat(InjuryGuard.cautionFor(it, emptyList())).isNull()
        }
    }

    // The brief already said no overhead pressing for a shoulder; this is
    // that, made impossible to ignore.
    @Test
    fun aShoulderInjuryRulesOutOverheadPressingButNotABenchPress() {
        assertThat(InjuryGuard.excludes(press, listOf(Injury.SHOULDER))).isTrue()
        assertThat(InjuryGuard.excludes(bench, listOf(Injury.SHOULDER))).isFalse()
    }

    // Care rather than refusal: a bench press is still offered to a sore
    // shoulder, with a line saying what to watch.
    @Test
    fun aMovementThatTouchesAnInjuryIsOfferedWithACaution() {
        assertThat(InjuryGuard.cautionFor(bench, listOf(Injury.SHOULDER)))
            .isEqualTo(Injury.SHOULDER)
        assertThat(InjuryGuard.cautionFor(squat, listOf(Injury.KNEE))).isEqualTo(Injury.KNEE)
        assertThat(InjuryGuard.cautionFor(bench, listOf(Injury.KNEE))).isNull()
    }

    // A movement that is never offered has no card to put a caution on.
    @Test
    fun aRuledOutMovementIsNeverAlsoCautioned() {
        assertThat(InjuryGuard.cautionFor(press, listOf(Injury.SHOULDER))).isNull()
    }

    // One line per card, from the injury the client named first.
    @Test
    fun twoInjuriesThatBothTouchAMovementGiveTheOneDeclaredFirst() {
        assertThat(InjuryGuard.cautionFor(squat, listOf(Injury.KNEE, Injury.HIP)))
            .isEqualTo(Injury.KNEE)
        assertThat(InjuryGuard.cautionFor(squat, listOf(Injury.HIP, Injury.KNEE)))
            .isEqualTo(Injury.HIP)
    }

    // Loaded from the floor is what a bad back cannot take; the same hinge
    // with a light dumbbell is offered with care.
    @Test
    fun aBadBackRulesOutABarbellHingeButNotADumbbellOne() {
        val dumbbellHinge = movement("dumbbell_romanian_deadlift", MovementPattern.HINGE, Equipment.DUMBBELL)

        assertThat(InjuryGuard.excludes(deadlift, listOf(Injury.LOWER_BACK))).isTrue()
        assertThat(InjuryGuard.excludes(dumbbellHinge, listOf(Injury.LOWER_BACK))).isFalse()
        assertThat(InjuryGuard.cautionFor(dumbbellHinge, listOf(Injury.LOWER_BACK)))
            .isEqualTo(Injury.LOWER_BACK)
    }

    // Every injury has to be able to say something, or declaring it changes
    // nothing on any card.
    @Test
    fun everyInjuryCautionsAtLeastOneKindOfMovement() {
        val patterns = MovementPattern.entries.map { movement("probe_$it", it) }

        Injury.entries.forEach { injury ->
            assertThat(patterns.mapNotNull { InjuryGuard.cautionFor(it, listOf(injury)) })
                .isNotEmpty()
        }
    }
}
