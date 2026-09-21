package com.jericx.trainr.domain.unstuck.intent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class IntentExtraction(
    val schemaVersion: String,
    val intent: IntentKind,
    val timeBudget: TimeBudgetMention?,
    val equipmentMention: String?,
    val concern: Concern,
    val memoryCandidate: Boolean,
    val clarification: Clarification,
    val evidence: List<Evidence>
)

@Serializable
enum class IntentKind {
    @SerialName("less_time") LESS_TIME,
    @SerialName("equipment_unavailable") EQUIPMENT_UNAVAILABLE,
    @SerialName("exercise_guidance") EXERCISE_GUIDANCE,
    @SerialName("pain_concern") PAIN_CONCERN,
    @SerialName("other_or_unclear") OTHER_OR_UNCLEAR
}

@Serializable
data class TimeBudgetMention(val minutes: Int, val scope: MentionScope)

@Serializable
enum class MentionScope {
    @SerialName("whole_session") WHOLE_SESSION,
    @SerialName("remaining") REMAINING,
    @SerialName("unknown") UNKNOWN
}

@Serializable
enum class Concern {
    @SerialName("none_stated") NONE_STATED,
    @SerialName("pain_or_unclear_discomfort") PAIN_OR_UNCLEAR_DISCOMFORT
}

@Serializable
enum class Clarification {
    @SerialName("none") NONE,
    @SerialName("duration") DURATION,
    @SerialName("duration_scope") DURATION_SCOPE,
    @SerialName("affected_exercise") AFFECTED_EXERCISE,
    @SerialName("available_equipment") AVAILABLE_EQUIPMENT,
    @SerialName("primary_constraint") PRIMARY_CONSTRAINT,
    @SerialName("meaning") MEANING
}

@Serializable
data class Evidence(
    val field: EvidenceField,
    val quote: String,
    val start: Int,
    val end: Int
)

@Serializable
enum class EvidenceField {
    @SerialName("intent") INTENT,
    @SerialName("time_budget") TIME_BUDGET,
    @SerialName("equipment_mention") EQUIPMENT_MENTION,
    @SerialName("concern") CONCERN,
    @SerialName("memory_candidate") MEMORY_CANDIDATE
}

object IntentJson {
    val format = Json {
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
        explicitNulls = true
    }
}
