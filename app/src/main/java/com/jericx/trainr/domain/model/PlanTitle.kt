package com.jericx.trainr.domain.model

// The week number belongs to the plan, not its title: a title carrying its own
// number contradicts the stored one as soon as the week is copied into another.
private val WEEK_NUMBER = Regex(
    """\s*[-–—:(\[]?\s*week\s*#?\s*\d+\s*[)\]]?\s*""",
    RegexOption.IGNORE_CASE
)

private val TRAILING_PUNCTUATION = Regex("""^[\s\-–—:,(\[]+|[\s\-–—:,(\[]+$""")

fun String.withoutWeekNumber(): String {
    val stripped = WEEK_NUMBER.replace(this, " ").replace(Regex("""\s{2,}"""), " ")
    val trimmed = TRAILING_PUNCTUATION.replace(stripped, "").trim()
    // An empty title would fail validation, so one that was only a week number
    // keeps what it had.
    return trimmed.ifBlank { trim() }
}
