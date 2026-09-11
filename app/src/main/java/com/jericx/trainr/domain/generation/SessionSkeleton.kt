package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.catalog.PatternRequirement
import com.jericx.trainr.domain.model.UnitSystem

// Session order. Whatever is trained first gains most (Nunes 2021), so the
// tier a slot sits at is a fatigue rule, not a presentation choice.
enum class SlotTier {
    WARM_UP, PRIMARY_COMPOUND, SECONDARY_COMPOUND, ACCESSORY,
    ISOLATION, CORE, CONDITIONING, MOBILITY;

    val isCompound: Boolean
        get() = this == PRIMARY_COMPOUND || this == SECONDARY_COMPOUND || this == ACCESSORY

    val isTimed: Boolean get() = this == WARM_UP || this == CONDITIONING || this == MOBILITY
}

enum class SessionFocus(val title: String, val isHard: Boolean = true) {
    FULL_BODY("Full Body"),
    UPPER("Upper Body"),
    LOWER("Lower Body"),
    PUSH("Push"),
    PULL("Pull"),
    LEGS("Legs"),
    ACTIVE_RECOVERY("Active Recovery", isHard = false),
    MOBILITY_FLOW("Mobility Flow", isHard = false)
}

data class SkeletonSlot(
    // Wire id, unique within its day: "primary", "isolation_2".
    val id: String,
    val label: String,
    val tier: SlotTier,
    val patterns: List<MovementPattern>,
    val muscles: Set<MuscleGroup>,
    // Ranked, disjoint within the day, never empty.
    val candidates: List<String>,
    // The skeleton owns the set count and the rest. The engine may return
    // fewer sets, never more, and never touches rest.
    val sets: Int,
    val restSeconds: Int,
    // For timed work, the seconds a set was budgeted at. The day's length was
    // worked out from this, so nothing downstream may prescribe more.
    val secondsPerSet: Int? = null,
    // The weekly pattern this slot was dealt, if any. Never dropped.
    val required: PatternRequirement? = null
) {
    val isDecided: Boolean get() = candidates.size == 1
}

data class SkeletonDay(
    val dayNumber: Int,
    val focus: SessionFocus,
    val slots: List<SkeletonSlot>
) {
    val id: String get() = "day$dayNumber"
    val fallbackTitle: String get() = focus.title
    val setCount: Int get() = slots.sumOf { it.sets }
    val openSlots: List<SkeletonSlot> get() = slots.filterNot { it.isDecided }
}

data class PlanSkeleton(
    val title: String,
    val days: List<SkeletonDay>,
    val units: UnitSystem,
    val maxSetsPerSession: Int,
    val sessionCeilingMinutes: Int,
    // What the week buys per region, counted the way SessionBudget counts:
    // one for the muscle trained, half for each assisted.
    val weeklySetsByRegion: Map<MuscleRegion, Float>,
    // Named so the caller stops asking for what the week cannot hold.
    val uncoveredPatterns: Set<PatternRequirement>
) {
    // A day with nothing in it is one the catalog could not fill, and no
    // amount of choosing makes a week out of it.
    val isComplete: Boolean get() = days.isNotEmpty() && days.none { it.slots.isEmpty() }

    val allowedKeys: Set<String> get() = days.flatMap { day -> day.slots.flatMap { it.candidates } }.toSet()

    // The patterns the week was actually dealt, which is what a finished plan
    // is held to.
    val requiredPatterns: Set<PatternRequirement>
        get() = days.flatMap { day -> day.slots.mapNotNull { it.required } }.toSet()
}
