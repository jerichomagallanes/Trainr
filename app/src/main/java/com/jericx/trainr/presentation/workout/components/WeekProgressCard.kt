package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Gray100
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.WeekStatus
import com.jericx.trainr.presentation.workout.model.chipColor
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 89.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, OutlineGray, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(12.dp)
                .fillMaxHeight()
                .background(Slate800)
        ) {}

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.small + Spacing.extraSmall, vertical = Spacing.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = buildAnnotatedString {
                        append(stringResource(R.string.week_number_format, weekNumber))
                        append(" ")
                        withStyle(SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize)) {
                            append(stringResource(R.string.week_range_parens, dateRange))
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate800
                )
                WeekStatusChip(status = status)
            }

            Text(
                text = stringResource(
                    R.string.days_completed_format,
                    completedDays,
                    totalDays,
                    completionPercentage
                ),
                style = MaterialTheme.typography.labelMedium,
                color = Slate800,
                modifier = Modifier
                    .background(Gray100, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.card, vertical = 5.dp)
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_arrow_forward_circle),
            contentDescription = null,
            modifier = Modifier
                .padding(end = Spacing.small)
                .size(30.dp)
        )
    }
}

@Composable
private fun WeekStatusChip(status: WeekStatus, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(status.labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .background(status.chipColor, RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.small, vertical = 3.dp)
    )
}

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
