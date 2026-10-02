package com.jericx.trainr.domain.unstuck

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.generation.SlotTier
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise

// A stored day has exercises, not the slots it was built from. This reads the
// slots back so the shape's own drop order can be applied to a real session.
object SessionTiers {

    fun assign(
        day: WorkoutDay,
        catalog: ExerciseCatalog,
        priority: GoalPriority? = null
    ): Map<Long, SlotTier> {
        val ordered = day.exercises.sortedWith(compareBy({ it.sortOrder }, { it.id }))
        val compounds = mutableListOf<WorkoutExercise>()
        val tiers = mutableMapOf<Long, SlotTier>()
        ordered.forEach { exercise ->
            val entry = catalog[exercise.exerciseKey]
            tiers[exercise.id] = when {
                exercise.exerciseKey == WARM_UP_KEY -> SlotTier.WARM_UP
                entry == null -> SlotTier.ACCESSORY
                entry.pattern == MovementPattern.MOBILITY -> SlotTier.MOBILITY
                entry.role == ExerciseRole.TIMED && entry.pattern == MovementPattern.CONDITIONING ->
                    SlotTier.CONDITIONING

                entry.pattern == MovementPattern.CORE -> SlotTier.CORE
                entry.role == ExerciseRole.ISOLATION -> SlotTier.ISOLATION
                else -> SlotTier.ACCESSORY.also { compounds += exercise }
            }
        }
        compounds.forEachIndexed { index, exercise ->
            tiers[exercise.id] = when (index) {
                0 -> SlotTier.PRIMARY_COMPOUND
                1 -> SlotTier.SECONDARY_COMPOUND
                else -> SlotTier.ACCESSORY
            }
        }
        val confirmed = priority?.catalogKey?.let { key -> ordered.firstOrNull { it.exerciseKey == key } }
        if (confirmed != null) {
            compounds.firstOrNull()?.takeIf { it.id != confirmed.id }
                ?.let { tiers[it.id] = SlotTier.SECONDARY_COMPOUND }
            tiers[confirmed.id] = SlotTier.PRIMARY_COMPOUND
        }
        return tiers
    }

    private const val WARM_UP_KEY = "warm_up"
}
