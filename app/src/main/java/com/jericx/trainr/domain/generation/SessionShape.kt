package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.model.FitnessGoal

data class SessionShape(
    val slotCount: Int,
    val sets: Map<SlotTier, Pair<Int, Int>>,
    val dropOrder: List<String>,
    // Weight loss and endurance take the rest of the session as
    // conditioning; every other goal takes a short fixed block.
    val conditioningFillsTheSession: Boolean = false
) {

    // What each goal gives up first, which is why the orders differ.
    companion object {
        fun forGoal(goal: FitnessGoal): SessionShape = when (goal) {
            FitnessGoal.STRENGTH -> SessionShape(
                slotCount = 6,
                sets = mapOf(
                    SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (3 to 5),
                    SlotTier.SECONDARY_COMPOUND to (2 to 4), SlotTier.ACCESSORY to (2 to 3),
                    SlotTier.ISOLATION to (2 to 2), SlotTier.CORE to (1 to 2), SlotTier.CONDITIONING to (1 to 1)
                ),
                dropOrder = listOf("isolation_2", "conditioning", "mobility_1", "accessory", "core", "isolation_1"),
            )
            FitnessGoal.MUSCLE_GAIN -> SessionShape(
                slotCount = 8,
                sets = mapOf(
                    SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (2 to 4),
                    SlotTier.SECONDARY_COMPOUND to (2 to 3), SlotTier.ACCESSORY to (2 to 3),
                    SlotTier.ISOLATION to (2 to 3), SlotTier.CORE to (1 to 3),
                    SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (1 to 1)
                ),
                dropOrder = listOf("mobility_1", "conditioning", "isolation_2", "core", "accessory", "isolation_1"),
            )
            FitnessGoal.GENERAL_FITNESS -> SessionShape(
                slotCount = 8,
                sets = mapOf(
                    SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (2 to 3),
                    SlotTier.SECONDARY_COMPOUND to (2 to 3), SlotTier.ACCESSORY to (2 to 3),
                    SlotTier.ISOLATION to (2 to 3), SlotTier.CORE to (1 to 3),
                    SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (1 to 1)
                ),
                dropOrder = listOf("mobility_1", "isolation_2", "isolation_1", "core", "conditioning", "accessory"),
            )
            FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> SessionShape(
                slotCount = 7,
                sets = mapOf(
                    SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (2 to 3),
                    SlotTier.SECONDARY_COMPOUND to (2 to 3), SlotTier.ACCESSORY to (2 to 3),
                    SlotTier.ISOLATION to (2 to 2), SlotTier.CORE to (2 to 3),
                    SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (1 to 1)
                ),
                dropOrder = listOf("isolation_2", "isolation_1", "accessory", "mobility_1", "secondary", "core"),
                conditioningFillsTheSession = true
            )
            FitnessGoal.FLEXIBILITY -> SessionShape(
                slotCount = 6,
                sets = mapOf(
                    SlotTier.WARM_UP to (1 to 1), SlotTier.CORE to (1 to 2),
                    SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (3 to 4)
                ),
                dropOrder = listOf("core", "conditioning", "mobility_4", "mobility_3"),
            )
        }
    }
}

// The slot ids the drop order is written in, as the tiers a built day can be
// read in: a day has exercises, not slots.
fun SessionShape.dropTiers(): List<SlotTier> =
    dropOrder.mapNotNull { SlotIdTiers[it.replace(InstanceSuffix, "")] }

private val SlotIdTiers = mapOf(
    "warm_up" to SlotTier.WARM_UP,
    "primary" to SlotTier.PRIMARY_COMPOUND,
    "secondary" to SlotTier.SECONDARY_COMPOUND,
    "accessory" to SlotTier.ACCESSORY,
    "isolation" to SlotTier.ISOLATION,
    "core" to SlotTier.CORE,
    "conditioning" to SlotTier.CONDITIONING,
    "mobility" to SlotTier.MOBILITY
)

private val InstanceSuffix = Regex("_\\d+$")
