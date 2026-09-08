package com.jericx.trainr.presentation.workout.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.workout.model.StatusTone

@Composable
fun StatusChip(
    @StringRes labelRes: Int,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    // The week card asks for one line beside a long week title; the day card
    // wrapped before this chip absorbed it, and clipping there would be a
    // light-mode change.
    singleLine: Boolean = false
) {
    val colors = MaterialTheme.trainrColors
    Text(
        text = stringResource(labelRes),
        color = colors.onStatus,
        style = MaterialTheme.typography.labelSmall,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        softWrap = !singleLine,
        modifier = modifier
            .background(
                when (tone) {
                    StatusTone.DONE -> colors.statusDone
                    StatusTone.ACTIVE -> colors.statusActive
                    StatusTone.IDLE -> colors.statusIdle
                },
                MaterialTheme.shapes.small
            )
            .padding(PaddingValues(horizontal = Spacing.small, vertical = 3.dp))
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            StatusChip(labelRes = R.string.completed, tone = StatusTone.DONE)
            StatusChip(labelRes = R.string.in_progress, tone = StatusTone.ACTIVE)
            StatusChip(labelRes = R.string.not_started, tone = StatusTone.IDLE)
        }
    }
}
