package com.jericx.trainr.presentation.common.components.core

import androidx.compose.animation.core.animate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import kotlin.math.roundToInt

private val TrackHeight = 55.dp
private val ThumbSize = 35.dp
private const val ConfirmFraction = 0.9f

@Composable
fun TrainrSlideToConfirm(
    text: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // A plain value, not an Animatable: an Animatable takes one mutator at a
    // time, so a snap queued behind the finger cancels the release mid-track.
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.trainrColors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceRaised)
            .border(3.dp, colors.brand, MaterialTheme.shapes.medium)
            .semantics(mergeDescendants = true) {
                onClick(label = text) {
                    onConfirm()
                    true
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val travel = with(density) { (maxWidth - Spacing.tight * 2 - ThumbSize).toPx() }
            .coerceAtLeast(0f)

        val labelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        val labelPadding = Spacing.tight + ThumbSize + Spacing.section

        // The paint ends where the thumb begins, so the thumb keeps sitting on
        // unpainted track instead of vanishing into its own colour. The inset
        // eases in with the offset rather than as a step, or a stub of paint is
        // left behind while the thumb springs home.
        val fillInset = with(density) { Spacing.tight.toPx() }
        val fillEnd = offsetPx + offsetPx.coerceAtMost(fillInset)

        Text(
            text = text,
            style = labelStyle,
            color = colors.brandLarge,
            modifier = Modifier.padding(start = labelPadding)
        )

        // The same label in inverse colours, clipped at the thumb, is what makes
        // one word orange behind the thumb and white ahead of it.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    clipRect(right = fillEnd) { this@drawWithContent.drawContent() }
                }
                .background(colors.brandLarge),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = labelStyle,
                color = colors.onBrand,
                modifier = Modifier.padding(start = labelPadding)
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_arrow_forward_circle_filled),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = Spacing.tight)
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .size(ThumbSize)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(0f, travel)
                    },
                    onDragStopped = {
                        if (offsetPx >= travel * ConfirmFraction) onConfirm()
                        animate(offsetPx, 0f) { value, _ -> offsetPx = value }
                    }
                )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrSlideToConfirmPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            TrainrSlideToConfirm(
                text = stringResource(R.string.slide_to_complete_routine),
                onConfirm = {}
            )
        }
    }
}
