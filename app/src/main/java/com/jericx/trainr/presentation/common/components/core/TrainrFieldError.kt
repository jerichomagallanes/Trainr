package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Preview(showBackground = true)
@Composable
private fun TrainrFieldErrorPreview() {
    TrainrTheme {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            TrainrFieldError(message = "Enter an age between 13 and 100")
        }
    }
}
