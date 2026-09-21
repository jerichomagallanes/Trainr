package com.jericx.trainr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnstuckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutcome(outcome: SessionOutcomeEntity): Long

    @Query("SELECT * FROM session_outcomes WHERE workoutDayId = :dayId")
    suspend fun getOutcomeForDay(dayId: Long): SessionOutcomeEntity?

    @Query("SELECT * FROM session_outcomes WHERE workoutDayId IN (:dayIds)")
    suspend fun getOutcomesForDays(dayIds: List<Long>): List<SessionOutcomeEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdjustment(adjustment: AppliedAdjustmentEntity): Long

    @Query("SELECT * FROM applied_adjustments WHERE proposalId = :proposalId")
    suspend fun getAdjustmentByProposalId(proposalId: String): AppliedAdjustmentEntity?

    @Query("SELECT * FROM applied_adjustments WHERE id = :id")
    suspend fun getAdjustmentById(id: Long): AppliedAdjustmentEntity?

    @Query(
        """
        SELECT * FROM applied_adjustments
        WHERE workoutDayId = :dayId AND undoneAt IS NULL
        ORDER BY appliedAt DESC LIMIT 1
        """
    )
    suspend fun getActiveAdjustmentForDay(dayId: Long): AppliedAdjustmentEntity?

    @Query("SELECT * FROM applied_adjustments WHERE workoutDayId = :dayId ORDER BY appliedAt, id")
    suspend fun getAdjustmentsForDay(dayId: Long): List<AppliedAdjustmentEntity>

    @Query("UPDATE applied_adjustments SET undoneAt = :undoneAt WHERE id = :id")
    suspend fun markUndone(id: Long, undoneAt: Long)

    @Query("UPDATE applied_adjustments SET undoneAt = NULL WHERE id = :id")
    suspend fun markReapplied(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(feedback: AdjustmentFeedbackEntity): Long

    @Query("SELECT * FROM adjustment_feedback WHERE adjustmentId = :adjustmentId")
    suspend fun getFeedbackForAdjustment(adjustmentId: Long): AdjustmentFeedbackEntity?

    @Insert
    suspend fun insertPreference(preference: TrainingPreferenceEntity): Long

    @Update
    suspend fun updatePreference(preference: TrainingPreferenceEntity)

    @Query("DELETE FROM training_preferences WHERE id = :id")
    suspend fun deletePreference(id: Long)

    @Query("SELECT * FROM training_preferences WHERE userId = :userId ORDER BY weekday, id")
    fun getPreferences(userId: Long): Flow<List<TrainingPreferenceEntity>>

    @Query("SELECT * FROM training_preferences WHERE userId = :userId ORDER BY weekday, id")
    suspend fun getPreferencesOnce(userId: Long): List<TrainingPreferenceEntity>

    @Insert
    suspend fun insertNote(note: SessionNoteEntity): Long

    @Update
    suspend fun updateNote(note: SessionNoteEntity)

    @Query("DELETE FROM session_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM session_notes WHERE userId = :userId ORDER BY createdAt DESC, id DESC")
    fun getNotes(userId: Long): Flow<List<SessionNoteEntity>>

    @Query("SELECT * FROM session_notes WHERE workoutDayId = :dayId ORDER BY updatedAt DESC, id DESC LIMIT 1")
    suspend fun getNoteForDay(dayId: Long): SessionNoteEntity?
}
