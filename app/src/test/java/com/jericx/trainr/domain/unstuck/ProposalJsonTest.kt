package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class ProposalJsonTest {

    private val fixture = File("../docs/unstuck-handoff/fixtures/proposal-example.json")

    private fun fixtureText(): String {
        assertWithMessage("fixture missing at ${fixture.absolutePath}").that(fixture.exists()).isTrue()
        return fixture.readText()
    }

    @Test
    fun theHandoffFixtureDecodesAndComesBackAsTheSameProposal() {
        val decoded = ProposalJson.decode(fixtureText())

        assertThat(decoded.proposalId).isEqualTo("fixture-proposal")
        assertThat(decoded.reasonCode).isEqualTo(ReasonCode.TIME_CONSTRAINT)
        assertThat(decoded.changes.single().kind).isEqualTo(ChangeKind.REDUCE_UNPERFORMED)
        assertThat(decoded.changes.single().after!!.sets).hasSize(1)
        assertThat(ProposalJson.decode(ProposalJson.encode(decoded))).isEqualTo(decoded)
    }

    @Test
    fun encodingWritesEveryFieldTheContractNamesAndNothingElse() {
        val decoded = ProposalJson.decode(fixtureText())

        val written = Json.parseToJsonElement(ProposalJson.encode(decoded))

        assertThat(written).isEqualTo(Json.parseToJsonElement(fixtureText()))
    }

    @Test
    fun anUnknownKeyIsRefused() {
        val original = Json.parseToJsonElement(fixtureText()) as JsonObject
        val extended = Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(original + ("note" to JsonPrimitive("smuggled")))
        )

        assertThrows(SerializationException::class.java) { ProposalJson.decode(extended) }
    }

    @Test
    fun anOmissionWithNoAfterSurvivesTheRoundTrip() {
        val omission = ProposalJson.decode(fixtureText()).let { proposal ->
            proposal.copy(
                changes = listOf(
                    proposal.changes.single().copy(kind = ChangeKind.OMIT_UNPERFORMED, after = null)
                )
            )
        }

        val json = ProposalJson.encode(omission)

        assertThat(json).contains("\"after\":null")
        assertThat(ProposalJson.decode(json)).isEqualTo(omission)
    }
}
