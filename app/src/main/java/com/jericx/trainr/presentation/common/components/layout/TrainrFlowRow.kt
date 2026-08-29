package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.components.core.TrainrRadioChip

@Composable
fun TrainrFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Spacing.small,
    verticalSpacing: Dp = Spacing.small,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width + horizontalSpacing.roundToPx()
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        } + (rows.size - 1) * verticalSpacing.roundToPx()

        layout(constraints.maxWidth, height) {
            var yPosition = 0

            rows.forEach { row ->
                var xPosition = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0

                row.forEach { placeable ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width + horizontalSpacing.roundToPx()
                }

                yPosition += rowHeight + verticalSpacing.roundToPx()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrFlowRowPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrFlowRow {
                listOf("Dumbells", "Yoga Mat", "Treadmill", "Barbell").forEach {
                    TrainrRadioChip(text = it, selected = it == "Yoga Mat", onClick = {})
                }
            }
        }
    }
}
