package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.generation.DeloadCheck
import com.jericx.trainr.domain.generation.ExerciseHistory
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.ProgressionEngine
import com.jericx.trainr.domain.generation.ProgressionNote
import com.jericx.trainr.domain.generation.ProgressionRequest
import com.jericx.trainr.domain.generation.ProgressionTarget
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot

// A skeleton plus whatever was chosen for it becomes a plan: the catalog says
// what each movement is, the engine says how much, and the skeleton says how
// many sets and how long between them.
class PlanExpander(private val catalog: ExerciseCatalog) {

    fun expand(skeleton: PlanSkeleton, selection: PlanSelection, request: PlanRequest): GeneratedPlan {
        val deload = DeloadCheck.isDue(request.user, request.history)
        return GeneratedPlan(
            title = skeleton.title,
            days = skeleton.days.map { day -> expand(day, selection.days[day.id], request, deload) }
        )
    }

    private fun expand(
        day: SkeletonDay,
        chosen: DaySelection?,
        request: PlanRequest,
        deload: Boolean
    ): GeneratedDay {
        val taken = mutableSetOf<String>()
        val exercises = day.slots.mapNotNull { slot ->
            // A key the slot never offered, or one already used today, falls
            // back to the slot's own list.
            val pick = chosen?.slots?.get(slot.id)?.takeIf { it in slot.candidates && it !in taken }
            val order = listOfNotNull(pick) + slot.candidates.filter { it != pick && it !in taken }
            fill(slot, order, day.dayNumber, request, deload)?.also { taken += it.exerciseKey }
        }
        return GeneratedDay(
            dayNumber = day.dayNumber,
            title = chosen?.title?.trim()?.take(MAX_TITLE_CHARS)?.takeIf { it.isNotBlank() }
                ?: day.fallbackTitle,
            exercises = exercises
        )
    }

    // The engine is asked before the key is fixed, so a starting weight
    // lighter than an empty bar, or a movement the client has outgrown, is
    // answered with the next movement on the list rather than a lie.
    private fun fill(
        slot: SkeletonSlot,
        order: List<String>,
        dayNumber: Int,
        request: PlanRequest,
        deload: Boolean
    ): GeneratedExercise? {
        var fallback: Pair<CatalogExercise, ProgressionTarget>? = null
        order.take(MAX_ATTEMPTS).forEach { key ->
            val movement = catalog[key] ?: return@forEach
            val target = ProgressionEngine.next(
                ProgressionRequest(
                    user = request.user,
                    exercise = movement,
                    history = ExerciseHistory.from(request.history, key),
                    sets = slot.sets,
                    nowMillis = request.startDateMillis + (dayNumber - 1) * DAY_MILLIS,
                    deload = deload,
                    cautioned = InjuryGuard.cautionFor(movement, request.user.injuries) != null,
                    secondsBudget = slot.secondsPerSet
                )
            )
            if (fallback == null) fallback = movement to target
            if (target.notes.none { it in AsksForAnother }) return exercise(slot, movement, target)
        }
        return fallback?.let { (movement, target) -> exercise(slot, movement, target) }
    }

    private fun exercise(slot: SkeletonSlot, movement: CatalogExercise, target: ProgressionTarget) =
        GeneratedExercise(
            exerciseKey = movement.key,
            restSeconds = slot.restSeconds,
            sets = target.sets.map { set ->
                GeneratedSet(
                    reps = set.targetReps,
                    weightKg = set.targetWeightKg,
                    // The day's length was fitted to this budget; the engine
                    // may ask for less, never more.
                    seconds = set.targetSeconds?.let { seconds ->
                        slot.secondsPerSet?.let { minOf(seconds, it) } ?: seconds
                    }
                )
            }
        )

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MAX_TITLE_CHARS = 40
        const val DAY_MILLIS = 86_400_000L
        val AsksForAnother = setOf(
            ProgressionNote.LIGHTER_THAN_THE_BAR, ProgressionNote.NEEDS_HARDER_VARIATION
        )
    }
}
