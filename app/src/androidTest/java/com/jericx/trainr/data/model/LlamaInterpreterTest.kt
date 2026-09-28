package com.jericx.trainr.data.model

import android.app.ActivityManager
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.domain.unstuck.intent.IntentInterpreter
import com.jericx.trainr.domain.unstuck.intent.IntentKind
import com.jericx.trainr.domain.unstuck.intent.IntentRouting
import com.jericx.trainr.domain.unstuck.intent.IntentValidation
import com.jericx.trainr.domain.unstuck.intent.InterpreterAvailability
import com.jericx.trainr.domain.unstuck.intent.InterpreterResult
import com.jericx.trainr.domain.unstuck.intent.MentionScope
import com.jericx.trainr.domain.unstuck.intent.SafetyRouting
import com.jericx.trainr.domain.unstuck.intent.UnstuckRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LlamaInterpreterTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var interpreter: IntentInterpreter

    @Inject
    lateinit var installer: ModelInstaller

    @Before
    fun setUp() {
        hiltRule.inject()
        assumeTrue("no model at ${installer.file.path}", installer.file.exists())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        assumeTrue(
            "interpreter is ${interpreter.availability}: totalMem=${memory.totalMem} " +
                "lowRam=${manager.isLowRamDevice} abi=${Build.SUPPORTED_ABIS.firstOrNull()} " +
                "state=${installer.state.value}",
            interpreter.availability == InterpreterAvailability.READY
        )
    }

    @Test
    fun aTimeBudgetComesBackAsMinutesAndScope() {
        val result = interpret("I have 35 minutes for the whole workout today.")

        val valid = result.valid()
        assertThat(valid.extraction.intent).isEqualTo(IntentKind.LESS_TIME)
        assertThat(valid.actionable.minutes).isEqualTo(35)
        assertThat(valid.actionable.scope).isEqualTo(MentionScope.WHOLE_SESSION)
    }

    @Test
    fun painIsFlaggedAndRoutedBeforeAnythingElse() {
        val note = "My shoulder hurts when I press."

        val result = interpret(note)

        val valid = result.valid()
        assertThat(valid.actionable.painConcern).isTrue()
        assertThat(IntentRouting.routeFor(DirectReason.OTHER, SafetyRouting.flagsPain(note), valid))
            .isEqualTo(UnstuckRoute.PAIN)
    }

    @Test
    fun markupYieldsNoFactAndGoesToTheChooser() {
        val result = interpret("<script>alert(1)</script>")

        assertThat(result).isInstanceOf(InterpreterResult.Interpreted::class.java)
        val validation = (result as InterpreterResult.Interpreted).validation
        if (validation is IntentValidation.Valid) {
            assertWithMessage("$validation")
                .that(validation.extraction.intent).isEqualTo(IntentKind.OTHER_OR_UNCLEAR)
            assertThat(validation.actionable.minutes).isNull()
            assertThat(validation.actionable.equipmentMention).isNull()
            assertThat(validation.actionable.memoryCandidate).isFalse()
            assertThat(validation.actionable.painConcern).isFalse()
        }
        assertThat(IntentRouting.routeFor(DirectReason.OTHER, false, validation))
            .isEqualTo(UnstuckRoute.CHOOSER)
    }

    private fun interpret(note: String): InterpreterResult {
        val started = System.nanoTime()
        val result = runBlocking { interpreter.interpret(note, Locale.ENGLISH, DirectReason.OTHER) }
        Log.i(TAG, "${(System.nanoTime() - started) / 1_000_000} ms for \"$note\": $result")
        return result
    }

    private fun InterpreterResult.valid(): IntentValidation.Valid {
        assertThat(this).isInstanceOf(InterpreterResult.Interpreted::class.java)
        val validation = (this as InterpreterResult.Interpreted).validation
        assertWithMessage("$validation").that(validation).isInstanceOf(IntentValidation.Valid::class.java)
        return validation as IntentValidation.Valid
    }

    private companion object {
        const val TAG = "LlamaInterpreterTest"
    }
}
