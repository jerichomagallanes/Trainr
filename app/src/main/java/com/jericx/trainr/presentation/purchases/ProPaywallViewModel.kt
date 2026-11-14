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
import com.revenuecat.purchases.models.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
        // Null until the store has answered. Nothing may be drawn that depends
        // on the answer while it is null.
        val isPro: Boolean? = null,
        val isLifetime: Boolean = false,
        // The store was asked and gave up. Distinct from the null above, which
        // is an answer still on its way.
        val readFailed: Boolean = false
    )

    private val _state = MutableStateFlow(
        State(
            isPro = settled(),
            isLifetime = entitlements.isLifetime.value,
            readFailed = entitlements.readFailed.value
        )
    )
    val state: StateFlow<State> = _state.asStateFlow()

    private var packages: List<Package> = emptyList()

    // The entitlement is mirrored from the store's own state rather than
    // written at the end of load(): a read that has already landed is in the
    // first composition, and a later one arrives the moment it lands instead of
    // waiting for the offering call beside it.
    init {
        viewModelScope.launch {
            combine(
                entitlements.isPro,
                entitlements.hasRead,
                entitlements.isLifetime,
                entitlements.readFailed
            ) { isPro, hasRead, isLifetime, readFailed ->
                Triple(isPro.takeIf { hasRead }, isLifetime, readFailed)
            }.collect { (isPro, isLifetime, readFailed) ->
                _state.value = _state.value.copy(
                    isPro = isPro,
                    isLifetime = isLifetime,
                    readFailed = readFailed
                )
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            entitlements.refresh()
            entitlements.loadOffering()
            // Only the three things we sell. Anything else in the offering is
            // ignored rather than shown with a guessed label.
            packages = entitlements.offering?.availablePackages.orEmpty()
                .filter { it.packageType in SOLD }
            val plans = packages.map { it.toPlan(packages) }
            _state.value = _state.value.copy(
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
            entitlements.purchase(activity, chosen)
            _state.value = _state.value.copy(
                isWorking = false,
                noticeRes = entitlements.purchaseNotice
            )
        }
    }

    fun restore() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true)
            val restored = entitlements.restore()
            // A restore that could not reach the store is not an empty one: it
            // says so, and it leaves the entitlement where it was, which a
            // subscriber's Pro depends on.
            _state.value = _state.value.copy(
                isWorking = false,
                noticeRes = entitlements.purchaseNotice
                    ?: if (restored) R.string.pro_restored else R.string.pro_nothing_to_restore
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

    private fun settled(): Boolean? =
        entitlements.isPro.value.takeIf { entitlements.hasRead.value }

    companion object {
        private val SOLD = setOf(PackageType.MONTHLY, PackageType.ANNUAL, PackageType.LIFETIME)

        internal fun preferred(plans: List<PaywallPlan>): PaywallPlan? =
            plans.firstOrNull { it.trial != null && it.savePercent != null }
                ?: plans.firstOrNull { it.trial != null }
                ?: plans.firstOrNull { it.savePercent != null }
                ?: plans.firstOrNull()

        // Said in days below a month, whichever unit the store used: "7 days
        // free" is the offer as people know it, and a store that calls the same
        // offer "1 week" should not change the copy.
        fun periodText(period: Period): String? {
            val (value, unit) = when (period.unit) {
                Period.Unit.DAY -> period.value to MeasureUnit.DAY
                Period.Unit.WEEK -> period.value * 7 to MeasureUnit.DAY
                Period.Unit.MONTH -> period.value to MeasureUnit.MONTH
                Period.Unit.YEAR -> period.value to MeasureUnit.YEAR
                Period.Unit.UNKNOWN -> return null
            }
            return MeasureFormat
                .getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.WIDE)
                .format(Measure(value, unit))
        }
    }
}
