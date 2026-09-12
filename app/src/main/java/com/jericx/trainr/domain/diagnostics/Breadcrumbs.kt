package com.jericx.trainr.domain.diagnostics

// Events and app state only — never a name, age, weight or injury. Crash
// reports are stored by Google and must "say what broke, not who you are".
interface Breadcrumbs {

    fun record(event: String)
}
