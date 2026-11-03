package com.jericx.trainr.presentation.onboarding.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMISourcesSheet(onDismiss: () -> Unit) {
    val colors = MaterialTheme.trainrColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceCard,
        scrimColor = colors.scrim
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            TrainrSectionTitle(
                text = stringResource(R.string.bmi_about_sources),
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("bmiSourcesClose")) {
                Text(stringResource(R.string.close), color = colors.brandStrong)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .testTag("bmiSourcesContent")
                .verticalScroll(rememberScrollState())
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            SourceSection(R.string.bmi_calculation_title, R.string.bmi_explanation, BMISource.Calculation)
            SourceSection(R.string.bmi_categories_title, R.string.bmi_ranges, BMISource.Categories)
            SourceSection(R.string.bmi_limitations_title, R.string.bmi_limitations, BMISource.Limitations)
            Text(
                text = stringResource(R.string.bmi_source_date),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
        }
    }
}

@Composable
private fun SourceSection(@StringRes title: Int, @StringRes explanation: Int, source: BMISource) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        TrainrSectionTitle(stringResource(title), modifier = Modifier.semantics { heading() })
        Text(
            text = stringResource(explanation),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.trainrColors.onSurface
        )
        BMISourceLink(source)
    }
}
