package com.jericx.trainr

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
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
        // How the app proves itself differs by build type, so the choice lives
        // in the source sets: only the debug build can see the provider that
        // accepts a hand-registered token.
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
