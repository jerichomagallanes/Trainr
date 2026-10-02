package com.jericx.trainr.presentation.unstuck.feedback

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
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.presentation.common.components.core.TrainrOptionRow
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun AdjustmentFeedbackScreen(
    state: AdjustmentFeedbackUiState,
    modifier: Modifier = Modifier,
    onAnswer: (FeedbackAnswer) -> Unit = {},
    onNotQuite: () -> Unit = {},
    onDiscomfort: () -> Unit = {},
    onNotNow: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            TrainrQuietButton(text = stringResource(R.string.not_now), onClick = onNotNow)
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.your_workout_is_saved),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
            Text(
                text = stringResource(R.string.did_the_adjustment_help),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.extraSmall)
            )
            Text(
                text = if (state.isReplacement) {
                    stringResource(
                        R.string.feedback_equipment_body_format,
                        state.substituteName,
                        state.originalName
                    )
                } else {
                    stringResource(R.string.feedback_time_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.small)
            )

            Column(
                modifier = Modifier.padding(top = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                TrainrOptionRow(
                    title = stringResource(
                        if (state.isReplacement) {
                            R.string.feedback_yes_equipment
                        } else {
                            R.string.feedback_yes_time
                        }
                    ),
                    description = null,
                    onClick = { onAnswer(FeedbackAnswer.HELPED) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.feedback_not_quite),
                    description = stringResource(R.string.feedback_not_quite_hint),
                    onClick = onNotQuite
                )
                TrainrOptionRow(
                    title = stringResource(R.string.feedback_discomfort),
                    description = stringResource(R.string.feedback_discomfort_hint),
                    onClick = onDiscomfort
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustmentFeedbackTimePreview() {
    TrainrTheme {
        AdjustmentFeedbackScreen(state = SampleFeedbackStates.time)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustmentFeedbackEquipmentPreview() {
    TrainrTheme(darkTheme = true) {
        AdjustmentFeedbackScreen(state = SampleFeedbackStates.equipment)
    }
}
