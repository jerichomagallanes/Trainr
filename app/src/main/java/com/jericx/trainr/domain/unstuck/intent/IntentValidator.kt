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
    EVIDENCE_QUOTE_MISMATCH,
    FACT_WITHOUT_EVIDENCE,
    MINUTES_NOT_IN_QUOTE
}

data class ActionableFacts(
    val minutes: Int?,
    val scope: MentionScope?,
    val equipmentMention: String?,
    val memoryCandidate: Boolean,
    val painConcern: Boolean
)

object IntentValidator {

    private const val SCHEMA_VERSION = "1.1"
    private const val MINUTES_RANGE_END = 1440
    private const val MAX_EQUIPMENT_MENTION_CODE_POINTS = 160
    private const val MAX_EVIDENCE_ENTRIES = 8
    private const val MAX_QUOTE_CODE_POINTS = 500
    private val NUMBER_WORDS = setOf(
        "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
        "twenty", "thirty", "forty", "fifty", "sixty", "ninety", "half", "hour", "hours"
    )

    fun validate(rawJson: String, input: String): IntentValidation {
        // A key answered twice decodes to one of its two answers and the
        // duplicate is gone before any check can see it, so the document is
        // refused rather than read.
        if (RepeatedKeys.present(rawJson)) {
            return IntentValidation.Rejected(listOf(RejectionReason.MALFORMED_JSON))
        }
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

        reasons += quoteFailures(extraction.evidence, normalized)
        if (factsWithoutEvidence(extraction)) reasons += RejectionReason.FACT_WITHOUT_EVIDENCE
        if (minutesNotQuoted(extraction)) reasons += RejectionReason.MINUTES_NOT_IN_QUOTE

        return if (reasons.isEmpty()) {
            IntentValidation.Valid(extraction, actionableFacts(extraction))
        } else {
            IntentValidation.Rejected(reasons.toList())
        }
    }

    private fun quoteFailures(evidence: List<Evidence>, normalized: String): Set<RejectionReason> {
        val failures = linkedSetOf<RejectionReason>()
        for (entry in evidence) {
            val quote = Normalizer.normalize(entry.quote, Normalizer.Form.NFC)
            if (quote.codePointLength() !in 1..MAX_QUOTE_CODE_POINTS) {
                failures += RejectionReason.EVIDENCE_QUOTE_LENGTH
            } else if (quote !in normalized) {
                failures += RejectionReason.EVIDENCE_QUOTE_MISMATCH
            }
        }
        return failures
    }

    private fun minutesNotQuoted(extraction: IntentExtraction): Boolean {
        val minutes = extraction.timeBudget?.minutes ?: return false
        val quotes = extraction.evidence.filter { it.field == EvidenceField.TIME_BUDGET }.map { it.quote }
        if (quotes.isEmpty()) return false
        val digits = minutes.toString()
        return quotes.none { quote -> runs(quote).any { it == digits || it in NUMBER_WORDS } }
    }

    // Whole runs only, so "30 minutes" never licenses 3 and "often" never licenses ten.
    private fun runs(quote: String): List<String> {
        val runs = mutableListOf<String>()
        val current = StringBuilder()
        var digits = false
        for (char in quote.lowercase()) {
            val usable = char.isLetter() || char.isDigit()
            if (!usable || (current.isNotEmpty() && char.isDigit() != digits)) {
                if (current.isNotEmpty()) runs += current.toString()
                current.clear()
            }
            if (usable) {
                digits = char.isDigit()
                current.append(char)
            }
        }
        if (current.isNotEmpty()) runs += current.toString()
        return runs
    }

    private fun factsWithoutEvidence(extraction: IntentExtraction): Boolean {
        val cited = extraction.evidence.mapTo(mutableSetOf()) { it.field }
        return (extraction.timeBudget != null && EvidenceField.TIME_BUDGET !in cited) ||
            (extraction.equipmentMention != null && EvidenceField.EQUIPMENT_MENTION !in cited) ||
            (extraction.memoryCandidate && EvidenceField.MEMORY_CANDIDATE !in cited) ||
            (extraction.concern == Concern.PAIN_OR_UNCLEAR_DISCOMFORT && EvidenceField.CONCERN !in cited)
    }

    private fun actionableFacts(extraction: IntentExtraction): ActionableFacts {
        val budget = extraction.timeBudget
        return ActionableFacts(
            minutes = budget?.minutes,
            scope = budget?.scope?.takeIf { it != MentionScope.UNKNOWN },
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

    // The contract's limits count Unicode code points, not UTF-16 chars.
    private fun String.codePointLength(): Int = codePointCount(0, length)
}

private object RepeatedKeys {

    fun present(json: String): Boolean {
        val frames = ArrayDeque<MutableSet<String>?>()
        var index = 0
        while (index < json.length) {
            when (json[index]) {
                '{' -> frames.addLast(mutableSetOf())
                '[' -> frames.addLast(null)
                '}', ']' -> frames.removeLastOrNull()
                '"' -> {
                    val token = StringBuilder()
                    index = readString(json, index, token)
                    val keys = frames.lastOrNull()
                    if (keys != null && nextMeaningful(json, index) == ':' &&
                        !keys.add(token.toString())
                    ) {
                        return true
                    }
                    continue
                }
            }
            index++
        }
        return false
    }

    // Returns the index just past the closing quote, unescaping as it goes so
    // a key spelled with \u escapes cannot pass as a different key.
    private fun readString(json: String, start: Int, into: StringBuilder): Int {
        var index = start + 1
        while (index < json.length) {
            when (val char = json[index]) {
                '\\' -> {
                    index++
                    if (index >= json.length) return index
                    when (val escape = json[index]) {
                        'u' -> {
                            val hex = json.substring(index + 1, minOf(index + 5, json.length))
                            hex.toIntOrNull(16)?.let { into.append(it.toChar()) }
                            index += 4
                        }
                        'n' -> into.append('\n')
                        't' -> into.append('\t')
                        'r' -> into.append('\r')
                        'b' -> into.append('\b')
                        else -> into.append(escape)
                    }
                }
                '"' -> return index + 1
                else -> into.append(char)
            }
            index++
        }
        return index
    }

    private fun nextMeaningful(json: String, from: Int): Char? {
        var index = from
        while (index < json.length && json[index].isWhitespace()) index++
        return json.getOrNull(index)
    }
}
