package com.jericx.trainr.presentation.onboarding.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing

@Composable
fun OnboardingFlowRow(
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