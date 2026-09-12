package com.jericx.trainr

import android.app.Application
import com.jericx.trainr.data.purchases.Entitlements
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TrainrApplication : Application() {

    @Inject
    lateinit var entitlements: Entitlements

    override fun onCreate() {
        super.onCreate()

        // Configured here rather than on a screen: the SDK has to be running
        // before anything asks what the person has paid for.
        entitlements.configure()
    }
}
