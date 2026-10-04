package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainrScaffold(
    onBackClick: (() -> Unit)? = null,
    closeInsteadOfBack: Boolean = false,
    topBar: @Composable () -> Unit = {
        if (onBackClick != null) {
            TrainrTopBar(onBackClick = onBackClick, closeInsteadOfBack = closeInsteadOfBack)
        }
    },
    bottomButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val raisedEdge = MaterialTheme.trainrColors.raisedEdge

    Scaffold(
        topBar = topBar,
        bottomBar = {
            // A bar that holds more than a button grows with the text setting
            // until the screen it belongs to is a strip. It is held to a share
            // of what the screen has and scrolls inside that.
            BoxWithConstraints {
                val ceiling = maxHeight * PINNED_SHARE
                Surface(
                    color = MaterialTheme.trainrColors.surfaceRaised,
                    modifier = Modifier
                        .navigationBarsPadding()
                        // Edge to edge the window is never resized, so without this
                        // the keyboard covers the button the screen is asking for.
                        .imePadding()
                        .drawWithContent {
                            drawContent()
                            val stroke = 1.dp.toPx()
                            drawLine(
                                color = raisedEdge,
                                start = Offset(0f, stroke / 2f),
                                end = Offset(size.width, stroke / 2f),
                                strokeWidth = stroke
                            )
                        },
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = ceiling)
                            .padding(Spacing.large)
                            .verticalScroll(rememberScrollState())
                    ) {
                        bottomButton()
                    }
                }
            }
        },
        containerColor = MaterialTheme.trainrColors.surfacePage,
        modifier = Modifier.statusBarsPadding()
    ) { paddingValues ->
        content(paddingValues)
    }
}

private const val PINNED_SHARE = 0.34f

@Preview(showBackground = true)
@Composable
private fun TrainrScaffoldPreview() {
    TrainrTheme {
        TrainrScaffold(
            onBackClick = {},
            bottomButton = { TrainrButton(text = "NEXT", onClick = {}) }
        ) { padding ->
            TrainrScreenContent(modifier = Modifier.padding(padding)) {
                TrainrScreenTitle(text = "YOUR MEASUREMENTS")
            }
        }
    }
}
