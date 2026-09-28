package com.jericx.trainr.data.model

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.unstuck.intent.IntentGrammar
import com.jericx.trainr.domain.unstuck.intent.IntentKind
import com.jericx.trainr.domain.unstuck.intent.IntentPrompt
import com.jericx.trainr.domain.unstuck.intent.IntentValidation
import com.jericx.trainr.domain.unstuck.intent.IntentValidator
import com.jericx.trainr.domain.unstuck.intent.MentionScope
import com.jericx.trainr.llama.CompletionResult
import com.jericx.trainr.llama.LlamaEngine
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LlamaSmokeTest {

    @Test
    fun theModelReadsAShortNoteUnderTheGrammar() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = File(context.getExternalFilesDir("models"), ModelArtifact.FILE_NAME)
        assumeTrue("no model at ${model.path}", model.exists())

        val engine = LlamaEngine(
            modelFile = { model },
            nativeLibraryDir = context.applicationInfo.nativeLibraryDir,
            availableMemory = { Long.MAX_VALUE },
            dispatcher = Dispatchers.IO
        )
        val note = "I have 35 minutes for the whole workout today."

        val started = System.nanoTime()
        val result = runBlocking {
            engine.complete(
                system = IntentPrompt.SYSTEM_INSTRUCTION,
                user = note,
                grammar = requireNotNull(IntentGrammar.forNote(note)),
                maxTokens = IntentGrammar.MAX_TOKENS,
                timeout = 120.seconds
            )
        }
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        Log.i("LlamaSmokeTest", "completion took $elapsedMillis ms: $result")

        val text = (result as CompletionResult.Text).text
        val valid = IntentValidator.validate(text, note) as IntentValidation.Valid
        assertThat(valid.extraction.intent).isEqualTo(IntentKind.LESS_TIME)
        assertThat(valid.actionable.minutes).isEqualTo(35)
        assertThat(valid.actionable.scope).isEqualTo(MentionScope.WHOLE_SESSION)
    }
}
