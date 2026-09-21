package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.WorkoutDay
import org.junit.Test

class UnstuckPolicyEquipmentTest {

    private val policy = UnstuckPolicy(testCatalog)

    private fun benchDay(weightKg: Float? = 60f) = testDay(
        planned("warm_up", sets = 1, id = 1),
        planned("barbell_bench_press", sets = 3, id = 2, weightKg = weightKg),
        planned("bicycle_crunch", sets = 3, id = 3, reps = 12)
    )

    private fun decide(
        day: WorkoutDay,
        exerciseId: Long,
        available: Set<Equipment>,
        injuries: List<Injury> = emptyList(),
        requestId: String = "request-1"
    ) = policy.decide(
        AdjustmentSnapshot(day, testUser(injuries = injuries)),
        AdjustmentConstraint.EquipmentUnavailable(exerciseId, available),
        requestId
    )

    @Test
    fun anEquipmentSwapKeepsTheSetCount() {
        val day = benchDay()

        val decision = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val change = decision.proposal.changes.single()

        assertThat(change.kind).isEqualTo(ChangeKind.REPLACE_UNPERFORMED)
        assertThat(change.after!!.sets).hasSize(change.before.sets.size)
        assertThat(change.after!!.sets.map { it.setId })
            .containsExactly("new:1", "new:2", "new:3").inOrder()
        assertThat(change.after!!.sets.map { it.targetReps }.toSet()).containsExactly(8)
        assertThat(decision.proposal.reasonCode).isEqualTo(ReasonCode.EQUIPMENT_CONSTRAINT)
        assertWellFormed(decision.proposal, testCatalog)
    }

    @Test
    fun aSubstituteNeverInheritsTheOriginalWeight() {
        val day = benchDay(weightKg = 60f)

        val decision = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val after = decision.proposal.changes.single().after!!

        assertThat(after.catalogKey).isEqualTo("dumbbell_bench_press")
        after.sets.forEach {
            assertThat(it.targetWeightKg).isNotNull()
            assertThat(it.targetWeightKg).isNotEqualTo(60f)
        }
    }

    @Test
    fun aBarbellToDumbbellSwapNamesTheBarbellTradeoff() {
        val decision = decide(benchDay(), 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed

        assertThat(decision.proposal.tradeoffCode).isEqualTo("less_barbell_practice")
        assertThat(decision.summary.tradeoffs.map { it.code }).containsExactly(
            TradeoffCode.LESS_BARBELL_PRACTICE, TradeoffCode.SEPARATE_LOAD_HISTORY
        ).inOrder()
        assertThat(decision.summary.kind).isEqualTo(ProposalKind.SUBSTITUTE)
        assertThat(decision.summary.rows).containsExactly(
            ChangeRow.Replaced(
                fromKey = "barbell_bench_press",
                fromName = checkNotNull(testCatalog["barbell_bench_press"]).name,
                toKey = "dumbbell_bench_press",
                toName = checkNotNull(testCatalog["dumbbell_bench_press"]).name,
                sets = 3
            )
        )
    }

    @Test
    fun aSubstituteRespectsInjuriesAndAvailableEquipment() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_overhead_press", sets = 3, id = 2)
        )

        val free = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val guarded = decide(
            day,
            2L,
            setOf(Equipment.DUMBBELL),
            injuries = listOf(Injury.SHOULDER)
        ) as PolicyDecision.Proposed

        val chosen = checkNotNull(testCatalog[guarded.proposal.changes.single().after!!.catalogKey])

        assertThat(free.proposal.changes.single().after!!.catalogKey)
            .isEqualTo("dumbbell_shoulder_press")
        assertThat(chosen.key).isNotEqualTo("dumbbell_shoulder_press")
        assertThat(chosen.pattern).isNotEqualTo(MovementPattern.VERTICAL_PUSH)
        assertThat(InjuryGuard.excludes(chosen, listOf(Injury.SHOULDER))).isFalse()
        assertThat(chosen.isAvailableWith(setOf(Equipment.DUMBBELL))).isTrue()
    }

    @Test
    fun noCandidateIsReportedNotInvented() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_bicep_curl", sets = 3, id = 2)
        )

        val decision = decide(day, 2L, emptySet())

        assertThat(decision)
            .isEqualTo(PolicyDecision.NoFeasibleChange(InfeasibleReason.NO_ELIGIBLE_SUBSTITUTE, null))
    }

    @Test
    fun anExerciseTheDayDoesNotHoldIsRefused() {
        val decision = decide(benchDay(), 99L, setOf(Equipment.DUMBBELL))

        assertThat(decision)
            .isEqualTo(PolicyDecision.NoFeasibleChange(InfeasibleReason.UNKNOWN_EXERCISE, null))
    }

    @Test
    fun anExerciseAlreadyFinishedNeedsNoAlternative() {
        val day = testDay(
            planned("barbell_bench_press", sets = 3, id = 2, performed = 3)
        )

        val decision = decide(day, 2L, setOf(Equipment.DUMBBELL))

        assertThat(decision)
            .isEqualTo(PolicyDecision.NoChange(NoChangeReason.NOTHING_UNPERFORMED, null))
    }

    @Test
    fun onlyTheAffectedExerciseChangesAndPerformedSetsAreListed() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_bench_press", sets = 3, id = 2, performed = 1),
            planned("bicycle_crunch", sets = 3, id = 3, reps = 12)
        )

        val decision = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val change = decision.proposal.changes.single()

        assertThat(change.before.sets.map { it.setId }).containsExactly("set:202", "set:203").inOrder()
        assertThat(change.after!!.sets).hasSize(2)
        assertThat(decision.proposal.preservedPerformedSetIds).containsExactly("set:201")
        assertThat(decision.proposal.factReferences).containsAtLeast(
            "exercise:2",
            "available:dumbbell",
            "candidate:dumbbell_bench_press",
            "goal:muscle_gain"
        )
    }

    @Test
    fun theSameRequestOnTheSameStateGivesTheSameProposalId() {
        val day = benchDay()

        val first = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val again = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val other = decide(
            day,
            2L,
            setOf(Equipment.DUMBBELL),
            requestId = "request-2"
        ) as PolicyDecision.Proposed

        assertThat(again.proposal.proposalId).isEqualTo(first.proposal.proposalId)
        assertThat(other.proposal.proposalId).isNotEqualTo(first.proposal.proposalId)
    }

    @Test
    fun theOrderEquipmentWasTickedDoesNotChangeTheProposalId() {
        val day = benchDay()

        val ticked = decide(day, 2L, setOf(Equipment.DUMBBELL, Equipment.MACHINE))
            as PolicyDecision.Proposed
        val tickedTheOtherWay = decide(day, 2L, setOf(Equipment.MACHINE, Equipment.DUMBBELL))
            as PolicyDecision.Proposed

        assertThat(tickedTheOtherWay.proposal.proposalId).isEqualTo(ticked.proposal.proposalId)
    }

    @Test
    fun theExerciseBeingReplacedIsNeverReportedAsKept() {
        val day = benchDay()

        val decision = decide(day, 2L, setOf(Equipment.DUMBBELL)) as PolicyDecision.Proposed
        val withPriorityOnTheTarget = policy.decide(
            AdjustmentSnapshot(day, testUser(), GoalPriority("barbell_bench_press")),
            AdjustmentConstraint.EquipmentUnavailable(2L, setOf(Equipment.DUMBBELL)),
            "request-1"
        ) as PolicyDecision.Proposed
        val withPriorityElsewhere = policy.decide(
            AdjustmentSnapshot(day, testUser(), GoalPriority("bicycle_crunch")),
            AdjustmentConstraint.EquipmentUnavailable(2L, setOf(Equipment.DUMBBELL)),
            "request-1"
        ) as PolicyDecision.Proposed

        assertThat(decision.summary.keptPriorityKey).isNull()
        assertThat(withPriorityOnTheTarget.summary.keptPriorityKey).isNull()
        assertThat(withPriorityElsewhere.summary.keptPriorityKey).isEqualTo("bicycle_crunch")
    }
}
