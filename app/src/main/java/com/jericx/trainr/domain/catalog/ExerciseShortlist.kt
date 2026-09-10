package com.jericx.trainr.domain.catalog

import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.FitnessGoal
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

        // Conditioning and mobility are modalities, not muscle regions, and
        // how much of each the week needs is what the goal answers. Ranked
        // among the regions they lost every slot to the alphabet, so a client
        // asking for flexibility was offered one stretch and a squat rack.
        val quota = quotaFor(user.fitnessGoal)
        kept += available.pick(kept, quota.conditioning) { it.isConditioning }
        kept += available.pick(kept, quota.mobility) { it.isMobility }

        // One from each region in turn, staples before the rest, so a cap
        // never leaves a client with nine chest movements and no legs.
        val queues = available
            .filterNot { it in kept || it.isConditioning || it.isMobility }
            .sortedWith(byRank)
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

    private val byRank =
        compareByDescending<CatalogExercise> { it.staple }.thenBy { it.key }

    private val CatalogExercise.isConditioning: Boolean
        get() = primary == MuscleGroup.CARDIO

    private val CatalogExercise.isMobility: Boolean
        get() = pattern == MovementPattern.MOBILITY

    private fun List<CatalogExercise>.pick(
        already: Set<CatalogExercise>,
        limit: Int,
        matching: (CatalogExercise) -> Boolean
    ): List<CatalogExercise> = asSequence()
        .filter { it !in already && matching(it) }
        .sortedWith(byRank)
        .take(limit)
        .toList()

    private data class Quota(val conditioning: Int, val mobility: Int)

    // A weight-loss week is mostly conditioning and a flexibility week is
    // mostly not; both were being handed the vocabulary of a hypertrophy
    // block. Mobility asks for two even where it is not the point, because
    // every session needs a warm-up and the catalog keeps that here.
    private fun quotaFor(goal: FitnessGoal): Quota = when (goal) {
        FitnessGoal.FLEXIBILITY -> Quota(conditioning = 2, mobility = 5)
        FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> Quota(conditioning = 6, mobility = 2)
        FitnessGoal.GENERAL_FITNESS -> Quota(conditioning = 4, mobility = 2)
        FitnessGoal.MUSCLE_GAIN, FitnessGoal.STRENGTH -> Quota(conditioning = 3, mobility = 2)
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
