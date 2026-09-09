package com.jericx.trainr.presentation.purchases

import androidx.annotation.StringRes

// One purchasable plan. The store owns every value it can — price, period and
// any trial — so a price change in the Play Console needs no release. The words
// that are ours stay resource ids, because a ViewModel has no business
// resolving them.
data class PaywallPlan(
    val id: String,
    @StringRes val termRes: Int,
    @StringRes val billingRes: Int,
    val price: String,
    val savePercent: Int? = null,
    val trial: String? = null,
    val renews: Boolean = true
)
