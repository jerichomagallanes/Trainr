package com.jericx.trainr.domain.unstuck.intent

import java.text.Normalizer

object IntentGrammar {

    const val MAX_TOKENS = 256

    private const val TOKEN_LIMIT = 200
    private const val TOKEN_SLOT = "{TOKENS}"
    private val WHITESPACE = Regex("[\\s\\p{Z}]+")

    private const val TEMPLATE: String = """root ::= "{" ws "\"schemaVersion\"" ws ":" ws "\"1.1\"" ws "," ws "\"intent\"" ws ":" ws intent ws "," ws "\"concern\"" ws ":" ws concern ws "," ws "\"clarification\"" ws ":" ws clar ws "," ws "\"evidence\"" ws ":" ws evlist ws "," ws "\"timeBudget\"" ws ":" ws budget ws "," ws "\"equipmentMention\"" ws ":" ws strornull ws "," ws "\"memoryCandidate\"" ws ":" ws bool ws "}"
intent ::= "\"less_time\"" | "\"equipment_unavailable\"" | "\"exercise_guidance\"" | "\"pain_concern\"" | "\"other_or_unclear\""
concern ::= "\"none_stated\"" | "\"pain_or_unclear_discomfort\""
clar ::= "\"none\"" | "\"duration\"" | "\"duration_scope\"" | "\"affected_exercise\"" | "\"available_equipment\"" | "\"primary_constraint\"" | "\"meaning\""
budget ::= "null" | "{" ws "\"minutes\"" ws ":" ws int ws "," ws "\"scope\"" ws ":" ws scope ws "}"
scope ::= "\"whole_session\"" | "\"remaining\"" | "\"unknown\""
evlist ::= "[" ws "]" | "[" ws ev evtail ws "]"
evtail ::= "" | ws "," ws ev evtail
ev ::= "{" ws "\"field\"" ws ":" ws field ws "," ws "\"quote\"" ws ":" ws quote ws "}"
field ::= "\"time_budget\"" | "\"equipment_mention\"" | "\"concern\"" | "\"memory_candidate\"" | "\"intent\""
quote ::= "\"" token (" " token)* "\""
token ::= {TOKENS}
strornull ::= "null" | quote
bool ::= "true" | "false"
int ::= [1-9] [0-9]? [0-9]? [0-9]?
ws ::= [ ]*"""

    fun forNote(note: String): String? {
        val tokens = tokens(note)
        if (tokens.isEmpty()) return null
        val alternatives = tokens.joinToString(" | ") { "\"$it\"" }
        return TEMPLATE.replace(TOKEN_SLOT, alternatives)
    }

    private fun tokens(note: String): List<String> =
        Normalizer.normalize(note, Normalizer.Form.NFC)
            .split(WHITESPACE)
            .filter { it.isNotEmpty() && '"' !in it && '\\' !in it }
            .distinct()
            .take(TOKEN_LIMIT)
}
