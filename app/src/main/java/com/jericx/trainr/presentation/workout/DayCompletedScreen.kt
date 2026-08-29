package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun DayCompletedScreen(
    dayNumber: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onViewProgressClick: () -> Unit = {},
    onBackToRoutineClick: () -> Unit = {}
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
                painter = painterResource(R.drawable.ic_award_star),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = stringResource(R.string.day_completed_format, dayNumber),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Slate800,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.screen)
            )

            Text(
                text = stringResource(R.string.day_completed_message),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = Slate800,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.card)
            )
        }

        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.screen,
                vertical = Spacing.section * 2
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.card)
        ) {
            TrainrButton(
                text = stringResource(R.string.view_weekly_progress),
                onClick = onViewProgressClick,
                isPrimary = false
            )
            TrainrButton(
                text = stringResource(R.string.back_to_weekly_routine),
                onClick = onBackToRoutineClick
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun DayCompletedScreenPreview() {
    TrainrTheme {
        DayCompletedScreen(dayNumber = 2)
    }
}
