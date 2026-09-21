package com.jericx.trainr.domain.unstuck

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdjustmentProposal(
    val schemaVersion: String = "1.0",
    val proposalId: String,
    val requestId: String,
    val sessionId: String,
    val baseRevision: String,
    val policyVersion: String,
    val scope: String = "today_only",
    val changes: List<ProposalChange>,
    val preservedPerformedSetIds: List<String>,
    val reasonCode: ReasonCode,
    val tradeoffCode: String,
    val factReferences: List<String>
)

@Serializable
enum class ReasonCode {
    @SerialName("time_constraint") TIME_CONSTRAINT,
    @SerialName("equipment_constraint") EQUIPMENT_CONSTRAINT,
    @SerialName("combined_confirmed_constraints") COMBINED_CONFIRMED_CONSTRAINTS
}

@Serializable
enum class ChangeKind {
    @SerialName("reduce_unperformed") REDUCE_UNPERFORMED,
    @SerialName("omit_unperformed") OMIT_UNPERFORMED,
    @SerialName("replace_unperformed") REPLACE_UNPERFORMED
}

@Serializable
data class ProposalChange(
    val kind: ChangeKind,
    val before: ExerciseSnapshot,
    val after: ExerciseSnapshot?
)

@Serializable
data class ExerciseSnapshot(
    val exerciseInstanceId: String,
    val catalogKey: String,
    val sets: List<SetSnapshot>
)

@Serializable
data class SetSnapshot(
    val setId: String,
    val targetReps: Int?,
    val targetWeightKg: Float?,
    val targetSeconds: Int?,
    val restSeconds: Int?
)

object ProposalJson {
    val format = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
        encodeDefaults = true
    }

    fun encode(proposal: AdjustmentProposal): String = format.encodeToString(proposal)

    fun decode(json: String): AdjustmentProposal = format.decodeFromString(json)
}
