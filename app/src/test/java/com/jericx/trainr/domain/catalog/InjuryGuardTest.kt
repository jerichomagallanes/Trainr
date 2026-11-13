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
            assertThat(InjuryGuard.cautionsFor(it, emptyList())).isEmpty()
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
        assertThat(InjuryGuard.cautionsFor(bench, listOf(Injury.SHOULDER)))
            .containsExactly(Injury.SHOULDER)
        assertThat(InjuryGuard.cautionsFor(squat, listOf(Injury.KNEE)))
            .containsExactly(Injury.KNEE)
        assertThat(InjuryGuard.cautionsFor(bench, listOf(Injury.KNEE))).isEmpty()
    }

    // A movement that is never offered has no card to put a caution on.
    @Test
    fun aRuledOutMovementIsNeverAlsoCautioned() {
        assertThat(InjuryGuard.cautionsFor(press, listOf(Injury.SHOULDER))).isEmpty()
    }

    // Every injury the movement touches gets its own line, so no declared
    // injury goes unheard.
    @Test
    fun everyInjuryThatTouchesAMovementIsCautioned() {
        assertThat(InjuryGuard.cautionsFor(squat, listOf(Injury.KNEE, Injury.ANKLE, Injury.HIP)))
            .containsExactly(Injury.KNEE, Injury.HIP, Injury.ANKLE)
    }

    // The client's answers arrive in whatever order they were tapped; the card
    // must not.
    @Test
    fun theLinesReadNarrowestFirstWhateverOrderTheInjuriesWereDeclaredIn() {
        val declaredOneWay = InjuryGuard.cautionsFor(squat, listOf(Injury.KNEE, Injury.LOWER_BACK))
        val declaredTheOther = InjuryGuard.cautionsFor(squat, listOf(Injury.LOWER_BACK, Injury.KNEE))

        assertThat(declaredOneWay).containsExactly(Injury.KNEE, Injury.LOWER_BACK).inOrder()
        assertThat(declaredTheOther).isEqualTo(declaredOneWay)
    }

    @Test
    fun anInjuryDeclaredTwiceIsStillOneLine() {
        assertThat(InjuryGuard.cautionsFor(squat, listOf(Injury.KNEE, Injury.KNEE)))
            .containsExactly(Injury.KNEE)
    }

    // Loaded from the floor is what a bad back cannot take; the same hinge
    // with a light dumbbell is offered with care.
    @Test
    fun aBadBackRulesOutABarbellHingeButNotADumbbellOne() {
        val dumbbellHinge = movement("dumbbell_romanian_deadlift", MovementPattern.HINGE, Equipment.DUMBBELL)

        assertThat(InjuryGuard.excludes(deadlift, listOf(Injury.LOWER_BACK))).isTrue()
        assertThat(InjuryGuard.excludes(dumbbellHinge, listOf(Injury.LOWER_BACK))).isFalse()
        assertThat(InjuryGuard.cautionsFor(dumbbellHinge, listOf(Injury.LOWER_BACK)))
            .containsExactly(Injury.LOWER_BACK)
    }

    // Every injury has to be able to say something, or declaring it changes
    // nothing on any card.
    @Test
    fun everyInjuryCautionsAtLeastOneKindOfMovement() {
        val patterns = MovementPattern.entries.map { movement("probe_$it", it) }

        Injury.entries.forEach { injury ->
            assertThat(patterns.flatMap { InjuryGuard.cautionsFor(it, listOf(injury)) })
                .isNotEmpty()
        }
    }
}
