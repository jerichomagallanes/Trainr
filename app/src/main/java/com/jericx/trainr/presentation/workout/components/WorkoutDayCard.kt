package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.themedPainter
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.workout.model.StatusTone
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData

private val AccentRuleHeight = 3.dp

@Composable
fun WorkoutDayCard(
    weekday: String,
    day: WorkoutDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMissed: Boolean = false
) {
    val started = day.status != WorkoutStatus.NOT_STARTED
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (started) colors.surfaceEmphasis else colors.surfaceCard)
                // Transparent in light: the top rule is the dark theme's started cue.
                .drawBehind {
                    if (started) {
                        drawRect(colors.accentRule, size = Size(size.width, AccentRuleHeight.toPx()))
                    }
                }
                .padding(Spacing.card),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (started) colors.onSurfaceEmphasis else colors.onSurface
                )
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (started) colors.onSurfaceEmphasis else colors.onSurface
                )
            }
            // Missed deliberately reads in the same grey as "not started".
            if (isMissed) {
                StatusChip(labelRes = R.string.missed, tone = StatusTone.IDLE)
            } else {
                StatusChip(labelRes = day.status.labelRes, tone = day.status.chipTone)
            }
        }

        HorizontalDivider(color = colors.outlineControl)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceCard)
                .padding(Spacing.card),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.extraSmall))
                    Text(
                        text = pluralStringResource(R.plurals.minutes, day.duration, day.duration),
                        style = MaterialTheme.typography.bodyMedium,
                        // Pure black here, not onSurface: light must stay #000000.
                        color = colors.onSurfaceStrong
                    )
                }

                Text(
                    text = pluralStringResource(R.plurals.exercises_count, day.exerciseCount, day.exerciseCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface,
                    modifier = Modifier
                        .background(colors.surfaceSunken, MaterialTheme.shapes.small)
                        .padding(horizontal = Spacing.small, vertical = 3.dp)
                )

                if (day.equipment.isNotEmpty()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                                append(stringResource(R.string.equipment_label) + " ")
                            }
                            append(day.equipment.joinToString(", "))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                }
            }

            Image(
                painter = themedPainter(
                    R.drawable.ic_arrow_forward_circle,
                    R.drawable.ic_arrow_forward_circle_night
                ),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutDayCardPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            val weekdays = listOf("Monday", "Wednesday", "Friday")
            SampleWorkoutData.weekOne.workoutDays.forEachIndexed { index, day ->
                WorkoutDayCard(weekday = weekdays[index], day = day, onClick = {})
            }
            Spacer(modifier = Modifier.height(Spacing.small))
        }
    }
}
