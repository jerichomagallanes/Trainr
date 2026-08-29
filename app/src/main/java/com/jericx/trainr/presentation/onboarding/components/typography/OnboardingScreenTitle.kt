package com.jericx.trainr.presentation.onboarding.components.typography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun OnboardingScreenTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenTitlePreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            OnboardingScreenTitle(text = "YOUR MEASUREMENTS")
        }
    }
}
