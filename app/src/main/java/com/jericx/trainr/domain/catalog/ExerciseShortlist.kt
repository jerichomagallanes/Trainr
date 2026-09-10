package com.jericx.trainr.domain.catalog

import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UserProfile

// The movements this client could actually be given this week, and nothing
// else. The whole catalog would be thousands of tokens of schema on every
// request and would still offer a barbell to someone who owns none; the
// shortlist is what makes the model's choice both cheap and possible.
object ExerciseShortlist {

    // Enough for a varied week without paying for a vocabulary no single week
    // could spend.
    private const val MAX_KEYS = 80

    fun forRequest(
        catalog: ExerciseCatalog,
        user: UserProfile,
        carriedOver: Set<String> = emptySet()
    ): List<CatalogExercise> {
        val owned = user.availableEquipment.toSet().ifEmpty { setOf(Equipment.NONE) }
        val available = catalog.availableWith(owned)
        val byKey = available.associateBy { it.key }

        // Last week's movements come first whatever else is dropped: a key the
        // model cannot name again is a lift whose history stops here.
        val kept = LinkedHashSet<CatalogExercise>()
        carriedOver.forEach { key -> byKey[key]?.let(kept::add) }

        // One from each region in turn, staples before the rest, so a cap
        // never leaves a client with nine chest movements and no legs.
        val queues = available
            .filterNot { it in kept }
            .sortedWith(compareByDescending<CatalogExercise> { it.staple }.thenBy { it.key })
            .groupBy { it.primary.region }
            .mapValues { (_, list) -> ArrayDeque(list) }

        val order = MuscleRegion.entries.filter { queues.containsKey(it) }
        var added = true
        while (kept.size < MAX_KEYS && added) {
            added = false
            for (region in order) {
                if (kept.size >= MAX_KEYS) break
                val next = queues[region]?.removeFirstOrNull() ?: continue
                kept.add(next)
                added = true
            }
        }

        return kept.toList().sortedBy { it.key }
    }

    // What the week must contain to be worth the client's time: something to
    // press with the legs, something to press overhead or in front, and
    // something to pull (Iversen 2021). Only asked for where the client's kit
    // can actually supply it.
    fun requiredPatterns(shortlist: List<CatalogExercise>): Set<PatternRequirement> =
        buildSet {
            if (shortlist.any { it.pattern.isLowerPush }) add(PatternRequirement.LOWER_PUSH)
            if (shortlist.any { it.pattern.isPush }) add(PatternRequirement.UPPER_PUSH)
            if (shortlist.any { it.pattern.isPull }) add(PatternRequirement.UPPER_PULL)
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
