package com.jericx.trainr.data.repository

import androidx.room.withTransaction
import com.jericx.trainr.data.local.AppliedAdjustmentEntity
import com.jericx.trainr.data.local.ExerciseSetEntity
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.local.UnstuckDao
import com.jericx.trainr.data.local.UnstuckMapper
import com.jericx.trainr.data.local.UserDao
import com.jericx.trainr.data.local.UserMapper
import com.jericx.trainr.data.local.WorkoutExerciseEntity
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.SessionMinutes
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.ActualOrigin
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ApplyRejection
import com.jericx.trainr.domain.unstuck.ApplyResult
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.ExerciseSnapshot
import com.jericx.trainr.domain.unstuck.PlanRevision
import com.jericx.trainr.domain.unstuck.ProposalChange
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.SetSnapshot
import com.jericx.trainr.domain.unstuck.TrainingPreference
import com.jericx.trainr.domain.unstuck.UndoResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AdjustmentRepositoryImpl(
    private val database: TrainrDatabase,
    private val userDao: UserDao,
    private val dao: UnstuckDao,
    private val mapper: UnstuckMapper,
    private val userMapper: UserMapper,
    private val catalog: ExerciseCatalog
) : AdjustmentRepository {

    override suspend fun saveOutcome(outcome: SessionOutcome): Long {
        return dao.upsertOutcome(mapper.mapToEntity(outcome))
    }

    override suspend fun getOutcome(dayId: Long): SessionOutcome? {
        return dao.getOutcomeForDay(dayId)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun getOutcomes(dayIds: List<Long>): List<SessionOutcome> {
        if (dayIds.isEmpty()) return emptyList()
        return dao.getOutcomesForDays(dayIds).map { mapper.mapToDomain(it) }
    }

    override suspend fun recordAdjustment(applied: AppliedAdjustment): Long {
        return dao.insertAdjustment(mapper.mapToEntity(applied))
    }

    override suspend fun getAdjustment(proposalId: String): AppliedAdjustment? {
        return dao.getAdjustmentByProposalId(proposalId)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun getAdjustmentById(id: Long): AppliedAdjustment? {
        return dao.getAdjustmentById(id)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun getActiveAdjustment(dayId: Long): AppliedAdjustment? {
        return dao.getActiveAdjustmentForDay(dayId)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun getAdjustments(dayId: Long): List<AppliedAdjustment> {
        return dao.getAdjustmentsForDay(dayId).map { mapper.mapToDomain(it) }
    }

    override suspend fun markUndone(id: Long, at: Long) {
        dao.markUndone(id, at)
    }

    override suspend fun markReapplied(id: Long) {
        dao.markReapplied(id)
    }

    override suspend fun apply(
        proposal: AdjustmentProposal,
        dayId: Long,
        reason: AdjustmentReason,
        nowMillis: Long
    ): ApplyResult = applying {
        val existing = dao.getAdjustmentByProposalId(proposal.proposalId)
        if (existing != null && existing.workoutDayId != dayId) {
            throw ApplyException(ApplyRejection.WRONG_SCOPE)
        }
        if (existing != null && existing.undoneAt == null) {
            return@applying ApplyResult.AlreadyApplied(mapper.mapToDomain(existing))
        }
        if (proposal.scope != TODAY_ONLY || proposal.sessionId != "$DAY_PREFIX$dayId") {
            throw ApplyException(ApplyRejection.WRONG_SCOPE)
        }
        if (proposal.changes.isEmpty()) throw ApplyException(ApplyRejection.EMPTY_CHANGES)
        if (existing != null) return@applying reapplied(existing)

        val day = loadDay(dayId) ?: throw ApplyException(ApplyRejection.UNKNOWN_DAY)
        val revision = PlanRevision.of(day)
        if (revision != proposal.baseRevision) return@applying ApplyResult.Stale(revision)
        validate(proposal, day)

        val record = AppliedAdjustment(
            workoutDayId = dayId,
            proposal = proposal,
            reason = reason,
            appliedAt = nowMillis
        )
        val id = dao.insertAdjustment(mapper.mapToEntity(record))
        ApplyResult.Applied(record.copy(id = id), patch(proposal, day, id, strict = true))
    }

    override suspend fun reapply(adjustmentId: Long, nowMillis: Long): ApplyResult = applying {
        val entity = dao.getAdjustmentById(adjustmentId)
            ?: throw IllegalArgumentException("No adjustment $adjustmentId")
        if (entity.undoneAt == null) {
            return@applying ApplyResult.AlreadyApplied(mapper.mapToDomain(entity))
        }
        reapplied(entity)
    }

    override suspend fun undo(adjustmentId: Long, nowMillis: Long): UndoResult = try {
        database.withTransaction {
            val entity = dao.getAdjustmentById(adjustmentId)
            when {
                entity == null -> UndoResult.Unknown
                entity.undoneAt != null -> UndoResult.AlreadyUndone
                else -> {
                    userDao.restoreOmittedSets(adjustmentId)
                    val kept = userDao.getExercisesAddedBy(adjustmentId).sumOf { withdraw(it) }
                    dao.markUndone(adjustmentId, nowMillis)
                    UndoResult.Restored(mapper.mapToDomain(entity.copy(undoneAt = nowMillis)), kept)
                }
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        UndoResult.Failed(failure)
    }

    private suspend fun reapplied(entity: AppliedAdjustmentEntity): ApplyResult {
        val stored = mapper.mapToDomain(entity)
        val day = loadDay(entity.workoutDayId) ?: throw ApplyException(ApplyRejection.UNKNOWN_DAY)
        val addedExerciseId = patch(stored.proposal, day, entity.id, strict = false)
        dao.markReapplied(entity.id)
        return ApplyResult.Applied(stored.copy(undoneAt = null), addedExerciseId)
    }

    private suspend fun applying(block: suspend () -> ApplyResult): ApplyResult = try {
        database.withTransaction { block() }
    } catch (rejected: ApplyException) {
        ApplyResult.Rejected(rejected.rejection)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        ApplyResult.Failed(failure)
    }

    private fun validate(proposal: AdjustmentProposal, day: WorkoutDay) {
        val performed = day.exercises.flatMap { it.sets }
            .filter { it.isCompleted }.map { "$SET_PREFIX${it.id}" }.toSet()
        if (performed != proposal.preservedPerformedSetIds.toSet()) {
            throw ApplyException(ApplyRejection.PERFORMED_SETS_MISMATCH)
        }
        proposal.changes.forEach { change ->
            val exercise = day.exerciseFor(change.before)
            if (change.before.catalogKey != exercise.exerciseKey) {
                throw ApplyException(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH)
            }
            if (!exercise.unperformed().matches(change.before.sets)) {
                throw ApplyException(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH)
            }
            when (change.kind) {
                ChangeKind.OMIT_UNPERFORMED ->
                    if (change.after != null) throw ApplyException(ApplyRejection.AFTER_NOT_A_SUBSET)
                ChangeKind.REDUCE_UNPERFORMED -> validateReduction(change)
                ChangeKind.REPLACE_UNPERFORMED -> validateReplacement(change)
            }
        }
    }

    private fun validateReduction(change: ProposalChange) {
        val after = change.after ?: throw ApplyException(ApplyRejection.AFTER_NOT_A_SUBSET)
        val beforeIds = change.before.sets.map { it.setId }.toSet()
        val afterIds = after.sets.map { it.setId }.toSet()
        val sameExercise = after.exerciseInstanceId == change.before.exerciseInstanceId &&
            after.catalogKey == change.before.catalogKey
        if (!sameExercise || afterIds.size != after.sets.size ||
            !beforeIds.containsAll(afterIds) || afterIds.size >= beforeIds.size
        ) {
            throw ApplyException(ApplyRejection.AFTER_NOT_A_SUBSET)
        }
    }

    private fun validateReplacement(change: ProposalChange) {
        val after = change.after ?: throw ApplyException(ApplyRejection.AFTER_NOT_A_SUBSET)
        if (after.exerciseInstanceId != NEW) throw ApplyException(ApplyRejection.BAD_PLACEHOLDER)
        if (catalog[after.catalogKey] == null) throw ApplyException(ApplyRejection.UNKNOWN_CATALOG_KEY)
        if (after.sets.size > change.before.sets.size) {
            throw ApplyException(ApplyRejection.AFTER_NOT_A_SUBSET)
        }
        after.sets.forEachIndexed { index, set ->
            if (set.setId != "$NEW:${index + 1}") throw ApplyException(ApplyRejection.BAD_PLACEHOLDER)
        }
    }

    private suspend fun patch(
        proposal: AdjustmentProposal,
        day: WorkoutDay,
        adjustmentId: Long,
        strict: Boolean
    ): Long? {
        val standing = if (strict) emptyList() else userDao.getExercisesAddedBy(adjustmentId)
        var addedExerciseId: Long? = null
        proposal.changes.forEach { change ->
            val exercise = day.exerciseFor(change.before)
            val kept = change.after?.takeIf { change.kind != ChangeKind.REPLACE_UNPERFORMED }
                ?.sets?.map { it.setId }.orEmpty().toSet()
            val shed = change.before.sets.map { it.setId }.filterNot { it in kept }
                .mapNotNull { it.idAfter(SET_PREFIX) }
            omit(if (strict) shed else shed.filter { it in exercise.unperformedIds() }, adjustmentId)
            if (change.kind != ChangeKind.REPLACE_UNPERFORMED) return@forEach
            val after = change.after ?: throw ApplyException(ApplyRejection.AFTER_NOT_A_SUBSET)
            addedExerciseId = standing.firstOrNull { it.exerciseKey == after.catalogKey }
                ?.let { topUp(it, after) }
                ?: insertSubstitute(after, exercise, day.id, adjustmentId)
        }
        return addedExerciseId
    }

    // omitSets passes over a performed row, so a short count means the plan
    // moved under the snapshot this proposal was built from.
    private suspend fun omit(setIds: List<Long>, adjustmentId: Long) {
        if (setIds.isEmpty()) return
        if (userDao.omitSets(setIds, adjustmentId) != setIds.size) {
            throw ApplyException(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH)
        }
    }

    private suspend fun insertSubstitute(
        after: ExerciseSnapshot,
        original: WorkoutExercise,
        dayId: Long,
        adjustmentId: Long
    ): Long {
        val candidate = catalog[after.catalogKey]
            ?: throw ApplyException(ApplyRejection.UNKNOWN_CATALOG_KEY)
        val restSeconds = after.sets.firstOrNull()?.restSeconds
        val exerciseId = userDao.insertWorkoutExercise(
            WorkoutExerciseEntity(
                workoutDayId = dayId,
                exerciseKey = candidate.key,
                name = candidate.name,
                measure = candidate.measure.name,
                setCount = after.sets.size,
                reps = null,
                duration = null,
                durationMinutes = SessionMinutes.forExercise(
                    measure = candidate.measure,
                    perSet = after.sets.map { it.work(candidate.measure) },
                    restSeconds = restSeconds ?: 0,
                    unilateral = candidate.unilateral
                ),
                restTime = restSeconds,
                equipment = listOf(candidate.equipment.name),
                videoTutorialUrl = null,
                isCompleted = false,
                notes = "",
                sortOrder = original.sortOrder,
                addedBy = adjustmentId
            )
        )
        userDao.insertExerciseSets(
            after.sets.mapIndexed { index, set -> plannedSet(exerciseId, index + 1, set) }
        )
        return exerciseId
    }

    private suspend fun topUp(added: WorkoutExerciseEntity, after: ExerciseSnapshot): Long {
        val present = userDao.getSetsForExercise(added.id).map { it.setNumber }.toSet()
        val missing = after.sets.mapIndexed { index, set -> index + 1 to set }
            .filterNot { (setNumber, _) -> setNumber in present }
        if (missing.isNotEmpty()) {
            userDao.insertExerciseSets(missing.map { (setNumber, set) -> plannedSet(added.id, setNumber, set) })
        }
        userDao.updateWorkoutExercise(added.copy(setCount = after.sets.size))
        return added.id
    }

    private fun plannedSet(exerciseId: Long, setNumber: Int, set: SetSnapshot) = ExerciseSetEntity(
        workoutExerciseId = exerciseId,
        setNumber = setNumber,
        targetReps = set.targetReps,
        targetWeightKg = set.targetWeightKg,
        targetSeconds = set.targetSeconds,
        actualReps = null,
        actualWeightKg = null,
        actualSeconds = null,
        isCompleted = false,
        actualOrigin = ActualOrigin.NONE.name
    )

    private suspend fun withdraw(added: WorkoutExerciseEntity): Int {
        val performed = userDao.getSetsForExercise(added.id).count { it.isCompleted }
        if (performed == 0) {
            userDao.deleteAddedExercise(added.id)
            return 0
        }
        userDao.deleteUnperformedSets(added.id)
        userDao.updateWorkoutExercise(added.copy(setCount = performed))
        return performed
    }

    private suspend fun loadDay(dayId: Long): WorkoutDay? {
        val entity = userDao.getWorkoutDayById(dayId) ?: return null
        val exercises = userDao.getExercisesForWorkoutDay(dayId).map { exercise ->
            userMapper.mapToDomain(exercise).copy(
                sets = userDao.getSetsForExercise(exercise.id).map { userMapper.mapToDomain(it) }
            )
        }
        return userMapper.mapToDomain(entity, exercises)
    }

    override suspend fun saveFeedback(feedback: AdjustmentFeedback): Long {
        return dao.upsertFeedback(mapper.mapToEntity(feedback))
    }

    override suspend fun getFeedback(adjustmentId: Long): AdjustmentFeedback? {
        return dao.getFeedbackForAdjustment(adjustmentId)?.let { mapper.mapToDomain(it) }
    }

    override suspend fun savePreference(preference: TrainingPreference): Long {
        return dao.insertPreference(mapper.mapToEntity(preference))
    }

    override suspend fun updatePreference(preference: TrainingPreference) {
        dao.updatePreference(mapper.mapToEntity(preference))
    }

    override suspend fun deletePreference(id: Long) {
        dao.deletePreference(id)
    }

    override fun observePreferences(userId: Long): Flow<List<TrainingPreference>> {
        return dao.getPreferences(userId).map { entities -> entities.map { mapper.mapToDomain(it) } }
    }

    override suspend fun getPreferences(userId: Long): List<TrainingPreference> {
        return dao.getPreferencesOnce(userId).map { mapper.mapToDomain(it) }
    }

    override suspend fun saveNote(note: SessionNote): Long {
        return dao.insertNote(mapper.mapToEntity(note))
    }

    override suspend fun updateNote(note: SessionNote) {
        dao.updateNote(mapper.mapToEntity(note))
    }

    override suspend fun deleteNote(id: Long) {
        dao.deleteNote(id)
    }

    override fun observeNotes(userId: Long): Flow<List<SessionNote>> {
        return dao.getNotes(userId).map { entities -> entities.map { mapper.mapToDomain(it) } }
    }

    override suspend fun getNote(dayId: Long): SessionNote? {
        return dao.getNoteForDay(dayId)?.let { mapper.mapToDomain(it) }
    }
}

private class ApplyException(val rejection: ApplyRejection) : RuntimeException()

private fun WorkoutDay.exerciseFor(before: ExerciseSnapshot): WorkoutExercise {
    val id = before.exerciseInstanceId.idAfter(EXERCISE_PREFIX)
    return exercises.firstOrNull { it.id == id }
        ?: throw ApplyException(ApplyRejection.UNKNOWN_EXERCISE)
}

private fun String.idAfter(prefix: String): Long? =
    if (startsWith(prefix)) removePrefix(prefix).toLongOrNull() else null

private fun WorkoutExercise.unperformed(): List<ExerciseSet> =
    sets.filter { !it.isCompleted && it.omittedBy == null }
        .sortedWith(compareBy({ it.setNumber }, { it.id }))

private fun WorkoutExercise.unperformedIds(): Set<Long> = unperformed().map { it.id }.toSet()

private fun List<ExerciseSet>.matches(snapshots: List<SetSnapshot>): Boolean =
    size == snapshots.size && zip(snapshots).all { (set, snapshot) ->
        snapshot.setId == "$SET_PREFIX${set.id}" &&
            snapshot.targetReps == set.targetReps &&
            snapshot.targetWeightKg == set.targetWeightKg &&
            snapshot.targetSeconds == set.targetSeconds
    }

private fun SetSnapshot.work(measure: ExerciseMeasure): Int =
    if (measure == ExerciseMeasure.DURATION) targetSeconds ?: 0 else targetReps ?: 0

private const val TODAY_ONLY = "today_only"
private const val DAY_PREFIX = "day:"
private const val NEW = "new"
private const val SET_PREFIX = "set:"
private const val EXERCISE_PREFIX = "exercise:"
