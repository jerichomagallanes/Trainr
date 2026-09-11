package com.jericx.trainr

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Swaps the app's Application for Hilt's, which is what lets a test inject the
// real graph. TrainrApplication's own start-up - the purchases SDK - does not
// run under it, and no instrumented test depends on it.
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application = super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
