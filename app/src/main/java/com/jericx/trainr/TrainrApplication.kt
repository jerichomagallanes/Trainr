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

        // Generation is refused without App Check: it attests the request came
        // from this app on a genuine device, so no key has to ship.
        Firebase.initialize(this)
        // The provider differs by build type, so it lives in the source sets:
        // only debug can see the one accepting a hand-registered token.
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
