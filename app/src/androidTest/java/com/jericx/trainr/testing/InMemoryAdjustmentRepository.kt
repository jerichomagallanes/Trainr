package com.jericx.trainr.testing

import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.TrainingPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Enough of the Unstuck records to put a route in front of a test without Room.
class InMemoryAdjustmentRepository : AdjustmentRepository {

    private var nextId = 1L
    private val outcomes = mutableMapOf<Long, SessionOutcome>()
    private val adjustments = mutableMapOf<Long, AppliedAdjustment>()
    private val feedback = mutableMapOf<Long, AdjustmentFeedback>()
    private val preferences = MutableStateFlow<Map<Long, TrainingPreference>>(emptyMap())
    private val notes = MutableStateFlow<Map<Long, SessionNote>>(emptyMap())

    private fun idFor(id: Long) = if (id == 0L) nextId++ else id

    override suspend fun saveOutcome(outcome: SessionOutcome): Long {
        val existing = outcomes.values.firstOrNull { it.workoutDayId == outcome.workoutDayId }
        val id = existing?.id ?: idFor(outcome.id)
        outcomes[id] = outcome.copy(id = id)
        return id
    }

    override suspend fun getOutcome(dayId: Long): SessionOutcome? =
        outcomes.values.firstOrNull { it.workoutDayId == dayId }

    override suspend fun getOutcomes(dayIds: List<Long>): List<SessionOutcome> =
        outcomes.values.filter { it.workoutDayId in dayIds }

    override suspend fun recordAdjustment(applied: AppliedAdjustment): Long {
        val id = idFor(applied.id)
        adjustments[id] = applied.copy(id = id)
        return id
    }

    override suspend fun getAdjustment(proposalId: String): AppliedAdjustment? =
        adjustments.values.firstOrNull { it.proposal.proposalId == proposalId }

    override suspend fun getActiveAdjustment(dayId: Long): AppliedAdjustment? =
        adjustments.values.firstOrNull { it.workoutDayId == dayId && it.isActive }

    override suspend fun getAdjustments(dayId: Long): List<AppliedAdjustment> =
        adjustments.values.filter { it.workoutDayId == dayId }

    override suspend fun markUndone(id: Long, at: Long) {
        adjustments[id]?.let { adjustments[id] = it.copy(undoneAt = at) }
    }

    override suspend fun markReapplied(id: Long) {
        adjustments[id]?.let { adjustments[id] = it.copy(undoneAt = null) }
    }

    override suspend fun saveFeedback(feedback: AdjustmentFeedback): Long {
        val id = idFor(feedback.id)
        this.feedback[id] = feedback.copy(id = id)
        return id
    }

    override suspend fun getFeedback(adjustmentId: Long): AdjustmentFeedback? =
        feedback.values.firstOrNull { it.adjustmentId == adjustmentId }

    override suspend fun savePreference(preference: TrainingPreference): Long {
        val id = idFor(preference.id)
        preferences.value = preferences.value + (id to preference.copy(id = id))
        return id
    }

    override suspend fun updatePreference(preference: TrainingPreference) {
        preferences.value = preferences.value + (preference.id to preference)
    }

    override suspend fun deletePreference(id: Long) {
        preferences.value = preferences.value - id
    }

    override fun observePreferences(userId: Long): Flow<List<TrainingPreference>> =
        preferences.map { all -> all.values.filter { it.userId == userId } }

    override suspend fun getPreferences(userId: Long): List<TrainingPreference> =
        preferences.value.values.filter { it.userId == userId }

    override suspend fun saveNote(note: SessionNote): Long {
        val id = idFor(note.id)
        notes.value = notes.value + (id to note.copy(id = id))
        return id
    }

    override suspend fun updateNote(note: SessionNote) {
        notes.value = notes.value + (note.id to note)
    }

    override suspend fun deleteNote(id: Long) {
        notes.value = notes.value - id
    }

    override fun observeNotes(userId: Long): Flow<List<SessionNote>> =
        notes.map { all -> all.values.filter { it.userId == userId } }

    override suspend fun getNote(dayId: Long): SessionNote? =
        notes.value.values.firstOrNull { it.workoutDayId == dayId }
}
