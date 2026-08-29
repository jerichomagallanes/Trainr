package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.components.core.TrainrToggleChip

@Composable
fun <T> TrainrChipGroup(
    items: List<T>,
    onItemClick: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    selectedItem: T? = null,
    selectedItems: Set<T>? = null,
    multiSelect: Boolean = false,
    horizontalSpacing: Dp = Spacing.small,
    verticalSpacing: Dp = Spacing.small
) {
    TrainrFlowRow(
        modifier = modifier,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing
    ) {
        items.forEach { item ->
            val isSelected = if (multiSelect) {
                selectedItems?.contains(item) == true
            } else {
                selectedItem == item
            }

            TrainrToggleChip(
                text = itemLabel(item),
                selected = isSelected,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrChipGroupPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            val selected = remember { mutableStateOf("Gym") }
            TrainrChipGroup(
                items = listOf("Home", "Gym", "Both"),
                onItemClick = { selected.value = it },
                itemLabel = { it },
                selectedItem = selected.value
            )
        }
    }
}
