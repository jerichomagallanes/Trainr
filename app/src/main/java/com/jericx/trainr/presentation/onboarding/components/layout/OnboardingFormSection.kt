package com.jericx.trainr.presentation.onboarding.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingSectionTitle

@Composable
fun OnboardingFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(vertical = Spacing.medium)) {
        OnboardingSectionTitle(text = title)
        Spacer(modifier = Modifier.height(Spacing.small))
        content()
    }
}