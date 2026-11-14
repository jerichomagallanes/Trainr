package com.jericx.trainr.presentation.purchases

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import org.junit.Test

class PaywallPreferredPlanTest {

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
        savePercent = 33
    )
    private val lifetime = PaywallPlan(
        id = "lifetime",
        termRes = R.string.pro_lifetime,
        billingRes = R.string.pro_pay_once,
        price = "$99.99",
        renews = false
    )

    @Test
    fun theSavingIsPreferredWhenNothingIsFree() {
        assertThat(ProPaywallViewModel.preferred(listOf(monthly, yearly, lifetime))).isEqualTo(yearly)
    }

    @Test
    fun aTrialBeatsASaving() {
        val tried = monthly.copy(trial = "7 days")
        assertThat(ProPaywallViewModel.preferred(listOf(tried, yearly, lifetime))).isEqualTo(tried)
    }

    @Test
    fun withATrialOnBothTheSavingDecides() {
        val triedMonthly = monthly.copy(trial = "7 days")
        val triedYearly = yearly.copy(trial = "7 days")
        assertThat(ProPaywallViewModel.preferred(listOf(triedMonthly, triedYearly, lifetime)))
            .isEqualTo(triedYearly)
    }

    @Test
    fun withNeitherTheFirstPlanStands() {
        assertThat(ProPaywallViewModel.preferred(listOf(monthly, lifetime))).isEqualTo(monthly)
        assertThat(ProPaywallViewModel.preferred(emptyList())).isNull()
    }
}
