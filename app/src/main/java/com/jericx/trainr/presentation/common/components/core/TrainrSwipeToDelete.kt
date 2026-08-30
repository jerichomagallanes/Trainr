package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.jericx.trainr.presentation.common.theme.RedError
import com.jericx.trainr.presentation.common.theme.Spacing

// Dragging a row aside to reveal a delete behind it. What the swipe means is
// the caller's business — a set goes at once, a week is asked about first — so
// the state is passed in and only the reveal itself lives here.
@Composable
fun TrainrSwipeToDelete(
    state: SwipeToDismissBoxState,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Shown for exactly as long as the row is held aside.
            // dismissDirection reads the offset, so it is Settled at rest and
            // nothing bleeds through an idle row. Pairing it with a progress
            // check took the red away mid-gesture instead: progress runs to 1
            // as the swipe nears its anchor, which is when the delete most
            // needs to be visible.
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
        }
    ) {
        // The row itself is transparent, so it is given a backdrop here or the
        // red shows through the very thing it is sliding out from behind.
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}
