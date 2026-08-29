package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.ExerciseStep
import com.jericx.trainr.presentation.workout.model.color
import com.jericx.trainr.presentation.workout.model.stepsFor
import com.jericx.trainr.presentation.workout.sample.SampleRoutine

private val StepSize = 20.dp
private val TrackHeight = 5.dp
private val TrackShape = RoundedCornerShape(5.dp)

@Composable
fun ExerciseStepIndicator(
    steps: List<ExerciseStep>,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) return

    val current = steps.indexOf(ExerciseStep.CURRENT)
    val progressLabel = stringResource(
        R.string.exercise_step_progress,
        current + 1,
        steps.size
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = progressLabel },
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TrackHeight)
                        .background(
                            // The track fills up to where you are, so the bar
                            // reads as progress rather than per-step state.
                            if (index <= current) ExerciseStep.COMPLETED.color else OutlineGray,
                            TrackShape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(StepSize)
                    .background(step.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseStepIndicatorPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.screen),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            val routine = SampleRoutine.cardioAndCore
            listOf(1, 3, 5).forEach { position ->
                ExerciseStepIndicator(steps = routine.stepsFor(position))
            }
        }
    }
}
