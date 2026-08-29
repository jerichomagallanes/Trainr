package com.jericx.trainr.presentation.onboarding.components.typography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun OnboardingSubtitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 14.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingSubtitlePreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            OnboardingSubtitle(text = "This helps us calculate your fitness metrics.")
        }
    }
}
