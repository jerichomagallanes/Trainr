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

// Without this screen a subscriber who reinstalls has nowhere to restore from:
// the paywall closes itself once the entitlement lands.
@RunWith(AndroidJUnit4::class)
class ProStatusScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setScreen(
        noticeRes: Int? = null,
        onRestore: () -> Unit = {},
        onNoticeShown: () -> Unit = {},
        onOpenLink: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ProStatusScreen(
                    isWorking = false,
                    noticeRes = noticeRes,
                    onRestore = onRestore,
                    onNoticeShown = onNoticeShown,
                    onOpenLink = onOpenLink
                )
            }
        }
    }

    @Test
    fun itSaysTheSubscriptionIsActive() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_active)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_restore)).assertIsDisplayed()
    }

    @Test
    fun managingASubscriptionLeavesForPlay() {
        var opened: String? = null
        setScreen(onOpenLink = { opened = it })

        composeTestRule.onNodeWithText(string(R.string.pro_manage)).performClick()

        assertThat(opened).isEqualTo(ProLinks.SUBSCRIPTIONS)
    }

    // The terms someone accepts when buying are the terms they are shown
    // afterwards, so both screens carry the same link.
    @Test
    fun theTermsAreShownToWhoeverAgreedToThem() {
        var opened: String? = null
        setScreen(onOpenLink = { opened = it })

        composeTestRule.onNodeWithText(string(R.string.pro_terms)).performClick()

        assertThat(opened).isEqualTo(ProLinks.TERMS)
    }

    @Test
    fun restoreIsReachableHere() {
        var restored = false
        setScreen(onRestore = { restored = true })

        composeTestRule.onNodeWithText(string(R.string.pro_restore)).performClick()

        assertThat(restored).isTrue()
    }

    // Restoring from here used to end in silence either way, so a subscriber
    // could not tell a working restore from a broken button.
    @Test
    fun aRestoreThatWorkedSaysSo() {
        var shown = false
        setScreen(noticeRes = R.string.pro_restored, onNoticeShown = { shown = true })

        composeTestRule.onNodeWithText(string(R.string.pro_restored)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.close)).performClick()

        assertThat(shown).isTrue()
    }

    @Test
    fun aRestoreThatFoundNothingSaysSo() {
        setScreen(noticeRes = R.string.pro_nothing_to_restore)

        composeTestRule.onNodeWithText(string(R.string.pro_nothing_to_restore)).assertIsDisplayed()
    }

    @Test
    fun noNoticeIsNoDialog() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_restored)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_nothing_to_restore)).assertDoesNotExist()
    }
}
