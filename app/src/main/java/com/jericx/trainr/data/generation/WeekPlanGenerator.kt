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
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import kotlin.math.roundToInt

// A whole week from the catalog: the skeleton, a movement for every slot, and
// the engine's numbers. Next week is last week's movements, progressed from
// what was lifted, for as long as every one of them still has a place; a
// profile edit that rules one out, a split with no room for it, or a client
// asking for new movements picks the week afresh.
class WeekPlanGenerator(private val catalog: ExerciseCatalog) : PlanGenerator {

    private val builder = PlanSkeletonBuilder(catalog)
    private val expander = PlanExpander(catalog)
    private val parser = GeneratedPlanParser(catalog)

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        if (catalog.all.isEmpty()) return PlanGenerationResult.Failed
        val skeleton = builder.build(request)
        if (!skeleton.isComplete) return PlanGenerationResult.Failed
        val previous = request.previousWeek?.takeUnless { request.freshCast }
        val carried = previous?.let { carry(it, skeleton) }?.let { assemble(skeleton, it, request) }
        return (carried ?: assemble(skeleton, choose(skeleton, request), request))
            ?.let { PlanGenerationResult.Generated(it) }
            ?: PlanGenerationResult.Failed
    }

    private fun assemble(skeleton: PlanSkeleton, selection: PlanSelection, request: PlanRequest): WeeklyWorkoutPlan? {
        val parsed = parser.parse(
            expander.expand(skeleton, selection, request),
            request.user.id,
            request.weekNumber,
            request.startDateMillis,
            PlanLimits(
                maxSetsPerSession = skeleton.maxSetsPerSession,
                allowedKeys = skeleton.allowedKeys,
                requiredPatterns = skeleton.requiredPatterns,
                sessionMinutes = request.user.workoutDuration,
                sessionCeilingMinutes = skeleton.sessionCeilingMinutes
            )
        )
        return (parsed as? PlanParseResult.Parsed)?.plan
    }

    // Each of last week's movements back in a slot that offers it, on the same
    // day, under the same title. Null when one is left over or a slot is left
    // empty, because the week would then not be last week's.
    private fun carry(previous: WeeklyWorkoutPlan, skeleton: PlanSkeleton): PlanSelection? {
        if (previous.workoutDays.size != skeleton.days.size) return null
        val days = skeleton.days.associate { day ->
            val before = previous.workoutDays.firstOrNull { it.dayNumber == day.dayNumber } ?: return null
            val remaining = before.exercises.map { it.exerciseKey }.toMutableList()
            day.slots.filter { it.isDecided }.forEach { remaining.remove(it.candidates.single()) }
            val slots = day.openSlots.associate { slot ->
                val key = remaining.firstOrNull { it in slot.candidates } ?: return null
                remaining.remove(key)
                slot.id to key
            }
            if (remaining.isNotEmpty()) return null
            day.id to DaySelection(slots, before.title)
        }
        return PlanSelection(days)
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
        val offset = (mixed(hash("$client:${slot.id}:$dayNumber")) % best.size.toUInt()).toInt()
        return best.drop(offset) + best.take(offset) + pool.drop(VARIETY_DEPTH)
    }

    // Who the client is, not how long they have. Session length and day count
    // reshape the week on their own; letting them move the seed as well would
    // mean answering "90 minutes" reshuffled every movement, and a longer
    // answer could then buy a shorter session.
    private fun seedOf(request: PlanRequest): Int {
        val user = request.user
        val answers = buildString {
            append(user.id).append(';').append(user.age).append(';')
            append((user.weight * 10).roundToInt()).append(';')
            append(user.gender.name).append(';').append(user.fitnessGoal.name).append(';')
            append(user.experienceLevel.name).append(';')
            append(user.availableEquipment.map { it.name }.sorted().joinToString(",")).append(';')
            append(user.injuries.map { it.name }.sorted().joinToString(","))
            // Regenerating is a request for a different cast.
            if (request.freshCast) append(";again:").append(request.weekNumber)
        }
        return hash(answers)
    }

    // Written out so Swift can run the same algorithm: its own string hashing
    // is seeded per process and would give a different week on every launch.
    // FNV-1a's low bits are a parity of the input's low bits, so a modulo read
    // straight off them turned every slot in the week on one bit and forty
    // clients shared two weeks. Murmur's finalizer spreads the high bits down.
    private fun mixed(h: Int): UInt {
        var u = h.toUInt()
        u = u xor (u shr 16); u *= 0x85ebca6bu
        u = u xor (u shr 13); u *= 0xc2b2ae35u
        return u xor (u shr 16)
    }

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
