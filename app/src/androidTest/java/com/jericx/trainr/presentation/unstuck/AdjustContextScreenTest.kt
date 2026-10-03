package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustContextScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        note: String = "",
        interpreter: InterpreterUi = InterpreterUi.Unsupported,
        isInterpreting: Boolean = false,
        hint: ContextHint? = null,
        onTypeNote: (String) -> Unit = {},
        onChoose: (DirectReason) -> Unit = {},
        onUseNote: () -> Unit = {},
        onInstallModel: () -> Unit = {},
        onCancelInstall: () -> Unit = {},
        onOpenLicence: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustContextScreen(
                    note = note,
                    interpreter = interpreter,
                    isInterpreting = isInterpreting,
                    hint = hint,
                    onTypeNote = onTypeNote,
                    onChoose = onChoose,
                    onUseNote = onUseNote,
                    onInstallModel = onInstallModel,
                    onCancelInstall = onCancelInstall,
                    onOpenLicence = onOpenLicence
                )
            }
        }
    }

    @Test
    fun theNoteStaysOnTheDeviceAndOffersTheStructuredChoices() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.context_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.context_private)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.context_option_time)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.context_option_equipment))
            .assertIsDisplayed()
    }

    @Test
    fun typingReportsTheNote() {
        var typed = ""
        setScreen(onTypeNote = { typed = it })

        composeTestRule.onNode(hasSetTextAction()).performTextInput("the rack is taken")

        assertThat(typed).isEqualTo("the rack is taken")
    }

    @Test
    fun choosingAPartReportsItsReason() {
        var chosen: DirectReason? = null
        setScreen(onChoose = { chosen = it })

        composeTestRule.onNodeWithText(string(R.string.context_option_equipment)).performClick()

        assertThat(chosen).isEqualTo(DirectReason.EQUIPMENT)
    }

    @Test
    fun anUnsupportedDeviceSeesNothingAboutTheModel() {
        setScreen(interpreter = InterpreterUi.Unsupported)

        composeTestRule.onNodeWithText(string(R.string.private_coaching_setup_title))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.context_use_note).uppercase()).assertDoesNotExist()
    }

    @Test
    fun anUninstalledModelOffersSetUpAndTheLicence() {
        var installs = 0
        var licences = 0
        setScreen(
            interpreter = InterpreterUi.NotInstalled,
            onInstallModel = { installs++ },
            onOpenLicence = { licences++ }
        )

        composeTestRule.onNodeWithText(string(R.string.private_coaching_setup_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.private_coaching_setup_title)).performClick()
        composeTestRule.onNodeWithText(string(R.string.private_coaching_licence)).performClick()

        assertThat(installs).isEqualTo(1)
        assertThat(licences).isEqualTo(1)
        composeTestRule.onNodeWithText(string(R.string.context_use_note).uppercase()).assertDoesNotExist()
    }

    @Test
    fun aDownloadShowsItsPercentageAndCanBeCancelled() {
        var cancels = 0
        setScreen(interpreter = InterpreterUi.Downloading(42), onCancelInstall = { cancels++ })

        composeTestRule.onNodeWithText(string(R.string.private_coaching_downloading_format, 42))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(cancels).isEqualTo(1)
    }

    @Test
    fun aDownloadBeingCheckedSaysSo() {
        setScreen(interpreter = InterpreterUi.Verifying)

        composeTestRule.onNodeWithText(string(R.string.private_coaching_verifying)).assertIsDisplayed()
    }

    @Test
    fun tooLittleSpaceSaysHowMuchToFree() {
        setScreen(interpreter = InterpreterUi.InsufficientStorage)

        composeTestRule.onNodeWithText(string(R.string.private_coaching_storage_message))
            .assertIsDisplayed()
    }

    @Test
    fun aFailedDownloadOffersAnotherTry() {
        var installs = 0
        setScreen(interpreter = InterpreterUi.Failed, onInstallModel = { installs++ })

        composeTestRule.onNodeWithText(string(R.string.private_coaching_failed_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(installs).isEqualTo(1)
    }

    @Test
    fun aReadyModelWaitsForSomethingToBeWritten() {
        setScreen(interpreter = InterpreterUi.Ready)

        composeTestRule.onNodeWithText(string(R.string.context_use_note).uppercase()).assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.back_to_workout)).assertIsDisplayed()
    }

    @Test
    fun aReadyModelReadsTheWrittenNote() {
        var used = 0
        setScreen(note = "the rack is taken", interpreter = InterpreterUi.Ready, onUseNote = { used++ })

        composeTestRule.onNodeWithText(string(R.string.context_use_note).uppercase()).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.context_use_note).uppercase()).performClick()

        assertThat(used).isEqualTo(1)
    }

    @Test
    fun readingTheNoteDisablesTheRowsAndSaysSo() {
        var chosen: DirectReason? = null
        setScreen(
            note = "35 minutes",
            interpreter = InterpreterUi.Ready,
            isInterpreting = true,
            onChoose = { chosen = it }
        )

        composeTestRule.onNodeWithText(string(R.string.context_reading_note).uppercase()).assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.context_option_time)).performClick()

        assertThat(chosen).isNull()
    }

    @Test
    fun eachHintIsShownAboveTheRows() {
        setScreen(interpreter = InterpreterUi.Ready, note = "help", hint = ContextHint.CHOOSER)

        composeTestRule.onNodeWithText(string(R.string.context_hint_chooser)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.context_option_time)).assertIsDisplayed()
    }

    @Test
    fun theFailedHintIsShown() {
        setScreen(interpreter = InterpreterUi.Ready, note = "help", hint = ContextHint.FAILED)

        composeTestRule.onNodeWithText(string(R.string.context_hint_failed)).assertIsDisplayed()
    }

    // The read takes a while and the answer lands on another screen, so the
    // keyboard has nothing left to type into.
    @Test
    fun usingTheNoteTakesTheFocusOffTheField() {
        var used = 0
        setScreen(interpreter = InterpreterUi.Ready, note = "no bar today", onUseNote = { used++ })
        composeTestRule.onNode(hasSetTextAction()).performClick()
        composeTestRule.onNode(hasSetTextAction()).assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.context_use_note).uppercase()).performClick()

        assertThat(used).isEqualTo(1)
        composeTestRule.onNode(hasSetTextAction()).assertIsNotFocused()
    }
}
