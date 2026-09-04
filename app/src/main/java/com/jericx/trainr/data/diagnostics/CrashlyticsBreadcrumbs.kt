package com.jericx.trainr.data.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jericx.trainr.domain.diagnostics.Breadcrumbs

// Written straight onto the next crash report.
//
// One implementation for every flavour rather than a no-op for development:
// collection is switched off in the dev manifest, so recording there costs
// nothing and goes nowhere, and every dev build still exercises this code. A
// breadcrumb that only runs in the shipped build is a breadcrumb nobody has
// ever seen work.
class CrashlyticsBreadcrumbs : Breadcrumbs {

    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    override fun record(event: String) {
        crashlytics.log(event)
    }

    override fun state(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
