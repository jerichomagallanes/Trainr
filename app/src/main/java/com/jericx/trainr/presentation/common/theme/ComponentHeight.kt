package com.jericx.trainr.presentation.common.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// A point size that holds text has to grow with the text, or the glyph inside it
// is a blob at the largest accessibility size.
@Composable
fun Dp.scaledWithText(): Dp = this * LocalDensity.current.fontScale

object ComponentHeight {
    val Pill = 28.dp
    val Chip = 35.dp
    val ChipTall = 39.dp
    val Field = 42.dp
    val Medium = 48.dp
    val Option = 49.dp
    val Large = 56.dp
}