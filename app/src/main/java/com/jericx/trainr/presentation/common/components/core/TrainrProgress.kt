package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun TrainrProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            // Without this nothing announces setup progress to a screen reader.
            .progressSemantics(
                value = currentStep.toFloat(),
                valueRange = 0f..totalSteps.toFloat()
            )
            .fillMaxWidth()
            .height(8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.trainrColors.trackEmpty)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(currentStep.toFloat() / totalSteps)
                .background(MaterialTheme.trainrColors.surfaceSelected)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrProgressPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrProgress(currentStep = 1, totalSteps = 7)
            TrainrProgress(currentStep = 4, totalSteps = 7)
            TrainrProgress(currentStep = 7, totalSteps = 7)
        }
    }
}
