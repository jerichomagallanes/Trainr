package com.jericx.trainr.presentation.unstuck.feedback

import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.ReasonCode
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.domain.unstuck.TradeoffCode

internal const val ADJUSTMENT_ID = 5L

internal fun reduceProposal() = proposal(
    ProposalChange(
        kind = ChangeKind.REDUCE_UNPERFORMED,
        before = ExerciseSnapshot(
            exerciseInstanceId = "exercise:2",
            catalogKey = "dumbbell_bicep_curl",
            sets = listOf(SetSnapshot("set:201", 10, null, null, null))
        ),
        after = ExerciseSnapshot(
            exerciseInstanceId = "exercise:2",
            catalogKey = "dumbbell_bicep_curl",
            sets = emptyList()
        )
    ),
    reasonCode = ReasonCode.TIME_CONSTRAINT
)

internal fun omitProposal() = proposal(
    ProposalChange(
        kind = ChangeKind.OMIT_UNPERFORMED,
        before = ExerciseSnapshot(
            exerciseInstanceId = "exercise:4",
            catalogKey = "dumbbell_bicep_curl",
            sets = listOf(SetSnapshot("set:401", 10, null, null, null))
        ),
        after = null
    ),
    reasonCode = ReasonCode.TIME_CONSTRAINT
)

internal fun replaceProposal() = proposal(
    ProposalChange(
        kind = ChangeKind.REPLACE_UNPERFORMED,
        before = ExerciseSnapshot(
            exerciseInstanceId = "exercise:3",
            catalogKey = "goblet_squat",
            sets = listOf(SetSnapshot("set:301", 12, 20f, null, null))
        ),
        after = ExerciseSnapshot(
            exerciseInstanceId = "new",
            catalogKey = "dumbbell_step_up",
            sets = listOf(SetSnapshot("new:1", 12, null, null, null))
        )
    ),
    reasonCode = ReasonCode.EQUIPMENT_CONSTRAINT
)

internal fun appliedAdjustment(
    dayId: Long,
    proposal: AdjustmentProposal = reduceProposal()
) = AppliedAdjustment(
    id = ADJUSTMENT_ID,
    workoutDayId = dayId,
    proposal = proposal,
    reason = AdjustmentReason.LESS_TIME,
    appliedAt = 1L
)

private fun proposal(change: ProposalChange, reasonCode: ReasonCode) = AdjustmentProposal(
    proposalId = "proposal-1",
    requestId = "request-1",
    sessionId = "day:7",
    baseRevision = "revision-1",
    policyVersion = "test",
    changes = listOf(change),
    preservedPerformedSetIds = emptyList(),
    reasonCode = reasonCode,
    tradeoffCode = TradeoffCode.REDUCED_SESSION.wire,
    factReferences = emptyList()
)
