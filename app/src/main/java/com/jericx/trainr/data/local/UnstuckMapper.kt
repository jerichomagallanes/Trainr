package com.jericx.trainr.data.local

import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.domain.unstuck.FinishKind
import com.jericx.trainr.domain.unstuck.PreferenceKind
import com.jericx.trainr.domain.unstuck.ProposalJson
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.SessionOutcome
import com.jericx.trainr.domain.unstuck.TrainingPreference

class UnstuckMapper {

    fun mapToEntity(outcome: SessionOutcome): SessionOutcomeEntity {
        return SessionOutcomeEntity(
            id = outcome.id,
            workoutDayId = outcome.workoutDayId,
            finishKind = outcome.finishKind.name,
            finishedAt = outcome.finishedAt,
            performedSetCount = outcome.performedSetCount,
            plannedSetCount = outcome.plannedSetCount
        )
    }

    fun mapToDomain(entity: SessionOutcomeEntity): SessionOutcome {
        return SessionOutcome(
            id = entity.id,
            workoutDayId = entity.workoutDayId,
            finishKind = runCatching { FinishKind.valueOf(entity.finishKind) }
                .getOrDefault(FinishKind.PARTIAL),
            finishedAt = entity.finishedAt,
            performedSetCount = entity.performedSetCount,
            plannedSetCount = entity.plannedSetCount
        )
    }

    fun mapToEntity(adjustment: AppliedAdjustment): AppliedAdjustmentEntity {
        return AppliedAdjustmentEntity(
            id = adjustment.id,
            workoutDayId = adjustment.workoutDayId,
            proposalId = adjustment.proposal.proposalId,
            reason = adjustment.reason.name,
            proposalJson = ProposalJson.encode(adjustment.proposal),
            policyVersion = adjustment.proposal.policyVersion,
            appliedAt = adjustment.appliedAt,
            undoneAt = adjustment.undoneAt
        )
    }

    fun mapToDomain(entity: AppliedAdjustmentEntity): AppliedAdjustment {
        return AppliedAdjustment(
            id = entity.id,
            workoutDayId = entity.workoutDayId,
            proposal = ProposalJson.decode(entity.proposalJson),
            reason = runCatching { AdjustmentReason.valueOf(entity.reason) }
                .getOrDefault(AdjustmentReason.LESS_TIME),
            appliedAt = entity.appliedAt,
            undoneAt = entity.undoneAt
        )
    }

    fun mapToEntity(feedback: AdjustmentFeedback): AdjustmentFeedbackEntity {
        return AdjustmentFeedbackEntity(
            id = feedback.id,
            adjustmentId = feedback.adjustmentId,
            answer = feedback.answer?.name,
            answeredAt = feedback.answeredAt,
            dismissedAt = feedback.dismissedAt
        )
    }

    fun mapToDomain(entity: AdjustmentFeedbackEntity): AdjustmentFeedback {
        return AdjustmentFeedback(
            id = entity.id,
            adjustmentId = entity.adjustmentId,
            answer = entity.answer?.let { runCatching { FeedbackAnswer.valueOf(it) }.getOrNull() },
            answeredAt = entity.answeredAt,
            dismissedAt = entity.dismissedAt
        )
    }

    fun mapToEntity(preference: TrainingPreference): TrainingPreferenceEntity {
        return TrainingPreferenceEntity(
            id = preference.id,
            userId = preference.userId,
            kind = preference.kind.name,
            minutes = preference.minutes,
            weekday = preference.weekday,
            sourceAdjustmentId = preference.sourceAdjustmentId,
            confirmedAt = preference.confirmedAt,
            updatedAt = preference.updatedAt
        )
    }

    fun mapToDomain(entity: TrainingPreferenceEntity): TrainingPreference {
        return TrainingPreference(
            id = entity.id,
            userId = entity.userId,
            kind = runCatching { PreferenceKind.valueOf(entity.kind) }
                .getOrDefault(PreferenceKind.TIME_LIMIT),
            minutes = entity.minutes,
            weekday = entity.weekday,
            sourceAdjustmentId = entity.sourceAdjustmentId,
            confirmedAt = entity.confirmedAt,
            updatedAt = entity.updatedAt
        )
    }

    fun mapToEntity(note: SessionNote): SessionNoteEntity {
        return SessionNoteEntity(
            id = note.id,
            userId = note.userId,
            workoutDayId = note.workoutDayId,
            text = note.text,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt
        )
    }

    fun mapToDomain(entity: SessionNoteEntity): SessionNote {
        return SessionNote(
            id = entity.id,
            userId = entity.userId,
            workoutDayId = entity.workoutDayId,
            text = entity.text,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
