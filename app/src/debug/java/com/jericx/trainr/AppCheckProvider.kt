package com.jericx.trainr

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// Play Integrity cannot vouch for a build installed by adb, so debug presents a
// token registered by hand in the console, printed to logcat on first run. Kept
// in the debug source set so that provider cannot reach a shipped build.
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
