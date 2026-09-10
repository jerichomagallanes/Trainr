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

    // Two plans is what actually ships: lifetime is built but not sold, so the
    // offering returns Monthly and Yearly only. Every other test here passes a
    // lifetime plan, which means the configuration real buyers see was the one
    // configuration nothing covered.
    @Test
    fun twoPlansIsAWholePaywall() {
        setScreen(plans = listOf(monthly, yearly), selectedId = "yearly")

        composeTestRule.onNodeWithText(string(R.string.pro_monthly)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_yearly)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_save_percent, 33)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            button(R.string.pro_subscribe_to, string(R.string.pro_yearly))
        ).assertIsDisplayed()

        // Nothing that belongs to a product we are not selling.
        composeTestRule.onNodeWithText(string(R.string.pro_lifetime)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_pay_once)).assertDoesNotExist()
        composeTestRule.onNodeWithText(button(R.string.pro_buy_lifetime)).assertDoesNotExist()

        // And the renewal terms still appear, because everything on sale renews.
        composeTestRule.onNodeWithText(string(R.string.pro_renewal_google))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // The table is the one place the free tier's actual number appears.
    @Test
    fun theComparisonNamesWhatTheFreeTierGets() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_compare_generated))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_compare_one)).assertIsDisplayed()
        // "Every week" and not "Unlimited": what Pro gets is a new week once the
        // current one is finished, which is a promise the plan screen keeps.
        composeTestRule.onNodeWithText(string(R.string.pro_compare_every_week))
            .assertIsDisplayed()
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

    // Play has no standard EULA to fall back on, so this link is the only terms
    // an Android buyer is ever shown.
    @Test
    fun theTermsAreLinkedBeforeBuying() {
        var opened: String? = null
        composeTestRule.setContent {
            TrainrTheme {
                ProPaywallScreen(
                    reason = PaywallReason.NEXT_WEEK,
                    plans = listOf(monthly, yearly, lifetime),
                    selectedId = "yearly",
                    isWorking = false,
                    onSelect = {},
                    onBuy = {},
                    onRestore = {},
                    onClose = {},
                    onOpenLink = { opened = it }
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.pro_terms))
            .performScrollTo()
            .performClick()

        assertThat(opened).isEqualTo(ProLinks.TERMS)
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
