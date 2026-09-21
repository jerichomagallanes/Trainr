package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
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

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setScreen(
        note: String = "",
        onTypeNote: (String) -> Unit = {},
        onChoose: (DirectReason) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustContextScreen(note = note, onTypeNote = onTypeNote, onChoose = onChoose)
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
}
