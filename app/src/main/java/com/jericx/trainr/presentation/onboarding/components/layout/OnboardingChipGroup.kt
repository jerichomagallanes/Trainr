package com.jericx.trainr.presentation.onboarding.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingToggleChip

@Composable
fun <T> OnboardingChipGroup(
    items: List<T>,
    selectedItem: T? = null,
    selectedItems: Set<T>? = null,
    onItemClick: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    horizontalSpacing: Dp = Spacing.small,
    verticalSpacing: Dp = Spacing.small
) {
    OnboardingFlowRow(
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

            OnboardingToggleChip(
                text = itemLabel(item),
                selected = isSelected,
                onClick = { onItemClick(item) }
            )
        }
    }
}
