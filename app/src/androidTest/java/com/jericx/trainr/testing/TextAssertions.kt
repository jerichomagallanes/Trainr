package com.jericx.trainr.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.TextLayoutResult

// Laid-out text is the only honest witness to a truncated label.
fun notEllipsized(): SemanticsMatcher =
    SemanticsMatcher("label is not ellipsized") { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action?.invoke(results)
        results.firstOrNull()?.isLineEllipsized(0) == false
    }

// A label given less room than its words need wraps rather than truncating, so
// a second line is how Compose shows a column squeezed too narrow.
fun onOneLine(): SemanticsMatcher =
    SemanticsMatcher("label is on one line") { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action?.invoke(results)
        results.firstOrNull()?.lineCount == 1
    }
