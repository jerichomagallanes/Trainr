package com.jericx.trainr.data.purchases

import android.content.Context
import com.jericx.trainr.BuildConfig
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// What the store says this person has paid for. There are no accounts in Trainr,
// so the entitlement lives in the buyer's Play account and comes back on a fresh
// install with no sign-in, which is the whole reason a subscription was chosen
// over anything we would have to remember ourselves.
class Entitlements(
    private val context: Context,
    private val breadcrumbs: Breadcrumbs
) {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    var offering: Offering? = null
        private set

    // Whether anything can actually be sold. The comment below used to claim the
    // paid paths were left open when the key was unusable; nothing implemented
    // that, so the free week simply ran out against a paywall with nothing on
    // it. ProGate reads this and lets generation through instead.
    val canSell: Boolean
        get() = Purchases.isConfigured

    fun configure() {
        if (!keyIsShippable) {
            // A release built against a key that validates nothing would take
            // money it cannot verify. Everything paid stays free instead, which
            // charges nobody and leaves nobody stuck.
            breadcrumbs.record("purchases_not_configured")
            return
        }
        if (BuildConfig.DEBUG) Purchases.logLevel = LogLevel.WARN
        Purchases.configure(PurchasesConfiguration.Builder(context, API_KEY).build())
    }

    suspend fun refresh(): Boolean {
        if (!Purchases.isConfigured) return false
        return runCatching { read(Purchases.sharedInstance.awaitCustomerInfo()) }
            .onFailure { breadcrumbs.record("entitlement_read_failed") }
            .getOrDefault(false)
    }

    suspend fun loadOffering() {
        if (!Purchases.isConfigured) return
        offering = runCatching { Purchases.sharedInstance.awaitOfferings().current }
            .onFailure { breadcrumbs.record("offerings_load_failed") }
            .getOrNull()
    }

    suspend fun restore(): Boolean {
        if (!Purchases.isConfigured) return false
        return runCatching { read(Purchases.sharedInstance.awaitRestore()) }
            .onFailure { breadcrumbs.record("restore_failed") }
            .getOrDefault(false)
    }

    private fun read(info: CustomerInfo): Boolean {
        val active = info.entitlements.all[ENTITLEMENT]?.isActive == true
        _isPro.value = active
        return active
    }

    companion object {
        const val ENTITLEMENT = "trainr_ai_workout_plans_pro"

        // RevenueCat's test store key, and the same one iOS uses: it is
        // platform-agnostic and returns an offering without any Play products
        // existing, which is what lets the paywall be used before the console
        // work is done. A public SDK key is meant to ship in the binary; the
        // secret key is never in the app.
        private const val API_KEY = "test_WMIQYjVmrPgWhTvqwpfnkobWhAB"

        // A sandbox key validates nothing a real buyer does, so a release built
        // against one refuses to configure and leaves every paid path open.
        val keyIsShippable: Boolean
            get() = BuildConfig.DEBUG || !API_KEY.startsWith("test_")
    }
}
