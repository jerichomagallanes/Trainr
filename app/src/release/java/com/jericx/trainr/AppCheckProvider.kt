package com.jericx.trainr

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// The shipped build. Play Integrity vouches that this is the real app, from
// Play, on a device that has not been tampered with — which is what replaced
// the key that used to travel inside the APK.
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
