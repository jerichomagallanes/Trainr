package com.jericx.trainr.data.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.domain.unstuck.intent.FailureKind
import com.jericx.trainr.domain.unstuck.intent.IntentGrammar
import com.jericx.trainr.domain.unstuck.intent.IntentKind
import com.jericx.trainr.domain.unstuck.intent.IntentPrompt
import com.jericx.trainr.domain.unstuck.intent.IntentValidation
import com.jericx.trainr.domain.unstuck.intent.InterpreterAvailability
import com.jericx.trainr.domain.unstuck.intent.InterpreterResult
import com.jericx.trainr.domain.unstuck.intent.LocalModelInstaller
import com.jericx.trainr.domain.unstuck.intent.ModelState
import com.jericx.trainr.domain.unstuck.intent.RejectionReason
import com.jericx.trainr.llama.CompletionResult
import com.jericx.trainr.llama.LocalModel
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LlamaIntentInterpreterTest {

    private val note = "I have 35 minutes for the whole workout today."

    private val validAnswer = """{"schemaVersion":"1.1","intent":"less_time","timeBudget":{"minutes":35,"scope":"whole_session"},"equipmentMention":null,"concern":"none_stated","memoryCandidate":false,"clarification":"none","evidence":[{"field":"time_budget","quote":"35 minutes for the whole workout"}]}"""

    private class FakeInstaller(state: ModelState) : LocalModelInstaller {
        override val state = MutableStateFlow(state)
        override fun install() = Unit
        override fun cancel() = Unit
    }

    private class FakeModel(private val answer: CompletionResult) : LocalModel {
        var calls = 0
        var lastSystem: String? = null
        var lastGrammar: String? = null
        var lastMaxTokens: Int? = null
        var lastTimeout: Duration? = null

        override suspend fun complete(
            system: String,
            user: String,
            grammar: String,
            maxTokens: Int,
            timeout: Duration
        ): CompletionResult {
            calls++
            lastSystem = system
            lastGrammar = grammar
            lastMaxTokens = maxTokens
            lastTimeout = timeout
            return answer
        }
    }

    private val supported = DeviceEligibility(4L shl 30, isLowRamDevice = false, primaryAbi = "arm64-v8a")
    private val unsupported = DeviceEligibility(4L shl 30, isLowRamDevice = true, primaryAbi = "arm64-v8a")

    private fun interpreter(
        state: ModelState = ModelState.Ready,
        eligibility: DeviceEligibility = supported,
        model: FakeModel = FakeModel(CompletionResult.Text(validAnswer))
    ) = LlamaIntentInterpreter(FakeInstaller(state), eligibility, model)

    @Test
    fun availabilityFollowsTheDeviceAndTheInstall() {
        assertThat(interpreter(eligibility = unsupported).availability)
            .isEqualTo(InterpreterAvailability.UNSUPPORTED_DEVICE)
        assertThat(interpreter(state = ModelState.NotInstalled).availability)
            .isEqualTo(InterpreterAvailability.NOT_INSTALLED)
        assertThat(interpreter(state = ModelState.Downloading(1, 2)).availability)
            .isEqualTo(InterpreterAvailability.NOT_INSTALLED)
        assertThat(interpreter().availability).isEqualTo(InterpreterAvailability.READY)
    }

    @Test
    fun aDirectPainReasonNeverReachesTheModel() = runTest {
        val model = FakeModel(CompletionResult.Text(validAnswer))

        val result = interpreter(model = model).interpret(note, Locale.ENGLISH, DirectReason.PAIN)

        assertThat(result).isEqualTo(InterpreterResult.Unavailable)
        assertThat(model.calls).isEqualTo(0)
    }

    @Test
    fun anUninstalledModelIsNotAsked() = runTest {
        val model = FakeModel(CompletionResult.Text(validAnswer))

        val result = interpreter(state = ModelState.NotInstalled, model = model)
            .interpret(note, Locale.ENGLISH, DirectReason.OTHER)

        assertThat(result).isEqualTo(InterpreterResult.Unavailable)
        assertThat(model.calls).isEqualTo(0)
    }

    @Test
    fun aNoteWithNoQuotableWordIsNotAsked() = runTest {
        val model = FakeModel(CompletionResult.Text(validAnswer))

        val result = interpreter(model = model).interpret("\"\"", Locale.ENGLISH, DirectReason.OTHER)

        assertThat(result).isEqualTo(InterpreterResult.Unavailable)
        assertThat(model.calls).isEqualTo(0)
    }

    @Test
    fun theRawTextGoesThroughTheValidator() = runTest {
        val model = FakeModel(CompletionResult.Text(validAnswer))

        val result = interpreter(model = model).interpret(note, Locale.ENGLISH, DirectReason.OTHER)

        val valid = (result as InterpreterResult.Interpreted).validation as IntentValidation.Valid
        assertThat(valid.extraction.intent).isEqualTo(IntentKind.LESS_TIME)
        assertThat(valid.actionable.minutes).isEqualTo(35)
        assertThat(model.lastSystem).isEqualTo(IntentPrompt.SYSTEM_INSTRUCTION)
        assertThat(model.lastGrammar).isEqualTo(IntentGrammar.forNote(note))
        assertThat(model.lastMaxTokens).isEqualTo(IntentGrammar.MAX_TOKENS)
        assertThat(model.lastTimeout).isEqualTo(25.seconds)
    }

    @Test
    fun anAnswerTheValidatorRefusesIsStillAnInterpretation() = runTest {
        val model = FakeModel(CompletionResult.Text("""{"schemaVersion":"2.0"}"""))

        val result = interpreter(model = model).interpret(note, Locale.ENGLISH, DirectReason.OTHER)

        val rejected = (result as InterpreterResult.Interpreted).validation as IntentValidation.Rejected
        assertThat(rejected.reasons).isNotEmpty()
        assertThat(rejected.reasons.first()).isIn(
            listOf(RejectionReason.UNKNOWN_KEY_OR_ENUM, RejectionReason.MALFORMED_JSON)
        )
    }

    @Test
    fun everyEngineFailureIsNamed() = runTest {
        suspend fun outcome(answer: CompletionResult) =
            interpreter(model = FakeModel(answer)).interpret(note, Locale.ENGLISH, DirectReason.OTHER)

        assertThat(outcome(CompletionResult.Timeout))
            .isEqualTo(InterpreterResult.Failed(FailureKind.TIMEOUT))
        assertThat(outcome(CompletionResult.Cancelled))
            .isEqualTo(InterpreterResult.Failed(FailureKind.CANCELLED))
        assertThat(outcome(CompletionResult.OutOfMemory))
            .isEqualTo(InterpreterResult.Failed(FailureKind.OUT_OF_MEMORY))
        assertThat(outcome(CompletionResult.Failed))
            .isEqualTo(InterpreterResult.Failed(FailureKind.RUNTIME))
    }
}
