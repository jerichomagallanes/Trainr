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
        onRestore: () -> Unit = {},
        onOpenLink: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ProStatusScreen(isWorking = false, onRestore = onRestore, onOpenLink = onOpenLink)
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
}
