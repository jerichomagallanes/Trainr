package com.jericx.trainr.domain.diagnostics

// The trail a crash report carries: what the app was doing just before it fell
// over. A stack trace says which line broke; this says how the client got there.
//
// Deliberately narrow. Only events and app state go in here — never a name, an
// age, a weight or an injury. A crash report is read by whoever is debugging,
// stored by Google, and outlives the session, so the profile has no business in
// one. The policy promises that crash reports "say what broke, not who you are",
// and this interface is where that promise is either kept or broken.
interface Breadcrumbs {

    // Something happened, in the order it happened.
    fun record(event: String)

    // A fact that is true until it changes, attached to whatever crash follows.
    fun state(key: String, value: String)
}

// For tests, and for anywhere a trail would be noise.
object NoBreadcrumbs : Breadcrumbs {
    override fun record(event: String) = Unit
    override fun state(key: String, value: String) = Unit
}
