package com.jericx.trainr.presentation.purchases

import android.app.Activity
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.R
import com.jericx.trainr.data.purchases.Entitlements
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.models.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProPaywallViewModel @Inject constructor(
    private val entitlements: Entitlements
) : ViewModel() {

    data class State(
        val plans: List<PaywallPlan> = emptyList(),
        val selectedId: String? = null,
        val isWorking: Boolean = false,
        val noticeRes: Int? = null,
        val isPro: Boolean = false,
        val isLifetime: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var packages: List<Package> = emptyList()

    fun load() {
        viewModelScope.launch {
            val isPro = entitlements.refresh()
            entitlements.loadOffering()
            // Only the three things we sell. Anything else in the offering is
            // ignored rather than shown with a guessed label.
            packages = entitlements.offering?.availablePackages.orEmpty()
                .filter { it.packageType in SOLD }
            val plans = packages.map { it.toPlan(packages) }
            _state.value = _state.value.copy(
                isPro = isPro,
                isLifetime = entitlements.isLifetime.value,
                plans = plans,
                selectedId = preferred(plans)?.id
            )
        }
    }

    fun select(id: String) {
        _state.value = _state.value.copy(selectedId = id)
    }

    fun buy(activity: Activity) {
        val chosen = packages.firstOrNull { it.identifier == _state.value.selectedId } ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true)
            val bought = runCatching {
                Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, chosen).build()
                )
            }.isSuccess
            _state.value = _state.value.copy(
                isWorking = false,
                isPro = if (bought) entitlements.refresh() else _state.value.isPro,
                isLifetime = entitlements.isLifetime.value
            )
        }
    }

    fun restore() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true)
            val restored = entitlements.restore()
            _state.value = _state.value.copy(
                isWorking = false,
                isPro = restored,
                isLifetime = entitlements.isLifetime.value,
                noticeRes = if (restored) R.string.pro_restored else R.string.pro_nothing_to_restore
            )
        }
    }

    fun noticeShown() {
        _state.value = _state.value.copy(noticeRes = null)
    }

    private fun Package.toPlan(all: List<Package>) = PaywallPlan(
        id = identifier,
        termRes = when (packageType) {
            PackageType.ANNUAL -> R.string.pro_yearly
            PackageType.LIFETIME -> R.string.pro_lifetime
            else -> R.string.pro_monthly
        },
        billingRes = when (packageType) {
            PackageType.ANNUAL -> R.string.pro_billed_annually
            PackageType.LIFETIME -> R.string.pro_pay_once
            else -> R.string.pro_billed_monthly
        },
        price = product.price.formatted,
        savePercent = savingOn(all),
        trial = trialLength(),
        renews = packageType != PackageType.LIFETIME
    )

    // Worked out from the prices the store returns rather than written into the
    // copy, so a price change in the Play Console needs no release.
    private fun Package.savingOn(all: List<Package>): Int? {
        if (packageType != PackageType.ANNUAL) return null
        val monthly = all.firstOrNull { it.packageType == PackageType.MONTHLY } ?: return null
        val yearOfMonthly = monthly.product.price.amountMicros * 12
        if (yearOfMonthly <= 0) return null
        val saved = 100 - (product.price.amountMicros * 100 / yearOfMonthly)
        return saved.toInt().takeIf { it > 0 }
    }

    private fun Package.trialLength(): String? =
        product.defaultOption?.freePhase?.billingPeriod?.let(::periodText)

    private fun preferred(plans: List<PaywallPlan>) =
        plans.firstOrNull { it.savePercent != null } ?: plans.firstOrNull()

    companion object {
        private val SOLD = setOf(PackageType.MONTHLY, PackageType.ANNUAL, PackageType.LIFETIME)

        // Held to the unit the store reported: a seven day trial that renders
        // as "1 week" is not the offer that was configured and not what the
        // receipt will say.
        fun periodText(period: Period): String? {
            val unit = when (period.unit) {
                Period.Unit.DAY -> MeasureUnit.DAY
                Period.Unit.WEEK -> MeasureUnit.WEEK
                Period.Unit.MONTH -> MeasureUnit.MONTH
                Period.Unit.YEAR -> MeasureUnit.YEAR
                Period.Unit.UNKNOWN -> return null
            }
            return MeasureFormat
                .getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.WIDE)
                .format(Measure(period.value, unit))
        }
    }
}
