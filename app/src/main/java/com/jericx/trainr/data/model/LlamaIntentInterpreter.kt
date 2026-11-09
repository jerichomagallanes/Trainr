package com.jericx.trainr.data.model

import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.domain.unstuck.intent.FailureKind
import com.jericx.trainr.domain.unstuck.intent.IntentGrammar
import com.jericx.trainr.domain.unstuck.intent.IntentInterpreter
import com.jericx.trainr.domain.unstuck.intent.IntentPrompt
import com.jericx.trainr.domain.unstuck.intent.IntentValidator
import com.jericx.trainr.domain.unstuck.intent.InterpreterAvailability
import com.jericx.trainr.domain.unstuck.intent.InterpreterResult
import com.jericx.trainr.domain.unstuck.intent.LocalModelInstaller
import com.jericx.trainr.domain.unstuck.intent.ModelState
import com.jericx.trainr.domain.unstuck.intent.NoteText
import com.jericx.trainr.llama.CompletionResult
import com.jericx.trainr.llama.LocalModel
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class LlamaIntentInterpreter(
    private val installer: LocalModelInstaller,
    private val eligibility: DeviceEligibility,
    private val model: LocalModel
) : IntentInterpreter {

    override val availability: InterpreterAvailability
        get() = when {
            !eligibility.isSupported -> InterpreterAvailability.UNSUPPORTED_DEVICE
            installer.state.value == ModelState.Ready -> InterpreterAvailability.READY
            else -> InterpreterAvailability.NOT_INSTALLED
        }

    override suspend fun interpret(
        text: String,
        locale: Locale,
        directReason: DirectReason?
    ): InterpreterResult {
        if (directReason == DirectReason.PAIN) return InterpreterResult.Unavailable
        if (availability != InterpreterAvailability.READY) return InterpreterResult.Unavailable
        if (!NoteText.isWithinLimit(text)) {
            return InterpreterResult.Failed(FailureKind.NOTE_TOO_LONG)
        }
        val grammar = IntentGrammar.forNote(text) ?: return InterpreterResult.Unavailable

        val result = model.complete(
            system = IntentPrompt.SYSTEM_INSTRUCTION,
            user = text,
            grammar = grammar,
            maxTokens = IntentGrammar.MAX_TOKENS,
            timeout = TIMEOUT
        )
        return when (result) {
            is CompletionResult.Text ->
                InterpreterResult.Interpreted(IntentValidator.validate(result.text, text))

            CompletionResult.Timeout -> InterpreterResult.Failed(FailureKind.TIMEOUT)
            CompletionResult.TooLong -> InterpreterResult.Failed(FailureKind.NOTE_TOO_LONG)
            CompletionResult.Cancelled -> InterpreterResult.Failed(FailureKind.CANCELLED)
            CompletionResult.OutOfMemory -> InterpreterResult.Failed(FailureKind.OUT_OF_MEMORY)
            CompletionResult.Failed -> InterpreterResult.Failed(FailureKind.RUNTIME)
        }
    }

    private companion object {
        // Matched to iOS: the same note must not be a read on one platform and
        // a timeout on the other.
        val TIMEOUT = 120.seconds
    }
}
