package com.jericx.trainr.presentation.unstuck

import androidx.annotation.StringRes
import com.jericx.trainr.R
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ChangeRow
import com.jericx.trainr.domain.unstuck.InfeasibleReason
import com.jericx.trainr.domain.unstuck.PolicyDecision
import com.jericx.trainr.domain.unstuck.ProposalKind
import com.jericx.trainr.domain.unstuck.ProposalSummary
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.domain.unstuck.Tradeoff
import com.jericx.trainr.domain.unstuck.TradeoffCode

sealed interface ReviewUi {

    data class Proposed(
        val kind: ProposalKind,
        val substituteEquipment: Equipment?,
        val priorityName: String?,
        @StringRes val goalLabelRes: Int,
        val budgetMinutes: Int?,
        val scope: TimeScope,
        val hasPerformedWork: Boolean,
        val keptNames: List<String>,
        val replacedFrom: String?,
        val replacedTo: String?,
        val tradeoffs: List<TradeoffUi>,
        val rows: List<ChangeRowUi>
    ) : ReviewUi

    data class NoChange(
        val priorityName: String?,
        @StringRes val goalLabelRes: Int,
        val plannedMinutes: Int
    ) : ReviewUi

    data class Infeasible(val reason: InfeasibleReason, val minimumMinutes: Int?) : ReviewUi
}

data class TradeoffUi(
    val code: TradeoffCode,
    val regions: List<MuscleRegion>,
    val exerciseName: String?
)

sealed interface ChangeRowUi {
    data class Reduced(val name: String, val fromSets: Int, val toSets: Int) : ChangeRowUi
    data class Omitted(val name: String) : ChangeRowUi
    data class Replaced(
        val fromName: String,
        val toName: String,
        val sets: Int,
        val reps: String
    ) : ChangeRowUi
}

@get:StringRes
val MuscleRegion.labelRes: Int
    get() = when (this) {
        MuscleRegion.CHEST -> R.string.region_chest
        MuscleRegion.BACK -> R.string.region_back
        MuscleRegion.SHOULDERS -> R.string.region_shoulders
        MuscleRegion.ARMS -> R.string.region_arms
        MuscleRegion.CORE -> R.string.region_core
        MuscleRegion.QUADS -> R.string.region_quads
        MuscleRegion.HAMSTRINGS -> R.string.region_hamstrings
        MuscleRegion.HIPS -> R.string.region_hips
        MuscleRegion.CALVES -> R.string.region_calves
        MuscleRegion.OTHER -> R.string.region_other
    }

fun PolicyDecision.toReviewUi(
    day: WorkoutDay,
    catalog: ExerciseCatalog,
    @StringRes goalLabelRes: Int,
    hasPerformedWork: Boolean,
    plannedMinutes: Int
): ReviewUi = when (this) {
    is PolicyDecision.Proposed -> proposed(
        summary, proposal, day, catalog, goalLabelRes, hasPerformedWork
    )

    is PolicyDecision.NoChange -> ReviewUi.NoChange(
        priorityName = null,
        goalLabelRes = goalLabelRes,
        plannedMinutes = estimateMinutes ?: plannedMinutes
    )

    is PolicyDecision.NoFeasibleChange -> ReviewUi.Infeasible(reason, minimumMinutes)
}

private fun proposed(
    summary: ProposalSummary,
    proposal: AdjustmentProposal,
    day: WorkoutDay,
    catalog: ExerciseCatalog,
    @StringRes goalLabelRes: Int,
    hasPerformedWork: Boolean
): ReviewUi.Proposed {
    val replaced = summary.rows.filterIsInstance<ChangeRow.Replaced>().firstOrNull()
    val touched = summary.rows.map { it.key }.toSet()

    return ReviewUi.Proposed(
        kind = summary.kind,
        substituteEquipment = replaced?.let { catalog[it.toKey]?.equipment },
        // A catalog key is not a name, and no screen may print the slug.
        priorityName = summary.keptPriorityKey?.let { catalog[it]?.name },
        goalLabelRes = goalLabelRes,
        budgetMinutes = summary.budgetMinutes,
        scope = if (hasPerformedWork) TimeScope.REMAINING else TimeScope.WHOLE_SESSION,
        hasPerformedWork = hasPerformedWork,
        keptNames = day.exercises
            .filter { exercise -> exercise.sets.any { it.omittedBy == null && !it.isCompleted } }
            .filterNot { it.exerciseKey in touched }
            .map { it.name },
        replacedFrom = replaced?.fromName,
        replacedTo = replaced?.toName,
        tradeoffs = summary.tradeoffs.map { it.toUi(catalog) },
        rows = summary.rows.map { it.toUi(proposal) }
    )
}

private val ChangeRow.key: String
    get() = when (this) {
        is ChangeRow.Reduced -> exerciseKey
        is ChangeRow.Omitted -> exerciseKey
        is ChangeRow.Replaced -> fromKey
    }

private fun Tradeoff.toUi(catalog: ExerciseCatalog): TradeoffUi = TradeoffUi(
    code = code,
    regions = regions,
    exerciseName = exerciseKeys.lastOrNull()?.let { catalog[it]?.name }
)

private fun ChangeRow.toUi(proposal: AdjustmentProposal): ChangeRowUi = when (this) {
    is ChangeRow.Reduced -> ChangeRowUi.Reduced(name, fromSets, toSets)
    is ChangeRow.Omitted -> ChangeRowUi.Omitted(name)
    is ChangeRow.Replaced -> ChangeRowUi.Replaced(
        fromName = fromName,
        toName = toName,
        sets = sets,
        reps = proposal.repsAfter(toKey)
    )
}

private fun AdjustmentProposal.repsAfter(catalogKey: String): String {
    val after = changes.firstOrNull {
        it.kind == ChangeKind.REPLACE_UNPERFORMED && it.after?.catalogKey == catalogKey
    }?.after ?: return ""
    val targets = after.sets.mapNotNull { it.targetReps ?: it.targetSeconds }.distinct()
    val low = targets.minOrNull() ?: return ""
    val high = targets.max()
    return if (low == high) "$low" else "$low–$high"
}
