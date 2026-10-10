package com.jericx.trainr.presentation.purchases

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
        noticeRes: Int? = null,
        onBuy: () -> Unit = {},
        onRestore: () -> Unit = {},
        onNoticeShown: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                ProPaywallScreen(
                    reason = reason,
                    plans = plans,
                    selectedId = selectedId,
                    isWorking = false,
                    noticeRes = noticeRes,
                    onSelect = {},
                    onBuy = onBuy,
                    onRestore = onRestore,
                    onNoticeShown = onNoticeShown,
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
        // Its own row in the table is the only other place it appears, and a
        // comparison row is read as one sentence, so that one lives in the
        // unmerged tree alone.
        composeTestRule.onAllNodesWithText(
            string(R.string.pro_feature_rewrite_title),
            useUnmergedTree = true
        ).assertCountEquals(2)
    }

    // Reaching the paywall from Unstuck must not open on the free week, which is
    // a limit the person may never have hit.
    @Test
    fun anAdjustmentThatAsksForProLeadsWithAdjusting() {
        setScreen(reason = PaywallReason.ADJUST)

        composeTestRule.onNodeWithText(string(R.string.pro_feature_adjust_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_feature_adjust_body))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_free_limit_adjust)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_free_limit)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_feature_next_week_title)).assertExists()
    }

    @Test
    fun itSellsAdjustingAsAFeatureAndARow() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_feature_adjust_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_compare_adjust), useUnmergedTree = true)
            .assertExists()
    }

    // Opened from the profile menu, nothing was reached for, so nothing leads.
    @Test
    fun itSellsTheEndOfAdsAsAFeatureAndARow() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_feature_ads_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pro_compare_ads), useUnmergedTree = true)
            .assertExists()
    }

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

        composeTestRule
            .onNodeWithText(string(R.string.pro_compare_generated), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        // Two rows read "1": the free week, and the adjustment included with it.
        composeTestRule.onAllNodesWithText(string(R.string.pro_compare_one), useUnmergedTree = true)
            .assertCountEquals(2)
        // "Every week" and not "Unlimited": what Pro gets is a new week once the
        // current one is finished, which is a promise the plan screen keeps.
        composeTestRule
            .onNodeWithText(string(R.string.pro_compare_every_week), useUnmergedTree = true)
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
                    noticeRes = null,
                    onSelect = {},
                    onBuy = {},
                    onRestore = {},
                    onNoticeShown = {},
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

    // A restore that found nothing used to end in silence, which reads as a
    // broken button.
    @Test
    fun aRestoreThatFoundNothingSaysSo() {
        var shown = false
        setScreen(noticeRes = R.string.pro_nothing_to_restore, onNoticeShown = { shown = true })

        composeTestRule.onNodeWithText(string(R.string.pro_nothing_to_restore)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.close)).performClick()

        assertThat(shown).isTrue()
    }

    @Test
    fun noNoticeIsNoDialog() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.pro_nothing_to_restore)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.pro_restored)).assertDoesNotExist()
    }

    // The defect is a window of this size: the emulator's own is a third taller
    // again, and the bar fits inside it whatever the ceiling does.
    private val reviewWindow = DpSize(375.dp, 667.dp)

    private fun setPaywallInAReviewWindow(scale: Float) {
        composeTestRule.setContent {
            val base = LocalDensity.current
            val container = with(base) {
                IntSize(reviewWindow.width.roundToPx(), reviewWindow.height.roundToPx())
            }
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale = scale),
                LocalWindowInfo provides FixedWindow(container)
            ) {
                TrainrTheme {
                    Box(modifier = Modifier.size(reviewWindow).testTag(WINDOW)) {
                        ProPaywallScreen(
                            reason = PaywallReason.NEXT_WEEK,
                            plans = listOf(monthly, yearly, lifetime),
                            selectedId = "yearly",
                            isWorking = false,
                            noticeRes = null,
                            onSelect = {},
                            onBuy = {},
                            onRestore = {},
                            onNoticeShown = {},
                            onClose = {},
                            onOpenLink = {}
                        )
                    }
                }
            }
        }
    }

    private fun callToActionBounds() = composeTestRule.onNodeWithText(
        button(R.string.pro_subscribe_to, string(R.string.pro_yearly))
    ).getUnclippedBoundsInRoot()

    // The bar is pinned and its one unbounded part is the list of plans, so the
    // list is what gives way first. At the size the review saw, the way out of
    // the paywall is on screen, cut off or scrolled away by nothing.
    @Test
    fun theWayOutOfThePaywallIsOnScreen() {
        setPaywallInAReviewWindow(scale = 1f)

        val window = composeTestRule.onNodeWithTag(WINDOW).getUnclippedBoundsInRoot()
        val wayOut = composeTestRule.onNodeWithText(string(R.string.pro_not_now))
            .getUnclippedBoundsInRoot()

        assertThat(callToActionBounds().bottom.value).isAtMost(window.bottom.value)
        assertThat(wayOut.bottom.value).isAtMost(window.bottom.value)
    }

    // Nothing shows this page and the whole of this bar at the largest text
    // size, so the bar keeps its ceiling, the page above it stays a page, and
    // the way out is reached by a scroll the bar draws rather than hides.
    @Test
    fun atTheLargestTextThePaywallKeepsItsPageAndItsWayOut() {
        setPaywallInAReviewWindow(scale = 2f)

        val window = composeTestRule.onNodeWithTag(WINDOW).getUnclippedBoundsInRoot()
        assertThat(callToActionBounds().top.value)
            .isAtLeast((window.top.value + window.bottom.value) / 2f)

        composeTestRule.onNodeWithText(string(R.string.pro_not_now)).performScrollTo()
        val wayOut = composeTestRule.onNodeWithText(string(R.string.pro_not_now))
            .getUnclippedBoundsInRoot()

        assertThat(wayOut.bottom.value).isAtMost(window.bottom.value)
    }

    // Three cards abreast leave no room for a price at the largest text size,
    // and a price that is cut is a price the screen has not stated.
    @Test
    fun atTheLargestTextThePlansStackSoEveryPriceHasItsOwnLine() {
        setPaywallInAReviewWindow(scale = 2f)

        val rows = listOf(monthly, yearly, lifetime).map {
            composeTestRule.onNodeWithText(it.price).getUnclippedBoundsInRoot()
        }

        assertThat(rows[1].top.value).isAtLeast(rows[0].bottom.value)
        assertThat(rows[2].top.value).isAtLeast(rows[1].bottom.value)
    }
}

private const val WINDOW = "reviewWindow"

private class FixedWindow(override val containerSize: IntSize) : WindowInfo {
    override val isWindowFocused = true
}
