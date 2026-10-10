package com.jericx.trainr.domain.unstuck.intent

import java.util.Locale

interface IntentInterpreter {
    val availability: InterpreterAvailability

    suspend fun interpret(text: String, locale: Locale, directReason: DirectReason?): InterpreterResult
}

enum class InterpreterAvailability { NOT_INSTALLED, UNSUPPORTED_DEVICE, READY }

sealed interface InterpreterResult {

    data object Unavailable : InterpreterResult

    data class Interpreted(val validation: IntentValidation) : InterpreterResult

    data class Failed(val kind: FailureKind) : InterpreterResult
}

enum class FailureKind { TIMEOUT, OUT_OF_MEMORY, CANCELLED, RUNTIME }

object UnavailableInterpreter : IntentInterpreter {

    override val availability = InterpreterAvailability.NOT_INSTALLED

    override suspend fun interpret(
        text: String,
        locale: Locale,
        directReason: DirectReason?
    ): InterpreterResult = InterpreterResult.Unavailable
}
