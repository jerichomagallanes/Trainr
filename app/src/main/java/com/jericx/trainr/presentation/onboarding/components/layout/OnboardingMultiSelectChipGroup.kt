package com.jericx.trainr.presentation.onboarding.components.layout

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

@Composable
fun OnboardingMultiSelectChipGroup(
    items: List<String>,
    selectedItems: Set<String>,
    onItemToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Spacing.small,
    verticalSpacing: Dp = Spacing.small
) {
    OnboardingChipGroup(
        items = items,
        selectedItems = selectedItems,
        onItemClick = onItemToggle,
        itemLabel = { it },
        modifier = modifier,
        multiSelect = true,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingMultiSelectChipGroupPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            val selected = remember { mutableStateOf(setOf("Yoga Mat")) }
            OnboardingMultiSelectChipGroup(
                items = listOf("Dumbells", "Yoga Mat", "Treadmill"),
                selectedItems = selected.value,
                onItemToggle = { item ->
                    selected.value = if (item in selected.value) {
                        selected.value - item
                    } else {
                        selected.value + item
                    }
                }
            )
        }
    }
}
