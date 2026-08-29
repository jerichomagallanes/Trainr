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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.Gray100
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData

@Composable
fun WorkoutDayCard(
    weekday: String,
    day: WorkoutDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A started workout gets the dark header; one not begun stays light.
    val headerIsDark = day.status != WorkoutStatus.NOT_STARTED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, OutlineGray, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (headerIsDark) Slate800 else Color.White)
                .padding(Spacing.card),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weekday,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (headerIsDark) Color.White else Slate800
                )
                Text(
                    text = day.title,
                    fontSize = 14.sp,
                    color = if (headerIsDark) Color.White else Slate800
                )
            }
            WorkoutStatusChip(status = day.status)
        }

        HorizontalDivider(color = OutlineGray)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(Spacing.card),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.minutes, day.duration),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                Text(
                    text = stringResource(R.string.exercises_count, day.exerciseCount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                    modifier = Modifier
                        .background(Gray100, MaterialTheme.shapes.small)
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
                        fontSize = 14.sp,
                        color = Slate800
                    )
                }
            }

            Image(
                painter = painterResource(R.drawable.ic_arrow_forward_circle),
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
