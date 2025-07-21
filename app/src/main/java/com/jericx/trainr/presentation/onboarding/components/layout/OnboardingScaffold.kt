package com.jericx.trainr.presentation.onboarding.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScaffold(
    onBackClick: (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {
        if (onBackClick != null) {
            OnboardingTopBar(onBackClick = onBackClick)
        }
    },
    bottomButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.navigationBarsPadding(),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(Spacing.large)) {
                    bottomButton()
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.statusBarsPadding()
    ) { paddingValues ->
        content(paddingValues)
    }
}