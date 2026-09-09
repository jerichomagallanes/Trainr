package com.jericx.trainr.presentation.purchases

// Shared by the paywall and the subscription screen, which have to agree: the
// terms someone accepts when buying are the terms they are shown afterwards.
object ProLinks {
    const val TERMS = "https://jerichomagallanes.github.io/Trainr/terms-of-use"
    const val PRIVACY = "https://jerichomagallanes.github.io/Trainr/privacy-policy"

    // Cancelling and changing plan happen in Play's own settings, never here.
    const val SUBSCRIPTIONS = "https://play.google.com/store/account/subscriptions"
}
