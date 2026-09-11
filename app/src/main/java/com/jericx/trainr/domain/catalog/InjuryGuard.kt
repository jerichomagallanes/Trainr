package com.jericx.trainr.domain.catalog

import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.Injury

// Two tiers, because a declared injury rules out a few movements and only
// asks for care with most. A sweep by pattern would be simpler and would
// leave a client with a sore wrist and no equipment unable to press at all:
// every bodyweight press in the catalog is a push-up.
object InjuryGuard {

    // Never offered.
    fun excludes(exercise: CatalogExercise, injuries: Collection<Injury>): Boolean =
        injuries.any { exercise.isRuledOutBy(it) }

    // Offered, with a line on the card saying what to watch. One line, from
    // the first injury the client declared that the movement touches.
    fun cautionFor(exercise: CatalogExercise, injuries: List<Injury>): Injury? =
        injuries.firstOrNull { injury ->
            !exercise.isRuledOutBy(injury) && exercise.pattern in CautionPatterns.getValue(injury)
        }

    // Each clause is one the coaching brief already stated in prose; this is
    // it made enforceable.
    private fun CatalogExercise.isRuledOutBy(injury: Injury): Boolean = when (injury) {
        Injury.LOWER_BACK -> primary == MuscleGroup.LOWER_BACK ||
            (pattern == MovementPattern.HINGE && equipment in FloorLoaded) ||
            key in LoadedSpinalFlexion

        Injury.KNEE -> pattern == MovementPattern.LUNGE ||
            key in Plyometric || key in DeepKneeFlexion

        Injury.SHOULDER -> pattern == MovementPattern.VERTICAL_PUSH ||
            key in Dips || key in UprightRows

        Injury.WRIST -> key in WristLoaded

        Injury.ANKLE -> key in Plyometric || key in RunningImpact

        Injury.HIP -> (pattern == MovementPattern.LUNGE && equipment != Equipment.NONE) ||
            (pattern == MovementPattern.HINGE && equipment == Equipment.BARBELL)

        Injury.NECK -> primary == MuscleGroup.NECK || key in Shrugs
    }

    private val FloorLoaded = setOf(Equipment.BARBELL, Equipment.PLATE)

    private val LoadedSpinalFlexion = setOf(
        "weighted_sit_up", "weighted_crunch", "weighted_decline_crunch", "weighted_russian_twist"
    )

    private val Plyometric = setOf(
        "box_jump", "lateral_box_jump", "burpee", "burpee_broad_jumps", "burpee_over_the_bar",
        "frog_jumps", "jump_squat", "jumping_lunge", "jumping_jack", "high_knee_skips",
        "sprints", "jump_shrug"
    )

    private val DeepKneeFlexion = setOf(
        "pistol_squat", "assisted_pistol_squats", "weighted_sissy_squat"
    )

    private val Dips = setOf(
        "assisted_chest_dip", "assisted_triceps_dip", "bench_dip", "chest_dip",
        "floor_triceps_dip", "ring_dips", "seated_dip_machine", "triceps_dip",
        "weighted_chest_dip", "weighted_triceps_dip"
    )

    private val UprightRows = setOf(
        "barbell_upright_row", "cable_upright_row", "dumbbell_upright_row"
    )

    private val WristLoaded = setOf(
        "ab_wheel", "handstand_push_up", "handstand_hold", "front_squat",
        "clap_push_ups", "one_arm_push_up"
    )

    // Impact, not effort: walking on a treadmill or climbing stairs is fine
    // on a bad ankle and stays offered, with a caution.
    private val RunningImpact = setOf("running", "jump_rope", "sprints")

    private val Shrugs = setOf(
        "barbell_shrug", "cable_shrug", "dumbbell_shrug", "machine_shrug", "smith_machine_shrug"
    )

    // Every key named anywhere above, so a test can hold the list to the
    // catalog. A renamed key would otherwise stop excluding anything, silently.
    internal val NamedKeys: Set<String> =
        LoadedSpinalFlexion + Plyometric + DeepKneeFlexion + Dips + UprightRows +
            WristLoaded + RunningImpact + Shrugs

    private val CautionPatterns: Map<Injury, Set<MovementPattern>> = mapOf(
        Injury.LOWER_BACK to setOf(
            MovementPattern.HINGE, MovementPattern.SQUAT, MovementPattern.CARRY,
            MovementPattern.CORE
        ),
        Injury.KNEE to setOf(MovementPattern.SQUAT, MovementPattern.LUNGE),
        Injury.SHOULDER to setOf(
            MovementPattern.VERTICAL_PUSH, MovementPattern.HORIZONTAL_PUSH,
            MovementPattern.VERTICAL_PULL
        ),
        Injury.WRIST to setOf(
            MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH, MovementPattern.CARRY
        ),
        Injury.ANKLE to setOf(
            MovementPattern.SQUAT, MovementPattern.LUNGE, MovementPattern.CONDITIONING
        ),
        Injury.HIP to setOf(MovementPattern.HINGE, MovementPattern.SQUAT, MovementPattern.LUNGE),
        Injury.NECK to setOf(MovementPattern.CORE, MovementPattern.VERTICAL_PUSH)
    )
}
