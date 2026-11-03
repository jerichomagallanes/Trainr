package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteSavedScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        note: String = "I had to leave early for work.",
        onViewPreferences: () -> Unit = {},
        onDone: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                NoteSavedScreen(
                    note = note,
                    onViewPreferences = onViewPreferences,
                    onDone = onDone
                )
            }
        }
    }

    @Test
    fun theSavedNoteIsShownBackAndTheNextWorkoutIsSaidToBeUnchanged() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.note_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText("I had to leave early for work.").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.next_workout_unchanged))
            .assertIsDisplayed()
    }

    // The note is the person's own words and is never parsed, so anything that
    // looks like markup has to read back exactly as it was typed.
    @Test
    fun markupInANoteStaysLiteralText() {
        val note = "<script>alert(1)</script>"
        setScreen(note = note)

        composeTestRule.onNodeWithText(note).assertIsDisplayed()
    }

    @Test
    fun preferencesAreOneTapAwayAndTheWayBackIsThePlan() {
        var viewedPreferences = false
        var done = false
        setScreen(onViewPreferences = { viewedPreferences = true }, onDone = { done = true })

        composeTestRule.onNodeWithText(string(R.string.view_training_preferences)).performClick()
        composeTestRule
            .onNodeWithText(string(R.string.back_to_workout_plan).uppercase())
            .performClick()

        assertThat(viewedPreferences).isTrue()
        assertThat(done).isTrue()
    }
}
