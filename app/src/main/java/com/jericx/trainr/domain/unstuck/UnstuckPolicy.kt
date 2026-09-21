package com.jericx.trainr.domain.unstuck

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.catalog.isLoadable
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.generation.LoadStep
import com.jericx.trainr.domain.generation.RepWindow
import com.jericx.trainr.domain.generation.SeedLoad
import com.jericx.trainr.domain.generation.SessionShape
import com.jericx.trainr.domain.generation.SlotTier
import com.jericx.trainr.domain.generation.Snap
import com.jericx.trainr.domain.generation.dropTiers
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise

// Every number here comes from domain/generation, nothing written here
// reaches storage, and refusing to change the day is a result like any other.
class UnstuckPolicy(private val catalog: ExerciseCatalog) {

    fun decide(
        snapshot: AdjustmentSnapshot,
        constraint: AdjustmentConstraint,
        requestId: String
    ): PolicyDecision = when (constraint) {
        is AdjustmentConstraint.LessTime -> shorten(snapshot, constraint, requestId)
        is AdjustmentConstraint.EquipmentUnavailable -> substitute(snapshot, constraint, requestId)
    }

    private fun shorten(
        snapshot: AdjustmentSnapshot,
        constraint: AdjustmentConstraint.LessTime,
        requestId: String
    ): PolicyDecision {
        val day = snapshot.day
        val user = snapshot.user
        if (!TimePresets.isSupported(constraint.minutes)) {
            return PolicyDecision.NoFeasibleChange(InfeasibleReason.INVALID_MINUTES, null)
        }
        if (day.exercises.none { it.unperformed().isNotEmpty() }) {
            return PolicyDecision.NoChange(NoChangeReason.NOTHING_UNPERFORMED, null)
        }
        val before = estimate(day, user, constraint.scope)
        if (before <= constraint.minutes) {
            return PolicyDecision.NoChange(NoChangeReason.ALREADY_FITS, before)
        }

        val tiers = SessionTiers.assign(day, catalog, snapshot.priority)
        val shape = SessionShape.forGoal(user.fitnessGoal)
        val kept = day.exercises.associate { it.id to it.unperformed().size }.toMutableMap()
        val fits = { estimate(day.shrunkTo(kept), user, constraint.scope) <= constraint.minutes }
        val order = droppable(day, tiers, shape)

        var moved = true
        while (moved && !fits()) {
            moved = false
            for (exercise in order) {
                if (!exercise.canLoseASet(kept, tiers, shape)) continue
                kept[exercise.id] = kept.getValue(exercise.id) - 1
                moved = true
                if (fits()) break
            }
        }
        if (!fits()) {
            for (exercise in order) {
                if (kept.getValue(exercise.id) == 0) continue
                kept[exercise.id] = 0
                if (fits()) break
            }
        }
        if (!fits()) {
            day.exercises.firstOrNull { tiers[it.id] == SlotTier.PRIMARY_COMPOUND }?.let { primary ->
                while (primary.canLoseASet(kept, tiers, shape)) {
                    kept[primary.id] = kept.getValue(primary.id) - 1
                    if (fits()) break
                }
            }
        }

        val after = estimate(day.shrunkTo(kept), user, constraint.scope)
        if (after > constraint.minutes) {
            return PolicyDecision.NoFeasibleChange(InfeasibleReason.TOO_SHORT_FOR_REQUIRED_WORK, after)
        }

        val touched = day.exercises.filter { kept.getValue(it.id) < it.unperformed().size }
        val changes = touched.map { it.reduction(kept.getValue(it.id)) }
        val omitted = changes.any { it.kind == ChangeKind.OMIT_UNPERFORMED }
        val code = if (omitted) TradeoffCode.REDUCED_SESSION else TradeoffCode.LESS_WORK_FOR_REGIONS
        val proposal = AdjustmentProposal(
            proposalId = proposalId(requestId, PlanRevision.of(day), constraint),
            requestId = requestId,
            sessionId = "day:${day.id}",
            baseRevision = PlanRevision.of(day),
            policyVersion = VERSION,
            changes = changes,
            preservedPerformedSetIds = day.performedSetIds(),
            reasonCode = ReasonCode.TIME_CONSTRAINT,
            tradeoffCode = code.wire,
            factReferences = listOf(
                "budget:${constraint.minutes}:${constraint.scope.name.lowercase()}",
                "estimate_before:$before",
                "estimate_after:$after",
                "goal:${user.fitnessGoal.name.lowercase()}",
                "revision:${PlanRevision.of(day)}"
            )
        )
        val summary = ProposalSummary(
            kind = ProposalKind.SHORTER_SESSION,
            keptPriorityKey = day.exercises.firstOrNull { tiers[it.id] == SlotTier.PRIMARY_COMPOUND }
                ?.exerciseKey,
            tradeoffs = listOf(
                Tradeoff(
                    code = code,
                    regions = touched.mapNotNull { catalog[it.exerciseKey]?.primary?.region }
                        .distinct().sorted(),
                    exerciseKeys = touched.map { it.exerciseKey }
                )
            ),
            rows = touched.map { it.row(kept.getValue(it.id)) },
            estimateBeforeMinutes = before,
            estimateAfterMinutes = after,
            budgetMinutes = constraint.minutes
        )
        return PolicyDecision.Proposed(proposal, summary)
    }

    private fun substitute(
        snapshot: AdjustmentSnapshot,
        constraint: AdjustmentConstraint.EquipmentUnavailable,
        requestId: String
    ): PolicyDecision {
        val day = snapshot.day
        val user = snapshot.user
        val target = day.exercises.firstOrNull { it.id == constraint.exerciseId }
            ?: return PolicyDecision.NoFeasibleChange(InfeasibleReason.UNKNOWN_EXERCISE, null)
        val entry = catalog[target.exerciseKey]
            ?: return PolicyDecision.NoFeasibleChange(InfeasibleReason.UNKNOWN_EXERCISE, null)
        val unperformed = target.unperformed()
        if (unperformed.isEmpty()) {
            return PolicyDecision.NoChange(NoChangeReason.NOTHING_UNPERFORMED, null)
        }

        val candidate = candidateFor(target, entry, constraint.available, day, user)
            ?: return PolicyDecision.NoFeasibleChange(InfeasibleReason.NO_ELIGIBLE_SUBSTITUTE, null)

        val revision = PlanRevision.of(day)
        val change = ProposalChange(
            kind = ChangeKind.REPLACE_UNPERFORMED,
            before = ExerciseSnapshot(
                exerciseInstanceId = "exercise:${target.id}",
                catalogKey = target.exerciseKey,
                sets = unperformed.map { it.snapshot(target.restTime) }
            ),
            after = ExerciseSnapshot(
                exerciseInstanceId = "new",
                catalogKey = candidate.key,
                sets = unperformed.mapIndexed { index, set ->
                    SetSnapshot(
                        setId = "new:${index + 1}",
                        targetReps = set.targetReps,
                        targetWeightKg = seedKg(user, candidate, set),
                        targetSeconds = set.targetSeconds,
                        restSeconds = target.restTime
                    )
                }
            )
        )
        val tradeoffs = tradeoffsFor(entry, candidate)
        val proposal = AdjustmentProposal(
            proposalId = proposalId(requestId, revision, constraint),
            requestId = requestId,
            sessionId = "day:${day.id}",
            baseRevision = revision,
            policyVersion = VERSION,
            changes = listOf(change),
            preservedPerformedSetIds = day.performedSetIds(),
            reasonCode = ReasonCode.EQUIPMENT_CONSTRAINT,
            tradeoffCode = tradeoffs.first().code.wire,
            factReferences = listOf(
                "exercise:${target.id}",
                "available:${constraint.available.map { it.name.lowercase() }.sorted().joinToString(",")}",
                "candidate:${candidate.key}",
                "goal:${user.fitnessGoal.name.lowercase()}",
                "revision:$revision"
            )
        )
        val summary = ProposalSummary(
            kind = ProposalKind.SUBSTITUTE,
            keptPriorityKey = snapshot.priority?.catalogKey?.takeIf { it != target.exerciseKey },
            tradeoffs = tradeoffs,
            rows = listOf(
                ChangeRow.Replaced(
                    fromKey = target.exerciseKey,
                    fromName = target.name,
                    toKey = candidate.key,
                    toName = candidate.name,
                    sets = unperformed.size
                )
            ),
            estimateBeforeMinutes = null,
            estimateAfterMinutes = null,
            budgetMinutes = null
        )
        return PolicyDecision.Proposed(proposal, summary)
    }

    private fun candidateFor(
        target: WorkoutExercise,
        entry: CatalogExercise,
        available: Set<Equipment>,
        day: WorkoutDay,
        user: UserProfile
    ): CatalogExercise? {
        val taken = day.exercises.filterNot { it.id == target.id }.map { it.exerciseKey }.toSet()
        val eligible = catalog.all.filter {
            it.key != entry.key && it.key !in taken && it.isAvailableWith(available) &&
                !InjuryGuard.excludes(it, user.injuries) &&
                (it.primary == entry.primary ||
                    (it.pattern == entry.pattern && it.role == ExerciseRole.COMPOUND))
        }
        val pool = eligible.filter { it.measure == target.measure }
            .ifEmpty { eligible.filter { it.measure in interchangeable(target.measure) } }
        return pool.sortedWith(
            compareBy(
                { closeness(it, entry) },
                { if (it.staple) 0 else 1 },
                { -it.secondary.count { muscle -> muscle in entry.secondary } },
                { it.key }
            )
        ).firstOrNull()
    }

    // A rep target transfers between a loaded and an unloaded movement; a hold
    // measured in seconds does not become one measured in reps.
    private fun interchangeable(measure: ExerciseMeasure): Set<ExerciseMeasure> =
        if (measure == ExerciseMeasure.DURATION) {
            setOf(ExerciseMeasure.DURATION)
        } else {
            setOf(ExerciseMeasure.REPS, ExerciseMeasure.WEIGHT_AND_REPS)
        }

    private fun closeness(candidate: CatalogExercise, target: CatalogExercise): Int = when {
        candidate.primary == target.primary && candidate.pattern == target.pattern -> 0
        candidate.pattern == target.pattern && candidate.role == ExerciseRole.COMPOUND -> 1
        else -> 2
    }

    private fun tradeoffsFor(entry: CatalogExercise, candidate: CatalogExercise): List<Tradeoff> {
        val keys = listOf(entry.key, candidate.key)
        val named = buildList {
            if (entry.equipment == Equipment.BARBELL && candidate.equipment != Equipment.BARBELL) {
                add(Tradeoff(TradeoffCode.LESS_BARBELL_PRACTICE, exerciseKeys = keys))
            } else if (entry.equipment != candidate.equipment) {
                add(Tradeoff(TradeoffCode.DIFFERENT_RESISTANCE, exerciseKeys = keys))
            }
            if (candidate.isLoadable) {
                add(Tradeoff(TradeoffCode.SEPARATE_LOAD_HISTORY, exerciseKeys = listOf(candidate.key)))
            }
        }
        return named.ifEmpty { listOf(Tradeoff(TradeoffCode.DIFFERENT_RESISTANCE, exerciseKeys = keys)) }
    }

    // A seed, never the weight that was on the bar: the two movements do not
    // share a load history (C08).
    private fun seedKg(user: UserProfile, candidate: CatalogExercise, set: ExerciseSet): Float? {
        if (!candidate.isLoadable) return null
        val reps = set.targetReps ?: RepWindow.forExercise(user, candidate).first
        return SeedLoad.loadKg(user, candidate, reps)
            ?.let { LoadStep.snap(it, candidate, user.weightUnits, Snap.DOWN) }
    }

    private fun estimate(day: WorkoutDay, user: UserProfile, scope: TimeScope): Int =
        SessionEstimate.minutes(day, user, scope, catalog)

    private fun droppable(
        day: WorkoutDay,
        tiers: Map<Long, SlotTier>,
        shape: SessionShape
    ): List<WorkoutExercise> {
        val pools = day.exercises
            .filter { tiers.getValue(it.id) !in setOf(SlotTier.WARM_UP, SlotTier.PRIMARY_COMPOUND) }
            .groupBy { tiers.getValue(it.id) }
            .mapValues { (_, exercises) -> exercises.sortedByDescending { it.sortOrder } }
        val taken = mutableMapOf<SlotTier, Int>()
        val ordered = mutableListOf<WorkoutExercise>()
        shape.dropTiers().forEach { tier ->
            val pool = pools[tier] ?: return@forEach
            val next = taken[tier] ?: 0
            if (next < pool.size) {
                ordered += pool[next]
                taken[tier] = next + 1
            }
        }
        val listed = ordered.map { it.id }.toSet()
        return ordered + pools.values.flatten().filterNot { it.id in listed }
            .sortedByDescending { it.sortOrder }
    }

    private fun WorkoutExercise.canLoseASet(
        kept: Map<Long, Int>,
        tiers: Map<Long, SlotTier>,
        shape: SessionShape
    ): Boolean {
        val remaining = kept.getValue(id)
        if (remaining <= 0) return false
        val floor = shape.sets[tiers.getValue(id)]?.first ?: 1
        return performed().size + remaining - 1 >= floor
    }

    private fun WorkoutDay.shrunkTo(kept: Map<Long, Int>): WorkoutDay =
        copy(exercises = exercises.map { exercise -> exercise.shrunkTo(kept.getValue(exercise.id)) })

    private fun WorkoutExercise.shrunkTo(kept: Int): WorkoutExercise {
        val shed = unperformed().drop(kept).map { it.setNumber }.toSet()
        return copy(sets = sets.filterNot { it.isUnperformed && it.setNumber in shed })
    }

    private fun WorkoutExercise.reduction(kept: Int): ProposalChange {
        val unperformed = unperformed()
        val snapshots = unperformed.map { it.snapshot(restTime) }
        val omit = kept == 0 && performed().isEmpty()
        return ProposalChange(
            kind = if (omit) ChangeKind.OMIT_UNPERFORMED else ChangeKind.REDUCE_UNPERFORMED,
            before = ExerciseSnapshot("exercise:$id", exerciseKey, snapshots),
            after = if (omit) null else ExerciseSnapshot("exercise:$id", exerciseKey, snapshots.take(kept))
        )
    }

    private fun WorkoutExercise.row(kept: Int): ChangeRow {
        val performed = performed().size
        val planned = performed + unperformed().size
        return if (kept == 0 && performed == 0) {
            ChangeRow.Omitted(exerciseKey, name, planned)
        } else {
            ChangeRow.Reduced(exerciseKey, name, planned, performed + kept)
        }
    }

    private fun WorkoutDay.performedSetIds(): List<String> =
        exercises.flatMap { exercise -> exercise.sets.filter { it.isCompleted }.map { "set:${it.id}" } }

    private fun ExerciseSet.snapshot(restSeconds: Int?): SetSnapshot =
        SetSnapshot("set:$id", targetReps, targetWeightKg, targetSeconds, restSeconds)

    private fun proposalId(
        requestId: String,
        revision: String,
        constraint: AdjustmentConstraint
    ): String = shortDigest(requestId + revision + constraint.canonicalKey())

    companion object {
        const val VERSION = "unstuck-policy-2026.09-unreviewed"
    }
}

// An idempotency key cannot be a data class toString: a Set renders in
// insertion order and a new property would change every id ever issued.
private fun AdjustmentConstraint.canonicalKey(): String = when (this) {
    is AdjustmentConstraint.LessTime -> "less_time:$minutes:${scope.name}"
    is AdjustmentConstraint.EquipmentUnavailable ->
        "equipment_unavailable:$exerciseId:${available.map { it.name }.sorted().joinToString(",")}"
}

private val ExerciseSet.isUnperformed: Boolean get() = omittedBy == null && !isCompleted

private fun WorkoutExercise.unperformed(): List<ExerciseSet> =
    sets.filter { it.isUnperformed }.sortedWith(compareBy({ it.setNumber }, { it.id }))

private fun WorkoutExercise.performed(): List<ExerciseSet> =
    sets.filter { it.omittedBy == null && it.isCompleted }
