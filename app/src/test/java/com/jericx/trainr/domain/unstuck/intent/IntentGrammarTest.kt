package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.serializer
import org.junit.Test

class IntentGrammarTest {

    private val note = "I have 35 minutes for the whole workout today."
    private val grammar = requireNotNull(IntentGrammar.forNote(note))

    @Test
    fun theNotesWordsAreTheOnlyQuotableTokens() {
        assertThat(grammar).contains(
            "token ::= \"I\" | \"have\" | \"35\" | \"minutes\" | \"for\" | \"the\" | \"whole\" | \"workout\" | \"today.\""
        )
    }

    @Test
    fun aRepeatedWordAppearsOnce() {
        assertThat(tokenRule("30 minutes, only 30 minutes")).isEqualTo("token ::= \"30\" | \"minutes,\" | \"only\" | \"minutes\"")
    }

    @Test
    fun aWordWithAQuoteOrABackslashIsDropped() {
        assertThat(tokenRule("say \"hi\" back\\slash now")).isEqualTo("token ::= \"say\" | \"now\"")
    }

    @Test
    fun aNoteWithNothingQuotableHasNoGrammar() {
        assertThat(IntentGrammar.forNote("\"")).isNull()
        assertThat(IntentGrammar.forNote("  ")).isNull()
    }

    @Test
    fun theTokensAreCutAtTwoHundred() {
        val words = (1..201).map { "w$it" }

        val rule = tokenRule(words.joinToString(" "))

        assertThat(rule).contains("\"w200\"")
        assertThat(rule).doesNotContain("\"w201\"")
    }

    @Test
    fun aJapaneseNoteIsOneToken() {
        assertThat(tokenRule("今日は30分しかありません。")).isEqualTo("token ::= \"今日は30分しかありません。\"")
    }

    @Test
    fun theGrammarNamesEveryPropertyOfTheContract() {
        assertQuoted(serializer<IntentExtraction>().descriptor)
        assertQuoted(serializer<Evidence>().descriptor)
        assertQuoted(serializer<TimeBudgetMention>().descriptor)
        assertThat(grammar).contains("\"\\\"1.1\\\"\"")
    }

    @Test
    fun theGrammarNamesEveryEnumValue() {
        assertQuoted(serializer<IntentKind>().descriptor)
        assertQuoted(serializer<MentionScope>().descriptor)
        assertQuoted(serializer<Concern>().descriptor)
        assertQuoted(serializer<Clarification>().descriptor)
        assertQuoted(serializer<EvidenceField>().descriptor)
    }

    @Test
    fun theGrammarIsOneRulePerLineWithARoot() {
        val rules = grammar.lines()

        assertThat(rules.first()).startsWith("root ::= ")
        rules.forEach { assertThat(it.split(" ::= ")).hasSize(2) }
        assertThat(IntentGrammar.MAX_TOKENS).isEqualTo(256)
    }

    private fun tokenRule(note: String): String =
        requireNotNull(IntentGrammar.forNote(note)).lines().single { it.startsWith("token ::= ") }

    private fun assertQuoted(descriptor: SerialDescriptor) {
        descriptor.elementNames.forEach { name ->
            assertWithMessage("grammar names $name from ${descriptor.serialName}")
                .that(grammar).contains("\"\\\"$name\\\"\"")
        }
    }
}
