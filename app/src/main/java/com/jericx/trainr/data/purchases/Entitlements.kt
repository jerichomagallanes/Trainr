package com.jericx.trainr.data.purchases

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.jericx.trainr.BuildConfig
import com.jericx.trainr.R
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
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

    // A lifetime purchase has no expiry, and nothing to manage or cancel.
    private val _isLifetime = MutableStateFlow(false)
    val isLifetime: StateFlow<Boolean> = _isLifetime.asStateFlow()

    var offering: Offering? = null
        private set

    // What the last purchase or restore has to say for itself when it did not
    // end in Pro: nothing for a cancellation, which the buyer did on purpose,
    // and an explanation for everything else, which used to end in silence.
    @StringRes
    var purchaseNotice: Int? = null
        private set

    // False when the store SDK never came up; ProGate lets generation through
    // rather than sell nothing.
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

    suspend fun purchase(activity: Activity, chosen: Package): Boolean {
        purchaseNotice = null
        if (!Purchases.isConfigured) {
            purchaseNotice = R.string.pro_purchase_failed
            return false
        }
        return runCatching {
            Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, chosen).build())
        }.fold(
            onSuccess = { result ->
                val active = read(result.customerInfo)
                if (!active) purchaseNotice = R.string.pro_purchase_not_active
                active
            },
            onFailure = { error ->
                breadcrumbs.record("purchase_failed")
                purchaseNotice = noticeFor(error)
                false
            }
        )
    }

    // Offered as its own action because a buyer on a new phone has no other way
    // back to what they paid for, and both stores require it.
    suspend fun restore(): Boolean {
        purchaseNotice = null
        if (!Purchases.isConfigured) {
            purchaseNotice = R.string.pro_restore_failed_google
            return false
        }
        return runCatching { read(Purchases.sharedInstance.awaitRestore()) }
            .onFailure {
                breadcrumbs.record("restore_failed")
                purchaseNotice = R.string.pro_restore_failed_google
            }
            .getOrDefault(false)
    }

    private fun read(info: CustomerInfo): Boolean {
        val pro = info.entitlements.all[ENTITLEMENT]
        val active = pro?.isActive == true
        _isPro.value = active
        _isLifetime.value = active && pro?.expirationDate == null
        return active
    }

    companion object {
        private const val ENTITLEMENT = "trainr_workout_planner_pro"

        // A cancelled purchase is silent: the buyer closed the sheet and knows
        // it. A pending one says Pro is on its way, so nobody buys it twice.
        // Anything else, the store's error or not, says what to do next.
        @StringRes
        fun noticeFor(error: Throwable): Int? = when {
            error is PurchasesTransactionException && error.userCancelled -> null
            error is PurchasesException &&
                error.code == PurchasesErrorCode.PurchaseCancelledError -> null
            error is PurchasesException &&
                error.code == PurchasesErrorCode.PaymentPendingError -> R.string.pro_purchase_pending_google
            else -> R.string.pro_purchase_failed
        }

        // A public SDK key is meant to ship in the binary; the secret key is
        // never in the app.
        private const val API_KEY = "goog_DCzKEzGLFppqStqXSGJrqEXGdlt"

        private val keyIsShippable: Boolean
            get() = BuildConfig.DEBUG || !API_KEY.startsWith("test_")
    }
}
