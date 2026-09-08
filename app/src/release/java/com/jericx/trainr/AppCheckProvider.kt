package com.jericx.trainr

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// The shipped build: Play Integrity vouches that this is the real app, from
// Play, on an untampered device, in place of a key travelling inside the APK.
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
