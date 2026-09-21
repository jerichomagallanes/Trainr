package com.jericx.trainr.presentation.workout

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.themedPainter
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun SessionSavedScreen(
    performedExercises: Int,
    plannedExercises: Int,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {}
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
                painter = themedPainter(R.drawable.img_task_done, R.drawable.img_task_done_night),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = stringResource(R.string.workout_saved),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.trainrColors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.screen)
            )

            Text(
                text = stringResource(
                    R.string.finished_early_summary_format,
                    performedExercises,
                    plannedExercises
                ),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = MaterialTheme.trainrColors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.card)
            )
        }

        TrainrButton(
            text = stringResource(R.string.done),
            onClick = onDoneClick,
            modifier = Modifier.padding(
                horizontal = Spacing.screen,
                vertical = Spacing.section * 2
            )
        )
    }
}

@Preview(showBackground = true, heightDp = 854)
@Composable
private fun SessionSavedScreenPreview() {
    TrainrTheme {
        SessionSavedScreen(performedExercises = 4, plannedExercises = 6)
    }
}
