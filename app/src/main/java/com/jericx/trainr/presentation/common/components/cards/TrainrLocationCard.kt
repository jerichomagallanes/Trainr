package com.jericx.trainr.presentation.common.components.cards

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrRadioDot
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

private val CardHeight = 124.dp
private val IconSize = 35.dp

@Composable
fun TrainrLocationCard(
    text: String,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Card(
        modifier = modifier
            .height(CardHeight)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.tight)
        ) {
            TrainrRadioDot(
                selected = isSelected,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = contentColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrLocationCardPreview() {
    TrainrTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrLocationCard(
                text = "Home",
                iconRes = R.drawable.ic_house,
                isSelected = true,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            TrainrLocationCard(
                text = "Gym",
                iconRes = R.drawable.ic_fitness_center,
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            TrainrLocationCard(
                text = "Both",
                iconRes = R.drawable.ic_sync_alt,
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}
