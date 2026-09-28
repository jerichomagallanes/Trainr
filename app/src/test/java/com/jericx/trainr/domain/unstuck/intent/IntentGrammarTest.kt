package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.serializer
import org.junit.Test

class IntentGrammarTest {

    private val grammar = IntentGrammar.forNote("I have 35 minutes.")

    @Test
    fun theGrammarNamesEveryPropertyOfTheContract() {
        assertQuoted(serializer<IntentExtraction>().descriptor)
        assertQuoted(serializer<Evidence>().descriptor)
        assertQuoted(serializer<TimeBudgetMention>().descriptor)
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
        val rules = grammar.trim().lines()

        assertThat(rules.first()).startsWith("root ::= ")
        rules.forEach { assertThat(it).contains(" ::= ") }
        assertThat(IntentGrammar.MAX_TOKENS).isEqualTo(320)
    }

    private fun assertQuoted(descriptor: SerialDescriptor) {
        descriptor.elementNames.forEach { name ->
            assertWithMessage("grammar names $name from ${descriptor.serialName}")
                .that(grammar).contains("\"\\\"$name\\\"\"")
        }
    }
}
