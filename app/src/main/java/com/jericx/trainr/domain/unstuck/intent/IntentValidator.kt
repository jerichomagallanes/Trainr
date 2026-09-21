package com.jericx.trainr.domain.unstuck.intent

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonDecodingException
import java.text.Normalizer

sealed interface IntentValidation {

    data class Valid(
        val extraction: IntentExtraction,
        val actionable: ActionableFacts
    ) : IntentValidation

    data class Rejected(val reasons: List<RejectionReason>) : IntentValidation
}

enum class RejectionReason {
    MALFORMED_JSON,
    UNKNOWN_KEY_OR_ENUM,
    WRONG_SCHEMA_VERSION,
    MINUTES_OUT_OF_RANGE,
    EQUIPMENT_MENTION_TOO_LONG,
    TOO_MUCH_EVIDENCE,
    EVIDENCE_QUOTE_LENGTH,
    EVIDENCE_SPAN_INVALID,
    EVIDENCE_QUOTE_MISMATCH,
    FACT_WITHOUT_EVIDENCE
}

data class ActionableFacts(
    val minutes: Int?,
    val scope: MentionScope?,
    val equipmentMention: String?,
    val memoryCandidate: Boolean,
    val painConcern: Boolean
)

object IntentValidator {

    private const val SCHEMA_VERSION = "1.0"
    private const val MINUTES_RANGE_END = 1440
    private const val MAX_EQUIPMENT_MENTION_CODE_POINTS = 160
    private const val MAX_EVIDENCE_ENTRIES = 8
    private const val MAX_QUOTE_CODE_POINTS = 500

    fun validate(rawJson: String, input: String): IntentValidation {
        val extraction = try {
            IntentJson.format.decodeFromString<IntentExtraction>(rawJson)
        } catch (error: Exception) {
            return IntentValidation.Rejected(listOf(decodeFailure(error)))
        }

        val normalized = Normalizer.normalize(input, Normalizer.Form.NFC)
        val reasons = linkedSetOf<RejectionReason>()

        if (extraction.schemaVersion != SCHEMA_VERSION) {
            reasons += RejectionReason.WRONG_SCHEMA_VERSION
        }
        extraction.timeBudget?.let { budget ->
            if (budget.minutes !in 1..MINUTES_RANGE_END) reasons += RejectionReason.MINUTES_OUT_OF_RANGE
        }
        extraction.equipmentMention?.let { mention ->
            if (mention.codePointLength() > MAX_EQUIPMENT_MENTION_CODE_POINTS) {
                reasons += RejectionReason.EQUIPMENT_MENTION_TOO_LONG
            }
        }
        if (extraction.evidence.size > MAX_EVIDENCE_ENTRIES) reasons += RejectionReason.TOO_MUCH_EVIDENCE

        reasons += spanFailures(extraction.evidence, normalized)
        if (factsWithoutEvidence(extraction)) reasons += RejectionReason.FACT_WITHOUT_EVIDENCE

        return if (reasons.isEmpty()) {
            IntentValidation.Valid(extraction, actionableFacts(extraction))
        } else {
            IntentValidation.Rejected(reasons.toList())
        }
    }

    private fun spanFailures(evidence: List<Evidence>, normalized: String): Set<RejectionReason> {
        val inputLength = normalized.codePointLength()
        val failures = linkedSetOf<RejectionReason>()
        for (entry in evidence) {
            if (entry.quote.codePointLength() !in 1..MAX_QUOTE_CODE_POINTS) {
                failures += RejectionReason.EVIDENCE_QUOTE_LENGTH
            }
            if (entry.start < 0 || entry.end <= entry.start || entry.end > inputLength) {
                failures += RejectionReason.EVIDENCE_SPAN_INVALID
            } else if (Normalizer.normalize(entry.quote, Normalizer.Form.NFC) !=
                normalized.codePointSlice(entry.start, entry.end)
            ) {
                failures += RejectionReason.EVIDENCE_QUOTE_MISMATCH
            }
        }
        return failures
    }

    private fun factsWithoutEvidence(extraction: IntentExtraction): Boolean {
        val cited = extraction.evidence.mapTo(mutableSetOf()) { it.field }
        return (extraction.timeBudget != null && EvidenceField.TIME_BUDGET !in cited) ||
            (extraction.equipmentMention != null && EvidenceField.EQUIPMENT_MENTION !in cited) ||
            (extraction.memoryCandidate && EvidenceField.MEMORY_CANDIDATE !in cited) ||
            (extraction.concern == Concern.PAIN_OR_UNCLEAR_DISCOMFORT && EvidenceField.CONCERN !in cited)
    }

    private fun actionableFacts(extraction: IntentExtraction): ActionableFacts {
        val budget = extraction.timeBudget?.takeIf { it.scope != MentionScope.UNKNOWN }
        return ActionableFacts(
            minutes = budget?.minutes,
            scope = budget?.scope,
            equipmentMention = extraction.equipmentMention,
            memoryCandidate = extraction.memoryCandidate,
            painConcern = extraction.concern == Concern.PAIN_OR_UNCLEAR_DISCOMFORT
        )
    }

    private fun decodeFailure(error: Exception): RejectionReason {
        // JsonDecodingException.message echoes the model's own bytes back; shortMessage does not.
        val message = (error as? JsonDecodingException)?.shortMessage ?: error.message
            ?: return if (error is SerializationException) {
                RejectionReason.UNKNOWN_KEY_OR_ENUM
            } else {
                RejectionReason.MALFORMED_JSON
            }
        return if (message.contains("unknown key", ignoreCase = true) ||
            message.contains("does not contain element")
        ) {
            RejectionReason.UNKNOWN_KEY_OR_ENUM
        } else {
            RejectionReason.MALFORMED_JSON
        }
    }

    // The contract counts Unicode code points, so UTF-16 char indices would
    // quietly accept a shifted span in any note containing an emoji.
    private fun String.codePointLength(): Int = codePointCount(0, length)

    private fun String.codePointSlice(start: Int, end: Int): String =
        substring(offsetByCodePoints(0, start), offsetByCodePoints(0, end))
}
