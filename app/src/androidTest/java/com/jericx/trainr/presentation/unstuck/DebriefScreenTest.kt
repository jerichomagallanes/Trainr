package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebriefScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        note: String = "",
        onTypeNote: (String) -> Unit = {},
        onSave: () -> Unit = {},
        onSkip: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                DebriefScreen(
                    note = note,
                    onTypeNote = onTypeNote,
                    onSave = onSave,
                    onSkip = onSkip
                )
            }
        }
    }

    @Test
    fun theQuestionIsAskedWithAnExampleAndAPromise() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.debrief_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.debrief_placeholder)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.notes_stay_on_device)).assertIsDisplayed()
    }

    // Nothing to save is not a thing to save: the button waits for words.
    @Test
    fun saveIsDisabledWhileTheNoteIsBlank() {
        setScreen(note = "   ")

        composeTestRule.onNodeWithText(string(R.string.save_note).uppercase()).assertIsNotEnabled()
    }

    @Test
    fun aWrittenNoteEnablesTheSave() {
        var saved = false
        setScreen(note = "Gym was busy.", onSave = { saved = true })

        composeTestRule.onNodeWithText(string(R.string.save_note).uppercase()).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.save_note).uppercase()).performClick()

        assertThat(saved).isTrue()
    }

    @Test
    fun whatIsTypedIsReported() {
        var typed: String? = null
        setScreen(onTypeNote = { typed = it })

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Left early.")

        assertThat(typed).isEqualTo("Left early.")
    }

    @Test
    fun theNoteCanAlwaysBeSkipped() {
        var skipped = false
        setScreen(onSkip = { skipped = true })

        composeTestRule.onNodeWithText(string(R.string.skip)).performClick()

        assertThat(skipped).isTrue()
    }
}
