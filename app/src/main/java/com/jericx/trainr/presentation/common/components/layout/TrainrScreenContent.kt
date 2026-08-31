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
    // The vertical padding insets the scrolling area rather than sitting inside
    // it. Applied after verticalScroll it was part of the content and scrolled
    // away with it, so a scrolled screen butted its text straight against the
    // progress bar above with nothing between them.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = Spacing.medium)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.large),
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
