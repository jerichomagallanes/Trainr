package com.jericx.trainr.presentation.unstuck.feedback

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

// How an adjustment fitted one session and what the training data can support
// are different claims, and this screen never lets the first stand in for the
// second: no score, no percentage, no comparison across exercises (C12).
@Composable
fun FeedbackOutcomeScreen(
    state: AdjustmentFeedbackUiState,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    onOpenGuidance: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            TrainrButton(text = stringResource(R.string.done), onClick = onDone)
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.feedback_saved),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium)
                    .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
                    .padding(Spacing.card)
            ) {
                Text(
                    text = stringResource(R.string.how_it_fit),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(state.answer.sentenceRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(top = Spacing.extraSmall)
                )
            }

            if (state.offersGuidance) {
                TrainrTextAction(
                    text = stringResource(R.string.view_exercise_guidance),
                    onClick = onOpenGuidance,
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }

            Text(
                text = stringResource(R.string.progress_toward_goal),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.large)
            )
            Text(
                text = stringResource(
                    R.string.progress_need_more_format,
                    stringResource(state.trendLabelRes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.small)
            )
            Text(
                text = stringResource(R.string.progress_one_session_caveat),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )

            Text(
                text = stringResource(R.string.future_workouts_unchanged),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium)
                    .background(colors.surfaceSunken, MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = Spacing.tight)
            )
        }
    }
}

@StringRes
private fun FeedbackAnswer?.sentenceRes(): Int = when (this) {
    FeedbackAnswer.HELPED -> R.string.outcome_helped
    FeedbackAnswer.STILL_TOO_LONG -> R.string.outcome_still_too_long
    FeedbackAnswer.EXERCISE_CONFUSING -> R.string.outcome_confusing
    FeedbackAnswer.DISCOMFORT -> R.string.outcome_discomfort
    else -> R.string.outcome_something_else
}

@Preview(showBackground = true)
@Composable
private fun FeedbackOutcomeHelpedPreview() {
    TrainrTheme {
        FeedbackOutcomeScreen(state = SampleFeedbackStates.helped)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeedbackOutcomeConfusingPreview() {
    TrainrTheme(darkTheme = true) {
        FeedbackOutcomeScreen(state = SampleFeedbackStates.confusing)
    }
}
