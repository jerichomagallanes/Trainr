package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.themedPainter
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.unstuck.feedback.FeedbackPromptViewModel

@Composable
fun SessionSavedRoute(
    performedExercises: Int,
    plannedExercises: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onFeedback: (Long) -> Unit = {},
    viewModel: FeedbackPromptViewModel = hiltViewModel()
) {
    val pending by viewModel.pendingAdjustmentId.collectAsStateWithLifecycle()

    SessionSavedScreen(
        performedExercises = performedExercises,
        plannedExercises = plannedExercises,
        modifier = modifier,
        onBackClick = onBackClick,
        onDoneClick = onDoneClick,
        extra = {
            pending?.let { adjustmentId ->
                AnythingToChangeCard(onFeedback = { onFeedback(adjustmentId) })
            }
        }
    )
}

@Composable
fun SessionSavedScreen(
    performedExercises: Int,
    plannedExercises: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    extra: @Composable () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        TrainrTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Spacing.section * 2))

            Image(
                painter = themedPainter(R.drawable.img_task_done, R.drawable.img_task_done_night),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = stringResource(R.string.workout_saved),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.trainrColors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.screen)
            )

            Text(
                text = pluralStringResource(
                    R.plurals.finished_early_summary_format,
                    plannedExercises,
                    performedExercises,
                    plannedExercises
                ),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = MaterialTheme.trainrColors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.card)
            )

            extra()
        }

        TrainrButton(
            text = stringResource(R.string.done),
            onClick = onDoneClick,
            modifier = Modifier.padding(
                horizontal = Spacing.screen,
                vertical = Spacing.section * 2
            )
        )
    }
}

@Composable
private fun AnythingToChangeCard(onFeedback: () -> Unit) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.screen)
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .padding(Spacing.card)
    ) {
        Text(
            text = stringResource(R.string.anything_to_change_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
        Text(
            text = stringResource(R.string.anything_to_change_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            modifier = Modifier.padding(top = Spacing.extraSmall)
        )
        TrainrTextAction(
            text = stringResource(R.string.tell_us_how_it_went),
            onClick = onFeedback,
            modifier = Modifier.padding(top = Spacing.small)
        )
    }
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun SessionSavedScreenPreview() {
    TrainrTheme {
        SessionSavedScreen(performedExercises = 4, plannedExercises = 6)
    }
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun SessionSavedWithFeedbackOfferPreview() {
    TrainrTheme {
        SessionSavedScreen(
            performedExercises = 4,
            plannedExercises = 6,
            extra = { AnythingToChangeCard(onFeedback = {}) }
        )
    }
}
