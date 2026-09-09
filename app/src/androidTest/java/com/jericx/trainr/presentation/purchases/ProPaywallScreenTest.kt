package com.jericx.trainr.presentation.purchases

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProPaywallScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) =
        composeTestRule.activity.getString(id, *args)

    // TrainrButton uppercases its label, so the CTA is never on screen as written.
    private fun button(id: Int, vararg args: Any) = string(id, *args).uppercase()

    private val monthly = PaywallPlan(
        id = "monthly",
        termRes = R.string.pro_monthly,
        billingRes = R.string.pro_billed_monthly,
        price = "$9.99"
    )
    private val yearly = PaywallPlan(
        id = "yearly",
        termRes = R.string.pro_yearly,
        billingRes = R.string.pro_billed_annually,
        price = "$79.99",
        savePercent = 33,
        trial = "7 days"
    )
    private val lifetime = PaywallPlan(
        id = "lifetime",
        termRes = R.string.pro_lifetime,
        billingRes = R.string.pro_pay_once,
        price = "$99.99",
        renews = false
    )

    private fun setScreen(
        reason: PaywallReason? = PaywallReason.NEXT_WEEK,
        plans: List<PaywallPlan> = listOf(monthly, yearly, lifetime),
        selectedId: String? = "yearly",
        onBuy: () -> Unit = {},
        onRestore: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ProPaywallScreen(
                    reason = reason,
                    plans = plans,
                    selectedId = selectedId,
                    isWorking = false,
                    onSelect = {},
                    onBuy = onBuy,
                    onRestore = onRestore,
                    onClose = {},
                    onOpenLink = {}
                )
            }
        }
    }

    @Test
    fun theReasonTheyArrivedLeads() {
        setScreen(reason = PaywallReason.REWRITE)

        composeTestRule.onNodeWithText(string(R.string.pro_feature_rewrite_body))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_free_limit)).assertIsDisplayed()
        // Its own row in the table is the only other place it appears.
        composeTestRule.onAllNodesWithText(string(R.string.pro_feature_rewrite_title))
            .assertCountEquals(2)
    }

    // Opened from the profile menu, nothing was reached for, so nothing leads.
    @Test
    fun withNoReasonNoFeatureLeads() {
        setScreen(reason = null)

        composeTestRule.onNodeWithText(string(R.string.pro_and_more)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_feature_next_week_title))
            .assertIsDisplayed()
    }

    // Play requires the renewal terms on the paywall itself.
    @Test
    fun aRenewingPlanDisclosesTheTerms() {
        setScreen(selectedId = "yearly")

        composeTestRule.onNodeWithText(string(R.string.pro_renewal_google))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // Saying a one-off payment renews would be a false disclosure.
    @Test
    fun aLifetimePurchaseDisclosesNoRenewal() {
        setScreen(selectedId = "lifetime")

        composeTestRule.onNodeWithText(string(R.string.pro_renewal_google)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_cancel_anytime)).assertDoesNotExist()
        composeTestRule.onNodeWithText(button(R.string.pro_buy_lifetime)).assertIsDisplayed()
    }

    @Test
    fun theTrialIsNamedWhereTheStoreOffersOne() {
        setScreen(selectedId = "yearly")

        composeTestRule.onNodeWithText(string(R.string.pro_trial_then, "7 days", "$79.99"))
            .assertIsDisplayed()
    }

    @Test
    fun withNoTrialItSaysTheSubscriptionCanBeCancelled() {
        setScreen(selectedId = "monthly")

        composeTestRule.onNodeWithText(string(R.string.pro_cancel_anytime)).assertIsDisplayed()
    }

    @Test
    fun withNoPlansItSaysSoRatherThanOfferingNothing() {
        setScreen(plans = emptyList(), selectedId = null)

        composeTestRule.onNodeWithText(string(R.string.pro_unavailable)).assertIsDisplayed()
    }

    // The table is the one place the free tier's actual number appears.
    @Test
    fun theComparisonNamesWhatTheFreeTierGets() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_compare_generated))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_compare_one)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(string(R.string.pro_compare_unlimited))
            .assertCountEquals(3)
    }

    // Answers are hidden until asked for, or the paywall becomes a wall of text.
    @Test
    fun aQuestionOpensItsAnswer() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_faq_cancel_a)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_faq_cancel_q))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(string(R.string.pro_faq_cancel_a)).assertIsDisplayed()
    }

    // Restore has to be reachable, not buried behind a purchase.
    @Test
    fun restoreIsOnThePaywall() {
        var restored = false
        setScreen(onRestore = { restored = true })

        composeTestRule.onNodeWithText(string(R.string.pro_restore)).performScrollTo().performClick()

        assertThat(restored).isTrue()
    }
}
