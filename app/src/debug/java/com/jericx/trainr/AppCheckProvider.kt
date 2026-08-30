package com.jericx.trainr

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// Nothing installed by adb came from Play, so Play Integrity can never vouch
// for a debug build. It presents a token registered by hand in the console
// instead — printed to logcat on first run — which is what makes development
// possible at all. This lives in the debug source set so the provider that
// accepts a hand-registered token cannot reach a shipped build.
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
