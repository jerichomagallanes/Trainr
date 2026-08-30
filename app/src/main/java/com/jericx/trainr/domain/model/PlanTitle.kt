package com.jericx.trainr.domain.model

// Which week a plan is belongs to the plan, not to its name: the app shows
// "Week 3" from the number it stored. A title that carries its own number
// contradicts that the moment the week is copied into another one — a repeat of
// week one would sit at week two still calling itself the first.
private val WEEK_NUMBER = Regex(
    """\s*[-–—:(\[]?\s*week\s*#?\s*\d+\s*[)\]]?\s*""",
    RegexOption.IGNORE_CASE
)

private val TRAILING_PUNCTUATION = Regex("""^[\s\-–—:,(\[]+|[\s\-–—:,(\[]+$""")

fun String.withoutWeekNumber(): String {
    val stripped = WEEK_NUMBER.replace(this, " ").replace(Regex("""\s{2,}"""), " ")
    val trimmed = TRAILING_PUNCTUATION.replace(stripped, "").trim()
    // A title that was nothing but its week number keeps what it had, since an
    // empty one would fail validation and cost the client a retry.
    return trimmed.ifBlank { trim() }
}
