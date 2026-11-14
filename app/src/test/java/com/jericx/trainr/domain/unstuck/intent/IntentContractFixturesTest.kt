package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class IntentContractFixturesTest {

    @Test
    fun theValidFixturePasses() {
        val fixture = handoffFixture("intent-valid.json")
        val input = fixture.getValue("input").jsonPrimitive.content

        val validation = IntentValidator.validate(fixture.rawOutput(), input)

        val valid = validation as IntentValidation.Valid
        assertThat(valid.extraction.intent).isEqualTo(IntentKind.LESS_TIME)
        assertThat(valid.extraction.evidence.single().field).isEqualTo(EvidenceField.TIME_BUDGET)
        assertThat(valid.actionable).isEqualTo(
            ActionableFacts(
                minutes = 35,
                scope = MentionScope.WHOLE_SESSION,
                equipmentMention = null,
                memoryCandidate = false,
                painConcern = false
            )
        )
    }

    @Test
    fun theExtraKeyFixtureIsRejected() {
        val fixture = handoffFixture("intent-invalid-extra-key.json")
        val input = fixture.getValue("input").jsonPrimitive.content

        val validation = IntentValidator.validate(fixture.rawOutput(), input)

        assertThat(rejection(validation)).containsExactly(RejectionReason.UNKNOWN_KEY_OR_ENUM)
    }

    @Test
    fun everyBehaviourCaseNamesAKnownIntent() {
        val cases = handoffFixture("behavior-cases.json").getValue("cases").jsonArray
        assertThat(cases).isNotEmpty()

        cases.forEach { case ->
            val id = case.jsonObject.getValue("id").jsonPrimitive.content
            val expected = case.jsonObject.getValue("expectedIntentOrRoute").jsonPrimitive.content

            assertWithMessage("case $id expects $expected")
                .that(IntentJson.format.decodeFromString<IntentKind>("\"$expected\""))
                .isNotNull()
        }
    }

    private fun JsonObject.rawOutput(): String =
        Json.encodeToString(JsonElement.serializer(), getValue("output").jsonObject)
}
