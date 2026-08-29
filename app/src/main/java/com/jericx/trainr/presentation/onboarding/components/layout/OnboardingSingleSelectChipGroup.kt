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
fun OnboardingSingleSelectChipGroup(
    items: List<String>,
    selectedItem: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Spacing.small,
    verticalSpacing: Dp = Spacing.small
) {
    OnboardingChipGroup(
        items = items,
        selectedItem = selectedItem,
        onItemClick = onItemClick,
        itemLabel = { it },
        modifier = modifier,
        multiSelect = false,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingSingleSelectChipGroupPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            val selected = remember { mutableStateOf("Gym") }
            OnboardingSingleSelectChipGroup(
                items = listOf("Home", "Gym", "Both"),
                selectedItem = selected.value,
                onItemClick = { selected.value = it }
            )
        }
    }
}
