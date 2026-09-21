package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

// Nothing here reads the gate, the interpreter or the policy: pain is a free
// route with no substitute and no clearance to continue (C09).
@Composable
fun AdjustPainScreen(
    modifier: Modifier = Modifier,
    onSaveAndFinishEarly: () -> Unit = {},
    onReturn: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                TrainrButton(
                    text = stringResource(R.string.save_and_finish_early),
                    onClick = onSaveAndFinishEarly
                )
                TrainrQuietButton(
                    text = stringResource(R.string.return_to_workout),
                    onClick = onReturn
                )
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.pain_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.pain_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.small)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium)
                    .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
                    .padding(Spacing.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.pain_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(R.string.pain_card_body_1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(R.string.pain_card_body_2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }

            Text(
                text = stringResource(R.string.pain_save_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.medium)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustPainScreenPreview() {
    TrainrTheme {
        AdjustPainScreen()
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustPainScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        AdjustPainScreen()
    }
}
