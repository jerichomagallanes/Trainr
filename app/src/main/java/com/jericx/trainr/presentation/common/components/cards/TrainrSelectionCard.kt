package com.jericx.trainr.presentation.common.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.components.core.TrainrRadioDot
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TextMuted
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun TrainrSelectionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.onBackground
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.tight)
        ) {
            TrainrRadioDot(
                selected = isSelected,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier.padding(
                    start = 5.dp,
                    top = 5.dp,
                    bottom = 5.dp,
                    end = Spacing.extraLarge
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.background
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.background
                        else
                            TextMuted
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrSelectionCardPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.card),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrSelectionCard(
                title = "Intermediate",
                description = "Working out regularly for 6+ months.",
                isSelected = true,
                onClick = {}
            )
            TrainrSelectionCard(
                title = "Beginner",
                description = "New to working out or getting back into it.",
                isSelected = false,
                onClick = {}
            )
        }
    }
}
