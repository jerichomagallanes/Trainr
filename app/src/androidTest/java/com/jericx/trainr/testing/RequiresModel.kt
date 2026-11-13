package com.jericx.trainr.testing

// The 730 MB GGUF is never on a CI emulator, so these tests can only skip there.
// A skip is written into the report as a failure, which left every pull request
// carrying a red check that could not go green and that nobody could read. CI
// excludes them by this annotation instead; a machine with the model pushed runs
// them by leaving the filter off.
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RequiresModel
