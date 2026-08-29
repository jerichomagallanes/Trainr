package com.jericx.trainr.presentation.common.components.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.theme.Animation
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun TrainrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.97f,
        animationSpec = tween(
            durationMillis = Animation.DurationShort,
            easing = FastOutSlowInEasing
        ),
        label = "buttonScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            !enabled && isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            !enabled && !isPrimary -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            isPrimary -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(
            durationMillis = Animation.DurationShort,
            easing = FastOutSlowInEasing
        ),
        label = "buttonColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            !enabled && isPrimary -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            !enabled && !isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            isPrimary -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(
            durationMillis = Animation.DurationShort,
            easing = FastOutSlowInEasing
        ),
        label = "textColor"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (enabled && isPrimary) 4.dp else 0.dp,
        animationSpec = tween(
            durationMillis = Animation.DurationShort,
            easing = FastOutSlowInEasing
        ),
        label = "shadowElevation"
    )

    Box(
        modifier = modifier
            .scale(buttonScale)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentHeight.Large)
                .shadow(
                    elevation = shadowElevation,
                    shape = MaterialTheme.shapes.medium,
                    spotColor = if (isPrimary)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        Color.Black.copy(alpha = 0.05f)
                ),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                disabledContainerColor = buttonColor
            ),
            border = if (isPrimary) {
                null
            } else {
                BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
            },
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrButtonPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrButton(text = "CONTINUE", onClick = {})
            TrainrButton(text = "CONTINUE", onClick = {}, enabled = false)
            TrainrButton(text = "SKIP", onClick = {}, isPrimary = false)
        }
    }
}
