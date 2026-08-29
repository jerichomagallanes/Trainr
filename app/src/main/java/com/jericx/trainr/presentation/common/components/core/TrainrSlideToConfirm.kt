package com.jericx.trainr.presentation.common.components.core

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }

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

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Orange500,
            modifier = Modifier.padding(start = Spacing.tight + ThumbSize + Spacing.section)
        )

        Image(
            painter = painterResource(R.drawable.ic_arrow_forward_circle_filled),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = Spacing.tight)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(ThumbSize)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(0f, travel))
                        }
                    },
                    onDragStopped = {
                        if (offset.value >= travel * ConfirmFraction) onConfirm()
                        offset.animateTo(0f)
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
