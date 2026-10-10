package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.theme.trainrColors

// A scroll held to a ceiling, which says so when it is holding anything back.
// Compose draws no scrollbar of its own, and content cut at a hard edge reads as
// the end of it: that is how the paywall's way out came to look missing.
@Composable
fun TrainrCappedScroll(
    ceiling: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberScrollState()
    val thumbColor = MaterialTheme.trainrColors.outlineControl

    Box(
        modifier = modifier
            .heightIn(max = ceiling)
            .drawWithContent {
                drawContent()
                val hidden = state.maxValue
                if (hidden <= 0) return@drawWithContent
                val track = size.height
                val thumb = (track * track / (track + hidden)).coerceAtLeast(THUMB_MIN.toPx())
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(
                        x = size.width - THUMB_WIDTH.toPx(),
                        y = (track - thumb) * (state.value.toFloat() / hidden)
                    ),
                    size = Size(THUMB_WIDTH.toPx(), thumb),
                    cornerRadius = CornerRadius(THUMB_WIDTH.toPx() / 2)
                )
            }
            .verticalScroll(state)
    ) {
        Box(modifier = Modifier.padding(end = if (state.maxValue > 0) THUMB_GUTTER else 0.dp)) {
            content()
        }
    }
}

private val THUMB_WIDTH = 3.dp
private val THUMB_MIN = 24.dp
private val THUMB_GUTTER = 7.dp
