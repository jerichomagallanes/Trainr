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
import androidx.compose.ui.graphics.Color
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
import com.jericx.trainr.presentation.common.theme.Orange500
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
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
    // A plain value, moved straight from the drag. It used to be an Animatable
    // fed by a coroutine launched per delta, and an Animatable takes one mutator
    // at a time: a snap queued behind the finger could land after the release
    // began animating and cancel it, stranding the thumb mid-track.
    var offsetPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White)
            .border(3.dp, Orange500, MaterialTheme.shapes.medium)
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

        // The paint trails the thumb: it starts where the thumb starts and stops
        // where the thumb begins, so it is exactly nothing until the first drag
        // and the circle keeps sitting on unpainted track, staying the
        // orange-on-white it was designed as. Filling under the thumb instead
        // made it the same colour as its own background and it disappeared.
        // The paint ends where the thumb begins, so the circle keeps sitting on
        // unpainted track and stays the orange-on-white it was designed as.
        // Painting under it instead made it the same colour as its own
        // background and it disappeared.
        //
        // The thumb's own inset eases in over the first few pixels rather than
        // being added the moment the thumb moves. Added as a step it left a
        // stub of paint behind while the thumb sprang home, which sat there and
        // then vanished all at once at the very end. This way the paint is a
        // continuous function of the offset, so it shrinks back to nothing.
        val fillInset = with(density) { Spacing.tight.toPx() }
        val fillEnd = offsetPx + offsetPx.coerceAtMost(fillInset)

        // Untouched track: an orange label on white.
        Text(
            text = text,
            style = labelStyle,
            color = Orange500,
            modifier = Modifier.padding(start = labelPadding)
        )

        // The same strip again in the inverse colours, cut off exactly where
        // the thumb has reached. Drawing it twice and clipping the top copy is
        // what lets one word be orange on the near side of the thumb and white
        // on the far side, instead of the fill sliding under unchanged text.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    clipRect(right = fillEnd) { this@drawWithContent.drawContent() }
                }
                .background(Orange500),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = labelStyle,
                color = Color.White,
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
                        // Short of the end, the thumb returns: a slide that was
                        // not finished did not ask for anything.
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
