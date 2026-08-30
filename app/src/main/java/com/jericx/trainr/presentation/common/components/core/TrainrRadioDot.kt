package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The design's 20px radio, drawn rather than Material's RadioButton: the M3
// widget carries a 48dp touch target that shoves the visible dot away from
// the corner the frames pin it to.
@Composable
fun TrainrRadioDot(
    selected: Boolean,
    modifier: Modifier = Modifier,
    color: Color = if (selected) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.outline
    }
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .border(width = 2.dp, color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
