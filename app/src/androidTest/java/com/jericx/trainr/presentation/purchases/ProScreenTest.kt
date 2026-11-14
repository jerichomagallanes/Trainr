package com.jericx.trainr.presentation.purchases

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// A subscriber used to be shown the sales page and an error on the way to the
// status screen, because the only state the first composition could have said
// they had not paid. These pin which screen each answer gets, first composition
// included: nothing here recomposes.
@RunWith(AndroidJUnit4::class)
class ProScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private val plans = listOf(
        PaywallPlan(
            id = "yearly",
            termRes = R.string.pro_yearly,
            billingRes = R.string.pro_billed_annually,
            price = "$79.99"
        )
    )

    private fun setScreen(
        isPro: Boolean?,
        plans: List<PaywallPlan> = emptyList(),
        readFailed: Boolean = false,
        onClose: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ProScreen(
                    state = ProPaywallViewModel.State(
                        isPro = isPro,
                        plans = plans,
                        readFailed = readFailed
                    ),
                    onSelect = {},
                    onBuy = {},
                    onRestore = {},
                    onNoticeShown = {},
                    onClose = onClose,
                    onOpenLink = {}
                )
            }
        }
    }

    @Test
    fun aSubscriberIsNeverShownTheSalesPage() {
        setScreen(isPro = true)

        composeTestRule.onNodeWithText(string(R.string.pro_active)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_full_access)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_compare_title)).assertDoesNotExist()
    }

    // The offering has not loaded in the first composition either, so the bar
    // used to tell a paying subscriber that Pro could not be sold to them.
    @Test
    fun aSubscriberIsNeverToldProIsUnavailable() {
        setScreen(isPro = true)

        composeTestRule.onNodeWithText(string(R.string.pro_unavailable)).assertDoesNotExist()
    }

    @Test
    fun someoneWithoutProStillGetsTheSalesPage() {
        setScreen(isPro = false, plans = plans)

        composeTestRule.onNodeWithText(string(R.string.pro_full_access)).assertIsDisplayed()
    }

    @Test
    fun anUnreadEntitlementShowsNeitherScreen() {
        setScreen(isPro = null)

        composeTestRule.onNodeWithText(string(R.string.pro_full_access)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_unavailable)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_active)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_check_failed)).assertDoesNotExist()
    }

    // A read that gave up is not a read still running: waiting it out would
    // leave a subscriber on a new phone with no way to restore anything.
    @Test
    fun aReadThatFailedSaysSoAndStillOffersRestore() {
        setScreen(isPro = null, readFailed = true)

        composeTestRule.onNodeWithText(string(R.string.pro_check_failed)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_restore)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_full_access)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_active)).assertDoesNotExist()
    }

    // Saying nothing must not mean stranding someone on a blank page.
    @Test
    fun anUnreadEntitlementStillOffersAWayBack() {
        var left = false
        setScreen(isPro = null, onClose = { left = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertThat(left).isTrue()
    }
}
