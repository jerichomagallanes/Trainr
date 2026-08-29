package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSubtitle

@Composable
fun TrainrScreenContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.large)
            .padding(vertical = Spacing.medium),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
private fun TrainrScreenContentPreview() {
    TrainrTheme {
        TrainrScreenContent {
            TrainrScreenTitle(text = "YOUR MEASUREMENTS")
            TrainrSubtitle(text = "This helps us calculate your fitness metrics.")
        }
    }
}
