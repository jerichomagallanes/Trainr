package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

internal fun handoffFixture(name: String): JsonObject {
    val file = File("../docs/unstuck-handoff/fixtures/$name")
    assertWithMessage("handoff fixture missing at ${file.absolutePath}").that(file.exists()).isTrue()
    return Json.parseToJsonElement(file.readText()).jsonObject
}

internal fun extraction(
    schemaVersion: String = "1.0",
    intent: IntentKind = IntentKind.OTHER_OR_UNCLEAR,
    timeBudget: TimeBudgetMention? = null,
    equipmentMention: String? = null,
    concern: Concern = Concern.NONE_STATED,
    memoryCandidate: Boolean = false,
    clarification: Clarification = Clarification.NONE,
    evidence: List<Evidence> = emptyList()
) = IntentExtraction(
    schemaVersion = schemaVersion,
    intent = intent,
    timeBudget = timeBudget,
    equipmentMention = equipmentMention,
    concern = concern,
    memoryCandidate = memoryCandidate,
    clarification = clarification,
    evidence = evidence
)

internal fun IntentExtraction.asJson(): String = IntentJson.format.encodeToString(this)

internal fun validated(extraction: IntentExtraction, input: String): IntentValidation.Valid {
    val validation = IntentValidator.validate(extraction.asJson(), input)
    assertWithMessage("expected a valid extraction, got $validation")
        .that(validation).isInstanceOf(IntentValidation.Valid::class.java)
    return validation as IntentValidation.Valid
}

internal fun rejection(validation: IntentValidation): List<RejectionReason> {
    assertWithMessage("expected a rejection, got $validation")
        .that(validation).isInstanceOf(IntentValidation.Rejected::class.java)
    return (validation as IntentValidation.Rejected).reasons
}
