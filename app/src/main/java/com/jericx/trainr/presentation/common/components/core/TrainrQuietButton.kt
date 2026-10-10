package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun TrainrQuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ComponentHeight.Medium)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.trainrColors.onSurfaceMuted
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrQuietButtonPreview() {
    TrainrTheme {
        TrainrQuietButton(text = "Keep today's plan", onClick = {})
    }
}
