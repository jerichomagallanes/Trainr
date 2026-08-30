package com.jericx.trainr.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.TextLayoutResult

// Truncated labels have shipped twice; laid-out text is the only honest witness.
fun notEllipsized(): SemanticsMatcher =
    SemanticsMatcher("label is not ellipsized") { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action?.invoke(results)
        results.firstOrNull()?.isLineEllipsized(0) == false
    }
