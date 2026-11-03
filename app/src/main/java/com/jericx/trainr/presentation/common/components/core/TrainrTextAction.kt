package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun TrainrTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.trainrColors.brandStrong,
        modifier = modifier
            .heightIn(min = ComponentHeight.Medium)
            .clickable(role = Role.Button, onClick = onClick)
    )
}
