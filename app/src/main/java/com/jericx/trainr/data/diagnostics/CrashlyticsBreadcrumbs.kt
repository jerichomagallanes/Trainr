package com.jericx.trainr.data.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jericx.trainr.domain.diagnostics.Breadcrumbs

// One implementation for every flavour rather than a no-op for development:
// collection is off in the dev manifest, so dev builds still exercise this
// code at no cost.
class CrashlyticsBreadcrumbs : Breadcrumbs {

    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    override fun record(event: String) {
        crashlytics.log(event)
    }
}
