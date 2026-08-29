package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.presentation.common.theme.AccentOrange
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.components.WorkoutDayCard
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter

@Composable
fun WeeklyPlanRoute(
    onDayClick: (WorkoutDay) -> Unit = {},
    onTrackProgressClick: () -> Unit = {},
    onStartTodayClick: () -> Unit = {},
    viewModel: WeeklyPlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WeeklyPlanScreen(
        state = state,
        onDayClick = onDayClick,
        onTrackProgressClick = onTrackProgressClick,
        onStartTodayClick = onStartTodayClick
    )
}

@Composable
fun WeeklyPlanScreen(
    state: WeeklyPlanUiState,
    onDayClick: (WorkoutDay) -> Unit = {},
    onTrackProgressClick: () -> Unit = {},
    onStartTodayClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.large, vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.your_weekly_workout_plan),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )

            HorizontalDivider()

            Text(
                text = stringResource(
                    R.string.week_range_format,
                    state.plan.weekNumber,
                    WorkoutDateFormatter.formatWeekRange(
                        startMillis = state.weekStartMillis,
                        endMillis = state.weekEndMillis,
                        locale = locale
                    )
                ),
                fontSize = 16.sp,
                color = Slate800
            )

            Text(
                text = stringResource(R.string.track_weekly_progress) + " →",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentOrange,
                modifier = Modifier.clickable(onClick = onTrackProgressClick)
            )

            state.days.forEach { planDay ->
                WorkoutDayCard(
                    weekday = WorkoutDateFormatter.formatWeekday(planDay.dateMillis, locale),
                    day = planDay.day,
                    onClick = { onDayClick(planDay.day) }
                )
            }
        }

        Button(
            onClick = onStartTodayClick,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large, vertical = Spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.start_todays_workout),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(vertical = Spacing.small)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyPlanScreenPreview() {
    TrainrTheme {
        WeeklyPlanScreen(
            state = WeeklyPlanViewModel.stateFor(
                plan = SampleWorkoutData.weekOne,
                isSample = true
            )
        )
    }
}
