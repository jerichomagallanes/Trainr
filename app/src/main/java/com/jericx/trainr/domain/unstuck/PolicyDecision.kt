package com.jericx.trainr.domain.unstuck

import com.jericx.trainr.domain.catalog.MuscleRegion

sealed interface PolicyDecision {

    data class Proposed(val proposal: AdjustmentProposal, val summary: ProposalSummary) : PolicyDecision

    data class NoChange(val reason: NoChangeReason, val estimateMinutes: Int?) : PolicyDecision

    data class NoFeasibleChange(val reason: InfeasibleReason, val minimumMinutes: Int?) : PolicyDecision
}

enum class NoChangeReason { ALREADY_FITS, NOTHING_UNPERFORMED }

enum class InfeasibleReason {
    TOO_SHORT_FOR_REQUIRED_WORK, NO_ELIGIBLE_SUBSTITUTE, UNKNOWN_EXERCISE, INVALID_MINUTES
}

enum class ProposalKind { SHORTER_SESSION, SUBSTITUTE }

enum class TradeoffCode {
    LESS_WORK_FOR_REGIONS, REDUCED_SESSION, DIFFERENT_RESISTANCE, LESS_BARBELL_PRACTICE,
    SEPARATE_LOAD_HISTORY;

    val wire: String get() = name.lowercase()
}

data class Tradeoff(
    val code: TradeoffCode,
    val regions: List<MuscleRegion> = emptyList(),
    val exerciseKeys: List<String> = emptyList()
)

sealed interface ChangeRow {

    data class Reduced(
        val exerciseKey: String,
        val name: String,
        val fromSets: Int,
        val toSets: Int
    ) : ChangeRow

    data class Omitted(val exerciseKey: String, val name: String, val sets: Int) : ChangeRow

    data class Replaced(
        val fromKey: String,
        val fromName: String,
        val toKey: String,
        val toName: String,
        val sets: Int
    ) : ChangeRow
}

data class ProposalSummary(
    val kind: ProposalKind,
    val keptPriorityKey: String?,
    val tradeoffs: List<Tradeoff>,
    val rows: List<ChangeRow>,
    val estimateBeforeMinutes: Int?,
    val estimateAfterMinutes: Int?,
    val budgetMinutes: Int?
)
