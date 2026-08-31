package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import com.jericx.trainr.presentation.common.theme.RedError
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

// Why a value was not accepted, under the field it belongs to. Null draws
// nothing, so a caller can pass its check straight in and an untouched field
// stays quiet: a form that opens already complaining has told the client they
// are wrong before they have done anything.
@Composable
fun TrainrFieldError(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return

    Spacer(modifier = Modifier.height(Spacing.small))
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = RedError,
        modifier = modifier
    )
}

// A field has been "touched" once it has been left, not while it is being
// filled in. Complaining that something is required while the client is still
// on their way to typing it is the form arguing with them mid-sentence.
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
