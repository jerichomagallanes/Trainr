package com.jericx.trainr.presentation.purchases

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
class ProPromptSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    // TrainrButton uppercases its label, so the CTA is never on screen as written.
    private fun button(id: Int) = string(id).uppercase()

    private fun setSheet(
        reason: PaywallReason = PaywallReason.NEXT_WEEK,
        onContinue: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ProPromptSheet(reason = reason, onContinue = onContinue, onDismiss = onDismiss)
            }
        }
    }

    // The limit is named where the tap happened, before any price is shown.
    @Test
    fun itNamesTheLimitThatWasHit() {
        setSheet(reason = PaywallReason.FRESH_PLAN)

        composeTestRule.onNodeWithText(string(R.string.pro_upgrade_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_prompt_fresh)).assertIsDisplayed()
    }

    @Test
    fun continueIsWhatOpensThePaywall() {
        var continued = false
        setSheet(onContinue = { continued = true })

        composeTestRule.onNodeWithText(button(R.string.pro_continue)).performClick()

        assertThat(continued).isTrue()
    }

    @Test
    fun notNowLeavesWithoutAPrice() {
        var dismissed = false
        setSheet(onDismiss = { dismissed = true })

        composeTestRule.onNodeWithText(string(R.string.pro_not_now)).performClick()

        assertThat(dismissed).isTrue()
    }
}
