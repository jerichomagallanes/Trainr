package com.jericx.trainr.presentation.common.components.core

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlin.math.abs
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.jericx.trainr.presentation.common.theme.RedError
import com.jericx.trainr.presentation.common.theme.Spacing
import kotlin.math.roundToInt

// Deleting asks for a deliberate swipe, decided when the finger lifts, the way
// the slide-to-complete control decides. Three quarters rather than its nine
// tenths because a row can only travel as far as the finger has room to carry
// it: a swipe begins where it lands, not at the far edge, so asking for nine
// tenths of the width would leave the delete unreachable for anyone starting an
// inch in. Half a swipe reveals the delete without doing it.

// Dragging a row aside to reveal a delete behind it. What the delete means is
// the caller's business — a set goes at once, a week is asked about first — so
// only the gesture and the reveal live here.
//
// Built on a plain draggable rather than SwipeToDismissBox: that settles on
// velocity as well as distance, so a quick flick deleted from halfway across
// however far the positional threshold was pushed out. Reading the offset on
// release is the same rule the slide-to-complete control uses, and it fires
// exactly once per gesture, so nothing can report a delete twice or strand a
// row half open.
private const val DELETE_FRACTION = 0.75f

@Composable
fun TrainrSwipeToDelete(
    onDelete: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    // A plain value, moved straight from the drag. It used to be an Animatable
    // fed by a coroutine launched per delta, and an Animatable takes one mutator
    // at a time: a snap queued behind the finger could land after the release
    // began animating and cancel it, leaving the row parked open.
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val currentOnDelete by rememberUpdatedState(onDelete)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val travel = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)

        // Behind the row for exactly as long as the row is held aside, so an
        // idle row has nothing red underneath to show through it.
        if (offsetPx < 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(RedError),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.padding(end = Spacing.medium)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (enabled) {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                offsetPx = (offsetPx + delta).coerceIn(-travel, 0f)
                            },
                            onDragStopped = {
                                if (-offsetPx >= travel * DELETE_FRACTION) currentOnDelete()
                                // One animation, in the drag's own scope, so a
                                // new drag interrupts it and nothing else can.
                                animate(offsetPx, 0f) { value, _ -> offsetPx = value }
                            }
                        )
                    } else {
                        // Where nothing can be deleted the swipe still has to be
                        // swallowed: a horizontal drag nothing consumes ends as a
                        // tap, and a refused swipe would open the very row it was
                        // trying to dismiss.
                        Modifier.swallowHorizontalDrags()
                    }
                )
        ) {
            content()
        }
    }
}

// Consumed in the Initial pass, before the row underneath can read the gesture
// as a press it should act on.
private fun Modifier.swallowHorizontalDrags(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var travelled = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: break
            if (change.changedToUpIgnoreConsumed()) break
            travelled += change.positionChangeIgnoreConsumed().x
            if (abs(travelled) > viewConfiguration.touchSlop) change.consume()
        }
    }
}
