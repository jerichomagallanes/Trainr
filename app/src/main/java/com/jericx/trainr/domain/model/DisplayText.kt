package com.jericx.trainr.domain.model

// Read off a controlled vocabulary: LOWER_BACK is Lower Back everywhere, so
// there is nothing to translate that the enum does not already say.
internal fun Enum<*>.asDisplayText(): String = name.lowercase()
    .split('_')
    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
