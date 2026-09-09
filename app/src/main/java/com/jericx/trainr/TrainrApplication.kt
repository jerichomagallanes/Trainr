package com.jericx.trainr

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.initialize
import com.jericx.trainr.data.purchases.Entitlements
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TrainrApplication : Application() {

    @Inject
    lateinit var entitlements: Entitlements

    override fun onCreate() {
        super.onCreate()

        // Generation is refused without App Check: it attests the request came
        // from this app on a genuine device, so no key has to ship.
        Firebase.initialize(this)
        // The provider differs by build type, so it lives in the source sets:
        // only debug can see the one accepting a hand-registered token.
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())

        // Configured here rather than on a screen: the SDK has to be running
        // before anything asks what the person has paid for.
        entitlements.configure()
    }
}
