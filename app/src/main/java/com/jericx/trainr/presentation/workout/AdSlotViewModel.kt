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

    val showAds: StateFlow<Boolean> = combine(entitlements.isPro, ads.canShowAds) { pro, allowed ->
        allowed && !pro
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val privacyOptionsRequired: StateFlow<Boolean> = ads.privacyOptionsRequired
}
