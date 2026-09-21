package com.jericx.trainr.data.repository

import com.jericx.trainr.data.local.UnstuckDao
import com.jericx.trainr.data.local.UnstuckMapper
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.TrainingPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AdjustmentRepositoryImpl(
    private val dao: UnstuckDao,
    private val mapper: UnstuckMapper
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
