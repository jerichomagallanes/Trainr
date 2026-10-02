package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

// The citation App Review asked for, one tap from the result it explains: the
// formula, the adult ranges, what BMI cannot tell anyone, and where the ranges
// come from. Mirrors BMISourcesView on iOS.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMISourcesSheet(onDismiss: () -> Unit) {
    val colors = MaterialTheme.trainrColors
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceCard,
        scrimColor = colors.scrim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            TrainrSectionTitle(text = stringResource(R.string.bmi_about_sources))
            Text(
                text = stringResource(R.string.bmi_explanation),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.bmi_ranges),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.bmi_limitations),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )
            SourceLink(
                text = stringResource(R.string.bmi_cdc_categories),
                onClick = { uriHandler.openUri(CDC_CATEGORIES) }
            )
            SourceLink(
                text = stringResource(R.string.bmi_cdc_about),
                onClick = { uriHandler.openUri(CDC_ABOUT) }
            )
            Text(
                text = stringResource(R.string.bmi_source_date),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
            TrainrButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SourceLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.trainrColors.brandStrong,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(vertical = Spacing.extraSmall)
    )
}

// The same two pages iOS cites, so both apps point a reviewer at one source.
internal const val CDC_CATEGORIES = "https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html"
internal const val CDC_ABOUT = "https://www.cdc.gov/bmi/about/index.html"
