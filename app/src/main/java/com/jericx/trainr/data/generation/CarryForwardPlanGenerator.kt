package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan

// Next week is last week's movements, progressed from what was lifted: a lift
// only moves on while it stays in the programme. A profile
// edit that rules a movement out, a split with no room for it, or a client
// asking for new movements hands the week to the generator behind this one.
class CarryForwardPlanGenerator(
    catalog: ExerciseCatalog,
    private val next: PlanGenerator
) : PlanGenerator {

    private val builder = PlanSkeletonBuilder(catalog)
    private val assembler = PlanAssembler(catalog)

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        val previous = request.previousWeek
        val carried = if (request.freshCast || previous == null) null else carry(previous, request)
        return carried?.let { PlanGenerationResult.Generated(it) } ?: next.generate(request)
    }

    private fun carry(previous: WeeklyWorkoutPlan, request: PlanRequest): WeeklyWorkoutPlan? {
        val skeleton = builder.build(request)
        if (!skeleton.isComplete) return null
        val selection = selectionFrom(previous, skeleton) ?: return null
        return assembler.assemble(skeleton, selection, request)
    }

    // Each of last week's movements back in a slot that offers it, on the same
    // day, under the same title. Null when one is left over or a slot is left
    // empty, because the week would then not be last week's.
    private fun selectionFrom(previous: WeeklyWorkoutPlan, skeleton: PlanSkeleton): PlanSelection? {
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
}
