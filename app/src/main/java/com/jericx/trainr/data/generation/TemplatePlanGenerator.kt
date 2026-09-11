package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.generation.SkeletonSlot
import kotlin.math.absoluteValue

// A whole week from the catalog: the skeleton, a movement for every slot, and
// the engine's numbers.
class TemplatePlanGenerator(private val catalog: ExerciseCatalog) : PlanGenerator {

    private val builder = PlanSkeletonBuilder(catalog)
    private val assembler = PlanAssembler(catalog)

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        if (catalog.all.isEmpty()) return PlanGenerationResult.Failed
        val skeleton = builder.build(request)
        if (!skeleton.isComplete) return PlanGenerationResult.Failed
        return assembler.assemble(skeleton, choose(skeleton, request), request)
            ?.let { PlanGenerationResult.Generated(it) }
            ?: PlanGenerationResult.Failed
    }

    // Two clients who answered the same way should not train the same week for
    // ever. Each slot takes one of its best few rather than always its first,
    // chosen from the client's own answers: the same person rebuilding the same
    // week gets the same movements, and the next person does not. Asking for a
    // fresh cast moves everyone along by a week.
    private fun choose(skeleton: PlanSkeleton, request: PlanRequest): PlanSelection {
        val client = seedOf(request)
        return PlanSelection(
            skeleton.days.associate { day ->
                val taken = mutableSetOf<String>()
                // A day that trains the same muscle the same way twice has spent
                // a slot on nothing, so variety never buys itself a repeat.
                val trained = day.slots.filter { it.isDecided }
                    .mapNotNull { shapeOf(it.candidates.single()) }.toMutableSet()
                day.id to DaySelection(
                    slots = day.openSlots.associate { slot ->
                        val order = rotated(slot, client, day.dayNumber, request.freshCast)
                            .filter { it !in taken }
                        val pick = order.firstOrNull { shapeOf(it) !in trained }
                            ?: order.firstOrNull()
                            ?: slot.candidates.first()
                        taken += pick
                        shapeOf(pick)?.let { trained += it }
                        slot.id to pick
                    },
                    title = day.fallbackTitle
                )
            }
        )
    }

    // What a movement trains and how, which is what makes two of them a repeat.
    private fun shapeOf(key: String): Pair<MovementPattern, MuscleGroup>? =
        catalog[key]?.let { it.pattern to it.primary }

    private fun rotated(
        slot: SkeletonSlot,
        client: Int,
        dayNumber: Int,
        freshCast: Boolean
    ): List<String> {
        // A fresh cast is a paid request for a different week, so it moves the
        // window itself rather than only the order inside it: where a slot's
        // best two train the same muscle the same way, reordering them alone
        // would leave the day unchanged.
        val pool = if (freshCast && slot.candidates.size > 1) {
            slot.candidates.drop(1) + slot.candidates.take(1)
        } else {
            slot.candidates
        }
        val best = pool.take(VARIETY_DEPTH)
        if (best.size < 2) return pool
        // Hashed together rather than xored: xor leaves the choice riding on the
        // seed's lowest bits, so with two candidates every slot in the week
        // turned on one bit and there were only ever two weeks to go round.
        val offset = hash("$client:${slot.id}:$dayNumber").absoluteValue % best.size
        return best.drop(offset) + best.take(offset) + pool.drop(VARIETY_DEPTH)
    }

    // Who the client is, not how long they have. Session length and day count
    // reshape the week on their own; letting them move the seed as well would
    // mean answering "90 minutes" reshuffled every movement, and a longer
    // answer could then buy a shorter session.
    private fun seedOf(request: PlanRequest): Int {
        val user = request.user
        val answers = listOf(
            user.id, user.age, user.weight, user.gender, user.fitnessGoal, user.experienceLevel,
            user.availableEquipment.sortedBy { it.name }, user.injuries.sortedBy { it.name }
        ).joinToString("|")
        // Regenerating is a request for a different cast. Folded into the text
        // rather than xored onto the result: a low bit has to move, and the
        // rotation only ever reads the low bits.
        return hash(if (request.freshCast) "$answers|again:${request.weekNumber}" else answers)
    }

    // Written out rather than String.hashCode() so both platforms rotate
    // identically: Swift's hashing is seeded per process and would not agree.
    private fun hash(text: String): Int {
        var h = FNV_OFFSET
        text.forEach { h = (h xor it.code) * FNV_PRIME }
        return h
    }

    private companion object {
        // Two candidates, not three: measured against the volume and frequency
        // the evidence asks for, going deeper spread a week's sets thinner
        // without buying any more variety worth having.
        const val VARIETY_DEPTH = 2
        const val FNV_OFFSET = -2128831035
        const val FNV_PRIME = 16777619
    }
}
