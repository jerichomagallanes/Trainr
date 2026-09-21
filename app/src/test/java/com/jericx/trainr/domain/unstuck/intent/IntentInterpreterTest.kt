package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale

class IntentInterpreterTest {

    @Test
    fun anUnavailableInterpreterAnswersUnavailable() = runTest {
        assertThat(UnavailableInterpreter.availability).isEqualTo(InterpreterAvailability.NOT_INSTALLED)

        val result = UnavailableInterpreter.interpret(
            text = "I have 35 minutes.",
            locale = Locale.ENGLISH,
            directReason = DirectReason.LESS_TIME
        )

        assertThat(result).isEqualTo(InterpreterResult.Unavailable)
    }
}
