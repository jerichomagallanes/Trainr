package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.toRoutineUi
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle

@Composable
fun ExerciseCard(
    exercise: ExerciseUi,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    onSetChanged: (ExerciseSet) -> Unit = {},
    onAddSet: () -> Unit = {},
    onDeleteSet: (ExerciseSet) -> Unit = {},
    units: UnitSystem = UnitSystem.Default,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors
    val accentInk = if (exercise.isCompleted) colors.statusDoneInk else colors.onSurface
    val accentOutline = if (exercise.isCompleted) colors.statusDoneEdge else colors.cardEdge
    val accentDivider = if (exercise.isCompleted) colors.statusDoneEdge else colors.cardRule
    val accentFill = if (exercise.isCompleted) colors.statusDone else colors.surfaceEmphasis
    val onAccentFill = if (exercise.isCompleted) colors.onStatus else colors.onSurfaceEmphasis

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, accentOutline, MaterialTheme.shapes.medium)
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
                    .background(accentFill, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.position.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = onAccentFill
                )
            }

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = accentInk,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.small)
            )

            Icon(
                painter = painterResource(
                    if (exercise.isCompleted) R.drawable.ic_check_box
                    else R.drawable.ic_check_box_blank
                ),
                contentDescription = stringResource(
                    if (exercise.isCompleted) R.string.mark_exercise_incomplete
                    else R.string.mark_exercise_complete
                ),
                tint = if (exercise.isCompleted) colors.statusDoneInk else colors.outlineControl,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onToggleCompleted)
            )
        }

        HorizontalDivider(
            color = accentDivider,
            modifier = Modifier.padding(top = Spacing.card)
        )

        Column(
            modifier = Modifier.padding(
                start = Spacing.tight,
                end = Spacing.tight,
                top = Spacing.card,
                bottom = Spacing.screen
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.screen)
        ) {
            // The muscles belong to the movement's name, not to the coaching
            // note under it, so the pair sits closer than the card's rhythm.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                if (exercise.primaryMuscle.isNotBlank()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(color = accentInk, fontWeight = FontWeight.Medium)
                            ) {
                                append(exercise.primaryMuscle)
                            }
                            if (exercise.secondaryMuscles.isNotEmpty()) {
                                append(MUSCLE_SEPARATOR)
                                append(exercise.secondaryMuscles.joinToString(", "))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted
                    )
                }

                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                    color = colors.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.minutes,
                        exercise.minutes,
                        exercise.minutes
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(start = Spacing.extraSmall)
                )
                Text(
                    text = exercise.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceEmphasis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = Spacing.small)
                        .background(colors.surfaceEmphasis, MaterialTheme.shapes.medium)
                        .padding(horizontal = Spacing.tight, vertical = 3.dp)
                )
            }

            // Drawn even when empty: gating it on sets takes away the only
            // Add set button.
            ExerciseSetTable(
                measure = exercise.measure,
                sets = exercise.sets,
                onSetChanged = onSetChanged,
                onAddSet = onAddSet,
                onDeleteSet = onDeleteSet,
                previousSets = exercise.previousSets,
                units = units
            )

            content()
        }
    }
}

// A middot rather than a label on each side: the line is read at a glance
// twelve times down a day, and "Primary:"/"Secondary:" twice per card is more
// words than the names themselves.
private const val MUSCLE_SEPARATOR = "  \u00b7  "

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ExerciseCardPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            SampleWorkoutData.dayFor(SampleWorkoutData.DEFAULT_DAY_NUMBER)
                .toRoutineUi(catalog = SampleWorkoutData.catalog).exercises.take(3).forEach { exercise ->
                ExerciseCard(exercise = exercise, onToggleCompleted = {})
            }
        }
    }
}
