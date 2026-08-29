package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.StatusCompleted
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.sample.SampleRoutine

@Composable
fun ExerciseCard(
    exercise: ExerciseUi,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A finished exercise turns green throughout: badge, name and rule.
    val accent = if (exercise.isCompleted) StatusCompleted else Slate800

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, accent, MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.tight, end = Spacing.tight, top = Spacing.card),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.position.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.small)
            )

            Image(
                painter = painterResource(
                    if (exercise.isCompleted) R.drawable.ic_check_box
                    else R.drawable.ic_check_box_blank
                ),
                contentDescription = stringResource(
                    if (exercise.isCompleted) R.string.mark_exercise_incomplete
                    else R.string.mark_exercise_complete
                ),
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onToggleCompleted)
            )
        }

        HorizontalDivider(
            color = accent,
            modifier = Modifier.padding(top = Spacing.card)
        )

        Column(
            modifier = Modifier.padding(horizontal = Spacing.tight, vertical = Spacing.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.card)
        ) {
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                color = Slate800
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.minutes,
                        exercise.minutes,
                        exercise.minutes
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate800,
                    modifier = Modifier.padding(start = Spacing.extraSmall)
                )
                Text(
                    text = exercise.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .padding(start = Spacing.small)
                        .background(Slate800, MaterialTheme.shapes.medium)
                        .padding(horizontal = Spacing.tight, vertical = 3.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ExerciseCardPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge - Spacing.extraSmall),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            SampleRoutine.cardioAndCore.take(3).forEach { exercise ->
                ExerciseCard(exercise = exercise, onToggleCompleted = {})
            }
        }
    }
}
