package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
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
fun EditPreferenceRoute(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: EditPreferenceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.savedEvents.collect { onFinished() } }

    // The stored limit is the starting answer, so nothing is drawn until it
    // has been read and the field cannot flash an empty value.
    if (state.isLoaded) {
        EditPreferenceScreen(
            state = state,
            modifier = modifier,
            onSelectMinutes = viewModel::selectMinutes,
            onTypeMinutes = viewModel::typeMinutes,
            onSave = viewModel::save,
            onCancel = onFinished,
            onBack = onBack
        )
    }
}

@Composable
fun EditPreferenceScreen(
    state: EditPreferenceUiState,
    modifier: Modifier = Modifier,
    onSelectMinutes: (Int) -> Unit = {},
    onTypeMinutes: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                TrainrButton(
                    text = stringResource(R.string.save),
                    onClick = onSave,
                    enabled = state.canSave
                )
                TrainrQuietButton(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel
                )
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.weekday_time_limit_format, state.weekdayName),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.time_for_whole_workout),
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
                text = stringResource(R.string.edit_applies_to_future),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.large)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditPreferenceScreenPreview() {
    TrainrTheme {
        EditPreferenceScreen(state = SamplePreferenceStates.editing)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditPreferenceScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        EditPreferenceScreen(state = SamplePreferenceStates.editingWithError)
    }
}
