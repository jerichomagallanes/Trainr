package com.jericx.trainr.presentation.onboarding.components.typography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun OnboardingSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingSectionTitlePreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            OnboardingSectionTitle(text = "Available equipment")
        }
    }
}
