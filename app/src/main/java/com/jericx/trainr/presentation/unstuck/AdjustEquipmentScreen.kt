package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.EquipmentChoices
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.core.TrainrRadioChip
import com.jericx.trainr.presentation.common.components.core.TrainrToggleChip
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.getLocalizedName
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdjustEquipmentScreen(
    state: AdjustmentUiState,
    modifier: Modifier = Modifier,
    onSelectExercise: (Long) -> Unit = {},
    onToggleEquipment: (Equipment) -> Unit = {},
    onShowRecommendation: () -> Unit = {},
    onKeepPlan: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                TrainrButton(
                    text = stringResource(R.string.show_recommendation),
                    onClick = onShowRecommendation,
                    enabled = state.canShowRecommendation
                )
                TrainrQuietButton(
                    text = stringResource(R.string.keep_todays_plan),
                    onClick = onKeepPlan
                )
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.adjust_equipment_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )

            // Entering from an exercise card already answered this, so the
            // chooser is only for the people who came in without one.
            if (!state.enteredWithExercise) {
                Text(
                    text = stringResource(R.string.adjust_equipment_exercise),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(top = Spacing.medium)
                )
                Column(
                    modifier = Modifier.padding(top = Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    state.exerciseChoices.forEach { choice ->
                        TrainrRadioChip(
                            text = choice.name,
                            selected = state.selectedExerciseId == choice.id,
                            onClick = { onSelectExercise(choice.id) }
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.adjust_equipment_available),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.large)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                EquipmentChoices.forEach { equipment ->
                    TrainrToggleChip(
                        text = equipment.getLocalizedName(),
                        selected = equipment in state.availableEquipment,
                        onClick = { onToggleEquipment(equipment) },
                        height = ComponentHeight.Medium,
                        horizontalPadding = Spacing.medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustEquipmentScreenPreview() {
    TrainrTheme {
        AdjustEquipmentScreen(state = SampleAdjustmentStates.equipment)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustEquipmentScreenDarkPreview() {
    TrainrTheme(darkTheme = true) {
        AdjustEquipmentScreen(state = SampleAdjustmentStates.equipment)
    }
}
