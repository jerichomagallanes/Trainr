package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun PreferencesRoute(
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Nothing is drawn until what is stored has been read, so an empty list is
    // never shown as an answer.
    if (state.isLoaded) {
        PreferencesScreen(
            state = state,
            modifier = modifier,
            onEdit = onEdit,
            onForget = viewModel::forget,
            onDeleteNote = viewModel::deleteNote,
            onBack = onBack,
            onDone = onDone
        )
    }
}

@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit = {},
    onForget: (Long) -> Unit = {},
    onDeleteNote: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            TrainrButton(
                text = stringResource(R.string.back_to_workout_plan),
                onClick = onDone
            )
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.your_training_preferences),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.things_you_asked_to_remember),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )

            if (state.hasForgotten) {
                Text(
                    text = stringResource(R.string.preference_forgotten),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            if (state.preferences.isEmpty()) {
                SunkenBox(modifier = Modifier.padding(top = Spacing.medium)) {
                    Text(
                        text = stringResource(R.string.no_preferences_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(top = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.tight)
                ) {
                    state.preferences.forEach { preference ->
                        PreferenceCard(
                            preference = preference,
                            onEdit = { onEdit(preference.id) },
                            onForget = { onForget(preference.id) }
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.forget_does_not_remove_workouts),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )

            if (state.notes.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.tight)
                ) {
                    state.notes.forEach { note ->
                        NoteCard(note = note, onDelete = { onDeleteNote(note.id) })
                    }
                }
            }

            Text(
                text = stringResource(R.string.todays_adjustment),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.large)
            )
            Text(
                text = stringResource(
                    when (state.todayAdjustment) {
                        TodayAdjustmentKind.SHORTER -> R.string.adjustment_shorter_today
                        TodayAdjustmentKind.ALTERNATIVE -> R.string.adjustment_alternative_today
                        null -> R.string.no_adjustment_applied
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.extraSmall)
            )
        }
    }
}

@Composable
private fun PreferenceCard(
    preference: PreferenceCardUi,
    onEdit: () -> Unit,
    onForget: () -> Unit
) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .padding(Spacing.card)
    ) {
        Text(
            text = stringResource(R.string.weekday_time_limit_format, preference.weekdayName),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
        Text(
            text = pluralStringResource(
                R.plurals.minutes_for_whole_session_format,
                preference.minutes,
                preference.minutes
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(top = Spacing.extraSmall)
        )
        Text(
            text = stringResource(R.string.confirmed_by_you_format, preference.confirmedOn),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
            modifier = Modifier.padding(top = Spacing.extraSmall)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            TrainrTextAction(text = stringResource(R.string.edit), onClick = onEdit)
            TrainrTextAction(text = stringResource(R.string.forget), onClick = onForget)
        }
    }
}

@Composable
private fun NoteCard(note: SessionNote, onDelete: () -> Unit) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .padding(Spacing.card)
    ) {
        Text(
            text = stringResource(R.string.your_session_note),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
        // Raw text the person owns: shown as written and never interpreted.
        Text(
            text = note.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(top = Spacing.extraSmall)
        )
        TrainrTextAction(text = stringResource(R.string.delete_note), onClick = onDelete)
    }
}

@Composable
private fun SunkenBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.trainrColors.surfaceSunken, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreferencesScreenPreview() {
    TrainrTheme {
        PreferencesScreen(state = SamplePreferenceStates.filled)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreferencesScreenEmptyPreview() {
    TrainrTheme(darkTheme = true) {
        PreferencesScreen(state = SamplePreferenceStates.empty)
    }
}
