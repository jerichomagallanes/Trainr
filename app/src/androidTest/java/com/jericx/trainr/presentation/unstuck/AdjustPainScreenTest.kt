package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.testing.notEllipsized
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustPainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setScreen(onSaveAndFinishEarly: () -> Unit = {}, onReturn: () -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustPainScreen(
                    onSaveAndFinishEarly = onSaveAndFinishEarly,
                    onReturn = onReturn
                )
            }
        }
    }

    @Test
    fun theScreenPausesAndPointsAtRealHelp() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pain_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pain_card_body_1)).assert(notEllipsized())
        composeTestRule.onNodeWithText(string(R.string.pain_card_body_2)).assert(notEllipsized())
    }

    // Pain is a free route: nothing here may sell anything or offer a substitute.
    @Test
    fun nothingHereAsksForPro() {
        setScreen()

        listOf(
            R.string.pro_upgrade_title,
            R.string.pro_continue,
            R.string.pro_name,
            R.string.show_recommendation
        ).forEach { composeTestRule.onNodeWithText(string(it)).assertDoesNotExist() }
    }

    @Test
    fun savingWhatWasDoneReportsBack() {
        var saved = false
        var returned = false
        setScreen(onSaveAndFinishEarly = { saved = true }, onReturn = { returned = true })

        composeTestRule.onNodeWithText(string(R.string.save_and_finish_early).uppercase())
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.return_to_workout)).performClick()

        assertThat(saved).isTrue()
        assertThat(returned).isTrue()
    }
}
