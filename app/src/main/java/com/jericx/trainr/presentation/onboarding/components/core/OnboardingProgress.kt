package com.jericx.trainr.presentation.onboarding.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun OnboardingProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(OutlineGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(currentStep.toFloat() / totalSteps)
                .background(MaterialTheme.colorScheme.onBackground)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingProgressPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            OnboardingProgress(currentStep = 1, totalSteps = 7)
            OnboardingProgress(currentStep = 4, totalSteps = 7)
            OnboardingProgress(currentStep = 7, totalSteps = 7)
        }
    }
}
