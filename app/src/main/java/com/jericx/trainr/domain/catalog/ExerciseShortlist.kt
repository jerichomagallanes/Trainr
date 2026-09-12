package com.jericx.trainr.domain.catalog

import com.jericx.trainr.domain.model.FitnessGoal

object ExerciseShortlist {

    // What the week must contain to be worth the client's time: something to
    // press with the legs, something to press overhead or in front, and
    // something to pull (Iversen 2021). Only asked for where the client's kit
    // can actually supply it, and not of someone who came for mobility: their
    // answer should not be overruled by a rejected plan.
    fun requiredPatterns(
        shortlist: List<CatalogExercise>,
        goal: FitnessGoal = FitnessGoal.GENERAL_FITNESS
    ): Set<PatternRequirement> {
        if (goal == FitnessGoal.FLEXIBILITY) return emptySet()
        return buildSet {
            if (shortlist.any { it.pattern.isLowerPush }) add(PatternRequirement.LOWER_PUSH)
            if (shortlist.any { it.pattern.isPush }) add(PatternRequirement.UPPER_PUSH)
            if (shortlist.any { it.pattern.isPull }) add(PatternRequirement.UPPER_PULL)
        }
    }
}

enum class PatternRequirement(val label: String) {
    LOWER_PUSH("a squat or lunge"),
    UPPER_PUSH("an upper-body press"),
    UPPER_PULL("an upper-body pull");

    fun isMetBy(pattern: MovementPattern): Boolean = when (this) {
        LOWER_PUSH -> pattern.isLowerPush
        UPPER_PUSH -> pattern.isPush
        UPPER_PULL -> pattern.isPull
    }
}
