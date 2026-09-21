package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.WorkoutDay
import org.junit.Test

class UnstuckPolicyTimeTest {

    private val policy = UnstuckPolicy(testCatalog)

    private fun fullDay() = testDay(
        planned("warm_up", sets = 1, id = 1),
        planned("barbell_bench_press", sets = 4, id = 2),
        planned("barbell_bent_over_row", sets = 3, id = 3),
        planned("dumbbell_bicep_curl", sets = 3, id = 4, reps = 10),
        planned("bicycle_crunch", sets = 3, id = 5, reps = 12)
    )

    private fun decide(
        day: WorkoutDay,
        minutes: Int,
        goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
        scope: TimeScope = TimeScope.WHOLE_SESSION,
        priority: GoalPriority? = null,
        requestId: String = "request-1"
    ) = policy.decide(
        AdjustmentSnapshot(day, testUser(goal = goal), priority),
        AdjustmentConstraint.LessTime(minutes, scope),
        requestId
    )

    private fun estimate(day: WorkoutDay, goal: FitnessGoal) =
        SessionEstimate.minutes(day, testUser(goal = goal), TimeScope.WHOLE_SESSION, testCatalog)

    @Test
    fun aSessionThatAlreadyFitsIsLeftAlone() {
        val day = fullDay()
        val before = estimate(day, FitnessGoal.MUSCLE_GAIN)

        val decision = decide(day, before + 5)

        assertThat(decision).isEqualTo(PolicyDecision.NoChange(NoChangeReason.ALREADY_FITS, before))
    }

    @Test
    fun aDayWithEverythingPerformedNeedsNoChange() {
        val day = testDay(
            planned("barbell_bench_press", sets = 3, id = 2, performed = 3),
            planned("dumbbell_bicep_curl", sets = 3, id = 4, performed = 3)
        )

        val decision = decide(day, 5)

        assertThat(decision).isEqualTo(PolicyDecision.NoChange(NoChangeReason.NOTHING_UNPERFORMED, null))
    }

    @Test
    fun strengthAndMuscleGainShedDifferentWorkForTheSameBudget() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_bench_press", sets = 4, id = 2),
            planned("barbell_squat", sets = 4, id = 3),
            planned("barbell_bent_over_row", sets = 4, id = 4),
            planned("bicycle_crunch", sets = 4, id = 5, reps = 12)
        )

        val strength = decide(day, estimate(day, FitnessGoal.STRENGTH) - 1, FitnessGoal.STRENGTH)
        val muscle = decide(day, estimate(day, FitnessGoal.MUSCLE_GAIN) - 1, FitnessGoal.MUSCLE_GAIN)

        assertThat(strength.reducedKeys()).containsExactly("barbell_bent_over_row")
        assertThat(muscle.reducedKeys()).containsExactly("bicycle_crunch")
    }

    @Test
    fun theWarmUpIsNeverReducedOrOmitted() {
        val day = fullDay()

        val decision = decide(day, MINIMUM_FEASIBLE_MINUTES) as PolicyDecision.Proposed

        assertThat(decision.proposal.changes.map { it.before.catalogKey }).doesNotContain("warm_up")
        assertThat(decision.summary.rows.map { it.key() }).doesNotContain("warm_up")
    }

    @Test
    fun thePrimaryCompoundIsNeverOmitted() {
        val day = fullDay()

        val decision = decide(day, MINIMUM_FEASIBLE_MINUTES) as PolicyDecision.Proposed
        val primary = decision.proposal.changes.single { it.before.catalogKey == "barbell_bench_press" }

        assertThat(primary.kind).isEqualTo(ChangeKind.REDUCE_UNPERFORMED)
        assertThat(primary.after!!.sets).isNotEmpty()
        assertThat(decision.summary.keptPriorityKey).isEqualTo("barbell_bench_press")
        assertWellFormed(decision.proposal, testCatalog)
    }

    @Test
    fun aConfirmedPriorityIsKeptWhole() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_bench_press", sets = 3, id = 2),
            planned("barbell_bent_over_row", sets = 3, id = 3),
            planned("dumbbell_bicep_curl", sets = 3, id = 4, reps = 10),
            planned("bicycle_crunch", sets = 3, id = 5, reps = 12)
        )

        val decision = decide(
            day,
            PRIORITY_BUDGET_MINUTES,
            priority = GoalPriority("barbell_bent_over_row")
        ) as PolicyDecision.Proposed

        assertThat(decision.summary.keptPriorityKey).isEqualTo("barbell_bent_over_row")
        assertThat(decision.proposal.changes.map { it.before.catalogKey })
            .doesNotContain("barbell_bent_over_row")
    }

    @Test
    fun setsComeOffTheEndAndPerformedSetsNeverMove() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_bench_press", sets = 3, id = 2),
            planned("dumbbell_bicep_curl", sets = 4, id = 3, performed = 2, reps = 10)
        )

        val one = decide(day, ONE_SET_OFF_MINUTES) as PolicyDecision.Proposed
        val change = one.proposal.changes.single()

        assertThat(change.before.sets.map { it.setId }).containsExactly("set:303", "set:304").inOrder()
        assertThat(change.after!!.sets.map { it.setId }).containsExactly("set:303")
        assertThat(one.proposal.preservedPerformedSetIds).containsExactly("set:301", "set:302")

        val both = decide(day, BOTH_SETS_OFF_MINUTES) as PolicyDecision.Proposed

        assertThat(both.proposal.changes.single().kind).isEqualTo(ChangeKind.REDUCE_UNPERFORMED)
        assertThat(both.proposal.changes.single().after!!.sets).isEmpty()
    }

    @Test
    fun restSecondsAreUntouched() {
        val day = testDay(
            planned("warm_up", sets = 1, id = 1),
            planned("barbell_bench_press", sets = 3, id = 2, rest = 150),
            planned("dumbbell_bicep_curl", sets = 4, id = 3, rest = 150, reps = 10)
        )

        val decision = decide(day, REST_BUDGET_MINUTES) as PolicyDecision.Proposed

        decision.proposal.changes.forEach { change ->
            assertThat(change.before.sets.map { it.restSeconds }.toSet()).containsExactly(150)
            change.after?.sets?.forEach { assertThat(it.restSeconds).isEqualTo(150) }
        }
    }

    @Test
    fun fiveMinutesIsReportedAsInfeasibleNotShrunk() {
        val day = fullDay()

        val decision = decide(day, 5) as PolicyDecision.NoFeasibleChange

        assertThat(decision.reason).isEqualTo(InfeasibleReason.TOO_SHORT_FOR_REQUIRED_WORK)
        assertThat(decision.minimumMinutes).isGreaterThan(5)
    }

    @Test
    fun fourMinutesIsOutsideTheSupportedRange() {
        val decision = decide(fullDay(), 4)

        assertThat(decision)
            .isEqualTo(PolicyDecision.NoFeasibleChange(InfeasibleReason.INVALID_MINUTES, null))
    }

    @Test
    fun theSameRequestOnTheSameStateGivesTheSameProposalId() {
        val day = fullDay()
        val budget = estimate(day, FitnessGoal.MUSCLE_GAIN) - 1

        val first = decide(day, budget) as PolicyDecision.Proposed
        val again = decide(day, budget) as PolicyDecision.Proposed
        val other = decide(day, budget, requestId = "request-2") as PolicyDecision.Proposed

        assertThat(again.proposal.proposalId).isEqualTo(first.proposal.proposalId)
        assertThat(other.proposal.proposalId).isNotEqualTo(first.proposal.proposalId)
        assertThat(first.proposal.baseRevision).isEqualTo(PlanRevision.of(day))
    }

    @Test
    fun aShorterSessionSaysWhatItCostAndWhatItWasMeasuredAgainst() {
        val day = fullDay()
        val before = estimate(day, FitnessGoal.MUSCLE_GAIN)

        val decision = decide(day, before - 1) as PolicyDecision.Proposed

        assertThat(decision.summary.kind).isEqualTo(ProposalKind.SHORTER_SESSION)
        assertThat(decision.summary.estimateBeforeMinutes).isEqualTo(before)
        assertThat(decision.summary.estimateAfterMinutes).isAtMost(before - 1)
        assertThat(decision.summary.budgetMinutes).isEqualTo(before - 1)
        assertThat(decision.proposal.reasonCode).isEqualTo(ReasonCode.TIME_CONSTRAINT)
        assertThat(decision.proposal.factReferences).containsAtLeast(
            "budget:${before - 1}:whole_session",
            "estimate_before:$before",
            "goal:muscle_gain",
            "revision:${PlanRevision.of(day)}"
        )
        assertWellFormed(decision.proposal, testCatalog)
    }

    private fun PolicyDecision.reducedKeys(): List<String> =
        (this as PolicyDecision.Proposed).proposal.changes.map { it.before.catalogKey }

    private fun ChangeRow.key(): String = when (this) {
        is ChangeRow.Reduced -> exerciseKey
        is ChangeRow.Omitted -> exerciseKey
        is ChangeRow.Replaced -> fromKey
    }

    private companion object {
        const val MINIMUM_FEASIBLE_MINUTES = 9
        const val PRIORITY_BUDGET_MINUTES = 12
        const val REST_BUDGET_MINUTES = 18
        const val ONE_SET_OFF_MINUTES = 18
        const val BOTH_SETS_OFF_MINUTES = 16
    }
}
