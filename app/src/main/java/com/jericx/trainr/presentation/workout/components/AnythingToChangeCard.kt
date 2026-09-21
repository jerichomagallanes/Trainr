package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrTextAction
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

// A note is worth leaving whatever happened, so the card stands on its own and
// the adjustment question only joins it when one is still unanswered.
@Composable
fun AnythingToChangeCard(
    onLeaveNote: () -> Unit,
    modifier: Modifier = Modifier,
    onFeedback: (() -> Unit)? = null
) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.screen)
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .padding(Spacing.card)
    ) {
        Text(
            text = stringResource(R.string.anything_to_change_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
        Text(
            text = stringResource(R.string.anything_to_change_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            modifier = Modifier.padding(top = Spacing.extraSmall)
        )
        onFeedback?.let {
            TrainrTextAction(
                text = stringResource(R.string.tell_us_how_it_went),
                onClick = it,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
        TrainrTextAction(
            text = stringResource(R.string.leave_a_note),
            onClick = onLeaveNote
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnythingToChangeCardPreview() {
    TrainrTheme {
        AnythingToChangeCard(onLeaveNote = {}, onFeedback = {})
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnythingToChangeCardNoteOnlyPreview() {
    TrainrTheme(darkTheme = true) {
        AnythingToChangeCard(onLeaveNote = {})
    }
}
