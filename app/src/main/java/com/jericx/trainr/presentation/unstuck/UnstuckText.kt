package com.jericx.trainr.presentation.unstuck

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R

@Composable
internal fun joinAnd(parts: List<String>): String = when {
    parts.isEmpty() -> ""
    parts.size == 1 -> parts.first()
    else -> stringResource(
        R.string.join_and_format,
        parts.dropLast(1).joinToString(", "),
        parts.last()
    )
}
