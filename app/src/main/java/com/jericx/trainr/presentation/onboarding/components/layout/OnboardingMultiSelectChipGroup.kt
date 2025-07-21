package com.jericx.trainr.presentation.onboarding.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing

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