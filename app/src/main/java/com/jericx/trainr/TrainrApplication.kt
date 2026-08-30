package com.jericx.trainr

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrainrApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Generation is refused without this. App Check attests that the
        // request came from this app on a genuine device, which is what makes
        // the key's absence from the APK worth something: there is no longer a
        // secret to steal, and the thing that replaced it cannot be copied.
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Nothing installed by adb came from Play, so Play Integrity can
                // never vouch for a debug build. It presents a token registered
                // by hand in the console instead — printed to logcat on first
                // run — which is why development is possible at all.
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
    }
}
