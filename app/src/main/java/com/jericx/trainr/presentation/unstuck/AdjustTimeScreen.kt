package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrFieldError
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextField
import com.jericx.trainr.presentation.common.components.core.TrainrToggleChip
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun AdjustTimeScreen(
    state: AdjustmentUiState,
    modifier: Modifier = Modifier,
    onSelectMinutes: (Int) -> Unit = {},
    onTypeMinutes: (String) -> Unit = {},
    onShowRecommendation: () -> Unit = {},
    onKeepPlan: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                TrainrButton(
                    text = stringResource(R.string.show_recommendation),
                    onClick = onShowRecommendation,
                    enabled = state.canShowRecommendation
                )
                TrainrQuietButton(
                    text = stringResource(R.string.keep_todays_plan),
                    onClick = onKeepPlan
                )
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.adjust_time_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(
                    if (state.scope == TimeScope.REMAINING) {
                        R.string.adjust_time_remaining
                    } else {
                        R.string.adjust_time_whole_session
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                state.presets.forEach { preset ->
                    TrainrToggleChip(
                        text = stringResource(R.string.minutes_short_format, preset),
                        selected = state.isPresetSelected && state.selectedMinutes == preset,
                        onClick = { onSelectMinutes(preset) },
                        modifier = Modifier.weight(1f),
                        height = ComponentHeight.Medium,
                        horizontalPadding = Spacing.small
                    )
                }
            }

            Text(
                text = stringResource(R.string.adjust_time_other),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            TrainrTextField(
                value = state.customMinutesText,
                onValueChange = onTypeMinutes,
                placeholder = stringResource(R.string.adjust_time_other),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(top = Spacing.small)
            )
            if (state.minutesError) {
                TrainrFieldError(message = stringResource(R.string.adjust_time_range_error))
            }

            Text(
                text = stringResource(R.string.your_priority),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.large)
            )
            Text(
                text = stringResource(state.goalLabelRes),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.adjust_time_promise),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustTimeScreenPreview() {
    TrainrTheme {
        AdjustTimeScreen(state = SampleAdjustmentStates.time)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustTimeScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        AdjustTimeScreen(state = SampleAdjustmentStates.timeWithError)
    }
}
