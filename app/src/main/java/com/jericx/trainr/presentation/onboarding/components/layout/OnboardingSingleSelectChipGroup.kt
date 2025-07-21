package com.jericx.trainr.presentation.onboarding.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing

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