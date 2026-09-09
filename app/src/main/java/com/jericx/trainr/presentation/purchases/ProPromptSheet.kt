package com.jericx.trainr.presentation.purchases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

// Shown where the tap happened rather than replacing the screen, so the limit is
// explained before anyone is asked to read a price.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPromptSheet(
    reason: PaywallReason,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.trainrColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surfaceCard,
        scrimColor = colors.scrim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            TrainrSectionTitle(text = stringResource(R.string.pro_upgrade_title))
            Text(
                text = stringResource(reason.prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center
            )
            TrainrButton(
                text = stringResource(R.string.pro_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.pro_not_now),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceMuted,
                modifier = Modifier.clickable(onClick = onDismiss)
            )
        }
    }
}
