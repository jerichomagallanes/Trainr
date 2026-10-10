package com.jericx.trainr.presentation.unstuck.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.presentation.common.components.core.TrainrOptionRow
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun FeedbackDetailScreen(
    modifier: Modifier = Modifier,
    onAnswer: (FeedbackAnswer) -> Unit = {},
    onSaveForLater: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            TrainrQuietButton(
                text = stringResource(R.string.save_for_later),
                onClick = onSaveForLater
            )
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.what_still_needs_changing),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = MaterialTheme.trainrColors.onSurface
            )

            Column(
                modifier = Modifier.padding(top = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                TrainrOptionRow(
                    title = stringResource(R.string.feedback_still_too_long),
                    description = stringResource(R.string.feedback_still_too_long_hint),
                    onClick = { onAnswer(FeedbackAnswer.STILL_TOO_LONG) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.feedback_exercise_confusing),
                    description = stringResource(R.string.feedback_exercise_confusing_hint),
                    onClick = { onAnswer(FeedbackAnswer.EXERCISE_CONFUSING) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.feedback_something_else),
                    description = stringResource(R.string.feedback_something_else_hint),
                    onClick = { onAnswer(FeedbackAnswer.SOMETHING_ELSE) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackDetailScreenPreview() {
    TrainrTheme {
        FeedbackDetailScreen()
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeedbackDetailScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        FeedbackDetailScreen()
    }
}
