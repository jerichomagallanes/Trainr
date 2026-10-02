package com.jericx.trainr.presentation.unstuck

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.common.components.core.TrainrOptionRow
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustTodaySheet(
    dayTitle: String,
    exercises: List<String>,
    onChoose: (DirectReason) -> Unit,
    onShowHowTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.trainrColors
    val locale = LocalLocale.current.platformLocale
    var pickingExercise by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Five choices and a quiet action do not fit a half sheet, and a
        // choice nobody can see is not a choice.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceCard,
        scrimColor = colors.scrim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            Text(
                text = stringResource(
                    R.string.adjust_sheet_eyebrow_format,
                    dayTitle.lowercase(locale)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
            Text(
                text = stringResource(
                    if (pickingExercise) R.string.guide_pick_exercise else R.string.adjust_sheet_title
                ),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = colors.onSurface
            )

            if (pickingExercise) {
                exercises.forEachIndexed { index, name ->
                    TrainrOptionRow(
                        title = name,
                        description = stringResource(R.string.adjust_reason_guidance_hint),
                        onClick = { onShowHowTo(index + 1) }
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.adjust_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted
                )
                TrainrOptionRow(
                    title = stringResource(R.string.adjust_reason_time),
                    description = stringResource(R.string.adjust_reason_time_hint),
                    onClick = { onChoose(DirectReason.LESS_TIME) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.adjust_reason_equipment),
                    description = stringResource(R.string.adjust_reason_equipment_hint),
                    onClick = { onChoose(DirectReason.EQUIPMENT) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.adjust_reason_guidance),
                    description = stringResource(R.string.adjust_reason_guidance_hint),
                    onClick = { pickingExercise = true }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.adjust_reason_pain),
                    description = stringResource(R.string.adjust_reason_pain_hint),
                    onClick = { onChoose(DirectReason.PAIN) }
                )
                TrainrOptionRow(
                    title = stringResource(R.string.adjust_reason_other),
                    description = stringResource(R.string.adjust_reason_other_hint),
                    onClick = { onChoose(DirectReason.OTHER) }
                )
            }

            TrainrQuietButton(
                text = stringResource(R.string.keep_todays_plan),
                onClick = onDismiss
            )
        }
    }
}
