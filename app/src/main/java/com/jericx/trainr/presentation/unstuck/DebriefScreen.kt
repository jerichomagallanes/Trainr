package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextArea
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun DebriefRoute(
    modifier: Modifier = Modifier,
    onSaved: () -> Unit = {},
    onSkip: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: DebriefViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.savedEvents.collect { onSaved() } }

    DebriefScreen(
        note = note,
        modifier = modifier,
        onTypeNote = viewModel::typeNote,
        onSave = viewModel::save,
        onSkip = onSkip,
        onBack = onBack
    )
}

@Composable
fun DebriefScreen(
    note: String,
    modifier: Modifier = Modifier,
    onTypeNote: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onSkip: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                TrainrButton(
                    text = stringResource(R.string.save_note),
                    onClick = onSave,
                    enabled = note.isNotBlank()
                )
                TrainrQuietButton(
                    text = stringResource(R.string.skip),
                    onClick = onSkip
                )
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.debrief_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.your_note),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            TrainrTextArea(
                value = note,
                onValueChange = onTypeNote,
                placeholder = stringResource(R.string.debrief_placeholder),
                modifier = Modifier.padding(top = Spacing.small)
            )
            Text(
                text = stringResource(R.string.notes_stay_on_device),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DebriefScreenPreview() {
    TrainrTheme {
        DebriefScreen(note = "")
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DebriefScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        DebriefScreen(note = "I had to leave early for work.")
    }
}
