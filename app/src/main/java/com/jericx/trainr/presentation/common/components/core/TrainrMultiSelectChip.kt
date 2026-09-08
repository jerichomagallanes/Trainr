package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun TrainrMultiSelectChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.trainrColors

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.surfaceSelected,
            containerColor = colors.surfaceCard,
            selectedLabelColor = colors.onSurfaceSelected,
            labelColor = colors.onSurface,
            selectedLeadingIconColor = colors.onSurfaceSelected,
            iconColor = colors.onSurfaceMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = colors.outlineControl,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun TrainrMultiSelectChipPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrMultiSelectChip(
                selected = true,
                onClick = {},
                label = { Text("Yoga Mat") }
            )
            TrainrMultiSelectChip(
                selected = false,
                onClick = {},
                label = { Text("Treadmill") }
            )
        }
    }
}
