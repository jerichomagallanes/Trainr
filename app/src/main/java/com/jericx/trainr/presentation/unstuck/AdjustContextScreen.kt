package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.common.components.core.TrainrOptionRow
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextArea
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun AdjustContextScreen(
    note: String,
    modifier: Modifier = Modifier,
    onTypeNote: (String) -> Unit = {},
    onChoose: (DirectReason) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            TrainrQuietButton(
                text = stringResource(R.string.back_to_workout),
                onClick = onBack
            )
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.context_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.context_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            TrainrTextArea(
                value = note,
                onValueChange = onTypeNote,
                placeholder = stringResource(R.string.context_placeholder),
                modifier = Modifier.padding(top = Spacing.small)
            )
            Text(
                text = stringResource(R.string.context_private),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )

            Text(
                text = stringResource(R.string.context_which_first),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.large)
            )
            Column(
                modifier = Modifier.padding(top = Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                TrainrOptionRow(
                    title = stringResource(R.string.context_option_time),
                    description = stringResource(R.string.context_option_time_hint),
                    onClick = { onChoose(DirectReason.LESS_TIME) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.context_option_equipment),
                    description = stringResource(R.string.context_option_equipment_hint),
                    onClick = { onChoose(DirectReason.EQUIPMENT) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustContextScreenPreview() {
    TrainrTheme {
        AdjustContextScreen(note = "")
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustContextScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        AdjustContextScreen(note = "Gym is busy tonight.")
    }
}
