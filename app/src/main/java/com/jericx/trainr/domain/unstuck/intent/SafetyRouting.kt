package com.jericx.trainr.domain.unstuck.intent

object SafetyRouting {

    private val painWords = Regex(
        "(?<![a-z])(hurt|hurts|pain|painful|sore|injur[a-z]*|sharp|ache|aching|strain|tweak|twinge|discomfort)(?![a-z])",
        RegexOption.IGNORE_CASE
    )

    fun flagsPain(note: String): Boolean = painWords.containsMatchIn(note)
}
