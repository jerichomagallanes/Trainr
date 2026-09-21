package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

// Reads the day's note through the debrief's own view model: this route
// carries the same dayNumber and weekNumber arguments, so it resolves the note
// that was just written rather than passing free text through a route.
@Composable
fun NoteSavedRoute(
    modifier: Modifier = Modifier,
    onViewPreferences: () -> Unit = {},
    onDone: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: DebriefViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()

    NoteSavedScreen(
        note = note,
        modifier = modifier,
        onViewPreferences = onViewPreferences,
        onDone = onDone,
        onBack = onBack
    )
}

@Composable
fun NoteSavedScreen(
    note: String,
    modifier: Modifier = Modifier,
    onViewPreferences: () -> Unit = {},
    onDone: () -> Unit = {},
    onBack: () -> Unit = {}
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
                text = stringResource(R.string.note_saved),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )

            // A plain Text: the note is the person's own words and is never
            // parsed, so anything that looks like markup stays literal.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium)
                    .background(colors.surfaceSunken, MaterialTheme.shapes.small)
                    .padding(12.dp)
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }

            Text(
                text = stringResource(R.string.next_workout_unchanged),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.medium)
            )

            TrainrTextAction(
                text = stringResource(R.string.view_training_preferences),
                onClick = onViewPreferences,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteSavedScreenPreview() {
    TrainrTheme {
        NoteSavedScreen(note = "I had to leave early for work.")
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoteSavedScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        NoteSavedScreen(note = "Gym was busy, skipped the last two sets.")
    }
}
