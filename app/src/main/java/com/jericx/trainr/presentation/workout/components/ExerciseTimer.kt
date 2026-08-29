package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrPillButton
import com.jericx.trainr.presentation.common.theme.Orange500
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TextMuted
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.ExerciseTimerUi

@Composable
fun ExerciseTimer(
    timer: ExerciseTimerUi?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (timer == null) {
        TrainrPillButton(
            text = stringResource(R.string.start_timer),
            iconRes = R.drawable.ic_play_arrow,
            onClick = onStart,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(2.dp, Orange500, MaterialTheme.shapes.medium)
                .padding(vertical = Spacing.card),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Text(
                text = timer.display,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Orange500
            )
            Text(
                text = stringResource(
                    if (timer.isRunning) R.string.exercise_in_progress
                    else R.string.timer_paused
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
            if (timer.isRunning) {
                TrainrPillButton(
                    text = stringResource(R.string.pause_timer),
                    iconRes = R.drawable.ic_pause,
                    onClick = onPause
                )
            } else {
                TrainrPillButton(
                    text = stringResource(R.string.resume_timer),
                    iconRes = R.drawable.ic_play_arrow,
                    onClick = onResume
                )
            }

            TrainrPillButton(
                text = stringResource(R.string.stop_timer),
                iconRes = R.drawable.ic_stop,
                onClick = onStop,
                filled = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseTimerPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.screen),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            ExerciseTimer(
                timer = null,
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {}
            )
            ExerciseTimer(
                timer = ExerciseTimerUi(position = 2, remainingSeconds = 58, isRunning = true),
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {}
            )
            ExerciseTimer(
                timer = ExerciseTimerUi(position = 4, remainingSeconds = 58, isRunning = false),
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {}
            )
        }
    }
}
