package com.jericx.trainr.data.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.jericx.trainr.BuildConfig
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Ads are the price of the free tier and nothing more: Pro never sees one, and
// no ad is requested until Google's consent flow says it may be. The flow is
// Google's own so that EEA and UK consent, and the iOS tracking prompt on the
// other platform, are handled the way AdMob requires rather than approximated.
class Ads(private val breadcrumbs: Breadcrumbs) {

    private val _canShowAds = MutableStateFlow(false)
    val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val initialised = AtomicBoolean(false)

    // Asked on every launch, as Google requires: consent already given comes
    // back from the SDK's cache without showing anything, and the rest of the
    // app never waits on it.
    fun gatherConsent(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { breadcrumbs.record("ads: consent form ${it.errorCode}") }
                    settle(activity, consent)
                }
            },
            { requestError ->
                breadcrumbs.record("ads: consent update ${requestError.errorCode}")
                settle(activity, consent)
            }
        )
        // A previous launch may already have answered; the ad slot can start
        // now rather than after this launch's round trip.
        settle(activity, consent)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { breadcrumbs.record("ads: privacy options ${it.errorCode}") }
        }
    }

    private fun settle(activity: Activity, consent: ConsentInformation) {
        _privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds()) return
        if (initialised.compareAndSet(false, true)) {
            MobileAds.initialize(activity.applicationContext) {}
        }
        _canShowAds.value = true
    }

    companion object {
        // AdMob ad units. The debug id is Google's public test unit, which pays
        // nothing and never risks the account. The release id belongs to the
        // "Weekly plan banner" unit under the Android app in apps.admob.com.
        val PLAN_BANNER_UNIT_ID: String = if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/9214589741"
        } else {
            BuildConfig.ADMOB_PLAN_BANNER_ID
        }
    }
}
