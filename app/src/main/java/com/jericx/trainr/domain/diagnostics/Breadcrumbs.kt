package com.jericx.trainr.domain.diagnostics

// Events and app state only — never a name, age, weight or injury. Crash
// reports are stored by Google and must "say what broke, not who you are".
interface Breadcrumbs {

    fun record(event: String)

    fun state(key: String, value: String)
}

object NoBreadcrumbs : Breadcrumbs {
    override fun record(event: String) = Unit
    override fun state(key: String, value: String) = Unit
}
