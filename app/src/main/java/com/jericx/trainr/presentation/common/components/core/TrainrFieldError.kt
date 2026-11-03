package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

// Null draws nothing, so an untouched field stays quiet rather than opening
// the form already complaining.
@Composable
fun TrainrFieldError(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return

    // Edge to edge the keyboard never resizes the window, so a message
    // explaining a disabled button is laid out behind it. The request waits a
    // frame because the text is not placed yet when the effect first runs.
    val requester = remember { BringIntoViewRequester() }
    val keyboard = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(keyboard) {
        withFrameNanos {}
        requester.bringIntoView()
    }

    Spacer(modifier = Modifier.height(Spacing.small))
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.trainrColors.dangerInk,
        modifier = modifier.bringIntoViewRequester(requester)
    )
}

// Touched means the field has been left, not that it is being filled in, so
// nothing complains mid-typing.
@Composable
fun Modifier.touchedOnBlur(onTouched: () -> Unit): Modifier {
    var everFocused by remember { mutableStateOf(false) }

    return onFocusChanged { state ->
        if (state.isFocused) {
            everFocused = true
        } else if (everFocused) {
            onTouched()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrFieldErrorPreview() {
    TrainrTheme {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            TrainrFieldError(message = "Enter an age between 13 and 100")
        }
    }
}
