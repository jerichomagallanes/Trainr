package com.jericx.trainr.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.data.ads.Ads
import com.jericx.trainr.data.purchases.Entitlements
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Kept apart from the plan's own view model so the plan stays testable without
// a billing SDK or an ad SDK in the room.
@HiltViewModel
class AdSlotViewModel @Inject constructor(
    entitlements: Entitlements,
    val ads: Ads
) : ViewModel() {

    // Nothing is requested until the store has answered. A subscriber reads as
    // not Pro for the whole of that gap, so the banner they pay to be rid of
    // would be fetched and drawn before the answer landed. A read that failed
    // never answers, and this stays false for it: an impression nobody serves
    // costs less than the one thing Pro promises to remove.
    val showAds: StateFlow<Boolean> = combine(
        entitlements.isPro,
        entitlements.hasRead,
        ads.canShowAds
    ) { pro, answered, allowed ->
        allowed && answered && !pro
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val privacyOptionsRequired: StateFlow<Boolean> = ads.privacyOptionsRequired
}
