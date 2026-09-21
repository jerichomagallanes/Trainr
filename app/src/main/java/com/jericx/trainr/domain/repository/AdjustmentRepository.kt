package com.jericx.trainr.domain.repository

import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.TrainingPreference
import kotlinx.coroutines.flow.Flow

interface AdjustmentRepository {
    suspend fun saveOutcome(outcome: SessionOutcome): Long
    suspend fun getOutcome(dayId: Long): SessionOutcome?
    suspend fun getOutcomes(dayIds: List<Long>): List<SessionOutcome>

    suspend fun recordAdjustment(applied: AppliedAdjustment): Long
    suspend fun getAdjustment(proposalId: String): AppliedAdjustment?
    suspend fun getActiveAdjustment(dayId: Long): AppliedAdjustment?
    suspend fun getAdjustments(dayId: Long): List<AppliedAdjustment>
    suspend fun markUndone(id: Long, at: Long)
    suspend fun markReapplied(id: Long)

    suspend fun saveFeedback(feedback: AdjustmentFeedback): Long
    suspend fun getFeedback(adjustmentId: Long): AdjustmentFeedback?

    suspend fun savePreference(preference: TrainingPreference): Long
    suspend fun updatePreference(preference: TrainingPreference)
    suspend fun deletePreference(id: Long)
    fun observePreferences(userId: Long): Flow<List<TrainingPreference>>
    suspend fun getPreferences(userId: Long): List<TrainingPreference>

    suspend fun saveNote(note: SessionNote): Long
    suspend fun updateNote(note: SessionNote)
    suspend fun deleteNote(id: Long)
    fun observeNotes(userId: Long): Flow<List<SessionNote>>
    suspend fun getNote(dayId: Long): SessionNote?
}
