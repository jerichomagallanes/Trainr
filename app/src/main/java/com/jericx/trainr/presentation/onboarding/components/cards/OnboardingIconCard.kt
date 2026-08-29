package com.jericx.trainr.presentation.onboarding.components.cards

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun OnboardingIconCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        border = if (!isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (isSelected)
                        MaterialTheme.colorScheme.background
                    else
                        MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.background
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.background
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingIconCardPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            OnboardingIconCard(
                iconRes = R.drawable.ic_fitness_center,
                title = "Muscle Gain",
                description = "Hypertrophy and mass-building programs",
                isSelected = true,
                onClick = {}
            )
            OnboardingIconCard(
                iconRes = R.drawable.ic_directions_run,
                title = "Endurance",
                description = "Cardiovascular and stamina improvement",
                isSelected = false,
                onClick = {}
            )
        }
    }
}
