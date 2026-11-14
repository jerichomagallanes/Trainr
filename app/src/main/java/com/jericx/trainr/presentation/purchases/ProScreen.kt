package com.jericx.trainr.presentation.purchases

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

// Which of the two Pro screens belongs here is the entitlement's answer, and
// until there is one there is nothing honest to draw: the sales page would be
// an advertisement aimed at someone already paying, and the status screen would
// promise Pro to someone who has not bought it. An empty page with a way out
// says neither.
@Composable
fun ProScreen(
    state: ProPaywallViewModel.State,
    onSelect: (String) -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onNoticeShown: () -> Unit,
    onClose: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    when (state.isPro) {
        null -> ProUnknownScreen(
            readFailed = state.readFailed,
            isWorking = state.isWorking,
            noticeRes = state.noticeRes,
            onRestore = onRestore,
            onNoticeShown = onNoticeShown,
            onBack = onClose,
            onOpenLink = onOpenLink
        )
        true -> ProStatusScreen(
            isWorking = state.isWorking,
            isLifetime = state.isLifetime,
            noticeRes = state.noticeRes,
            onRestore = onRestore,
            onNoticeShown = onNoticeShown,
            onBack = onClose,
            onOpenLink = onOpenLink
        )
        false -> ProPaywallScreen(
            reason = null,
            plans = state.plans,
            selectedId = state.selectedId,
            isWorking = state.isWorking,
            noticeRes = state.noticeRes,
            onSelect = onSelect,
            onBuy = onBuy,
            onRestore = onRestore,
            onNoticeShown = onNoticeShown,
            onClose = onClose,
            onOpenLink = onOpenLink
        )
    }
}

// Silence is right while the store is still answering. Once it has failed to,
// silence is a dead end: restoring lives on the other two screens only, and a
// subscriber on a new phone would have nowhere left to reach it.
@Composable
private fun ProUnknownScreen(
    readFailed: Boolean,
    isWorking: Boolean,
    @StringRes noticeRes: Int?,
    onRestore: () -> Unit,
    onNoticeShown: () -> Unit,
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfacePage)
    ) {
        TrainrTopBar(onBackClick = onBack, showLogo = false)
        if (readFailed) {
            TrainrScreenContent(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pro_check_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted
                )
                Spacer(modifier = Modifier.height(Spacing.sectionGap))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    Text(
                        text = stringResource(R.string.pro_restore),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.brandStrong,
                        modifier = Modifier.clickable(enabled = !isWorking, onClick = onRestore)
                    )
                    Text(
                        text = stringResource(R.string.pro_terms),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.brandStrong,
                        modifier = Modifier.clickable { onOpenLink(ProLinks.TERMS) }
                    )
                    Text(
                        text = stringResource(R.string.pro_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.brandStrong,
                        modifier = Modifier.clickable { onOpenLink(ProLinks.PRIVACY) }
                    )
                }
                ProNoticeDialog(noticeRes = noticeRes, onDismiss = onNoticeShown)
            }
        }
    }
}
