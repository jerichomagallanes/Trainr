package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.themedPainter
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.workout.model.WeekStatus
import com.jericx.trainr.presentation.workout.model.chipTone
import com.jericx.trainr.presentation.workout.model.labelRes
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress

@Composable
fun WeekProgressCard(
    weekNumber: Int,
    dateRange: String,
    completedDays: Int,
    totalDays: Int,
    completionPercentage: Int,
    status: WeekStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.trainrColors
    val rail = colors.surfaceEmphasis

    // The rail is painted rather than laid out: an intrinsic height would fix
    // the card to one line of chip and cut a wrapped label in half.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 89.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceCard)
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .drawBehind { drawRect(rail, size = Size(RailWidth.toPx(), size.height)) }
            .clickable(onClick = onClick)
            .padding(
                start = RailWidth + Spacing.small + Spacing.extraSmall,
                end = Spacing.small + Spacing.extraSmall,
                top = Spacing.small + Spacing.extraSmall,
                bottom = Spacing.small + Spacing.extraSmall
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.screen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = buildAnnotatedString {
                    append(stringResource(R.string.week_number_format, weekNumber))
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append(stringResource(R.string.week_range_parens, dateRange))
                    }
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = colors.onSurface
            )
            StatusChip(labelRes = status.labelRes, tone = status.chipTone)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.days_completed_format,
                    totalDays,
                    completedDays,
                    totalDays,
                    completionPercentage
                ),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                modifier = Modifier
                    .background(colors.surfaceSunken, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.card, vertical = 5.dp)
            )

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

private val RailWidth = 12.dp

@Preview(showBackground = true)
@Composable
private fun WeekProgressCardPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.screen),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            SampleWeeklyProgress.weeks.take(4).forEach { week ->
                WeekProgressCard(
                    weekNumber = week.weekNumber,
                    dateRange = "Jul 21 – 27, 2025",
                    completedDays = week.completedDays,
                    totalDays = week.totalDays,
                    completionPercentage = week.completionPercentage,
                    status = week.status,
                    onClick = {}
                )
            }
        }
    }
}
