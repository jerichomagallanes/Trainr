package com.jericx.trainr

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// The minified build exists to run release's R8 configuration, so it attests
// the way release does. It cannot pass on a CI emulator, which is fine: the
// smoke test asks whether the app launches, not whether it can generate.
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
