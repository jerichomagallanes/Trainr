package com.jericx.trainr.domain.catalog

// The muscle a movement is prescribed for. One prime mover per exercise: a
// plan that credits a row to four muscles cannot be checked for balance.
enum class MuscleGroup(val region: MuscleRegion) {
    ABDOMINALS(MuscleRegion.CORE),
    BICEPS(MuscleRegion.ARMS),
    CHEST(MuscleRegion.CHEST),
    FOREARMS(MuscleRegion.ARMS),
    LATS(MuscleRegion.BACK),
    LOWER_BACK(MuscleRegion.CORE),
    NECK(MuscleRegion.OTHER),
    SHOULDERS(MuscleRegion.SHOULDERS),
    TRAPS(MuscleRegion.BACK),
    TRICEPS(MuscleRegion.ARMS),
    UPPER_BACK(MuscleRegion.BACK),
    ABDUCTORS(MuscleRegion.HIPS),
    ADDUCTORS(MuscleRegion.HIPS),
    CALVES(MuscleRegion.CALVES),
    GLUTES(MuscleRegion.HIPS),
    HAMSTRINGS(MuscleRegion.HAMSTRINGS),
    QUADRICEPS(MuscleRegion.QUADS),
    CARDIO(MuscleRegion.OTHER),
    FULL_BODY(MuscleRegion.OTHER),
    OTHER(MuscleRegion.OTHER)
}

// Volume is counted per region, not per muscle. Twice a week for each of
// twenty muscles is a week nobody has; twice a week for each of nine trainable
// regions is what the frequency evidence actually asks for.
enum class MuscleRegion {
    CHEST,
    BACK,
    SHOULDERS,
    ARMS,
    CORE,
    QUADS,
    HAMSTRINGS,
    HIPS,
    CALVES,
    OTHER;

    val isTrainable: Boolean get() = this != OTHER
}

// What the movement does, so a week can be checked for the patterns that earn
// the most for their time rather than for a list of names.
enum class MovementPattern {
    SQUAT,
    HINGE,
    LUNGE,
    HORIZONTAL_PUSH,
    VERTICAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PULL,
    CARRY,
    CORE,
    ISOLATION,
    CONDITIONING,
    MOBILITY;

    val isLowerPush: Boolean get() = this == SQUAT || this == LUNGE
    val isPush: Boolean get() = this == HORIZONTAL_PUSH || this == VERTICAL_PUSH
    val isPull: Boolean get() = this == HORIZONTAL_PULL || this == VERTICAL_PULL
}
