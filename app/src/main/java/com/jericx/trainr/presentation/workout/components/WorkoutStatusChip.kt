package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun WorkoutStatusChip(
    status: WorkoutStatus,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(status.labelRes),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(status.chipColor, MaterialTheme.shapes.small)
            .padding(PaddingValues(horizontal = Spacing.small, vertical = 3.dp))
    )
}

@Preview(showBackground = true)
@Composable
private fun WorkoutStatusChipPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            WorkoutStatus.entries.forEach { WorkoutStatusChip(status = it) }
        }
    }
}
