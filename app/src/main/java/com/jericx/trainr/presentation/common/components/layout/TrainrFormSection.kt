package com.jericx.trainr.presentation.common.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.components.core.TrainrTextField
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle

@Composable
fun TrainrFormSection(
    title: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = Spacing.medium,
    titleGap: Dp = Spacing.small,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(vertical = verticalPadding)) {
        TrainrSectionTitle(text = title)
        Spacer(modifier = Modifier.height(titleGap))
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrFormSectionPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            TrainrFormSection(title = "Height (cm)") {
                TrainrTextField(value = "170", onValueChange = {}, placeholder = "170")
            }
        }
    }
}
