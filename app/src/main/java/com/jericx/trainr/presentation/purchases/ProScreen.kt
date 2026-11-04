package com.jericx.trainr.presentation.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
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
        null -> ProUnknownScreen(onBack = onClose)
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

@Composable
private fun ProUnknownScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.trainrColors.surfacePage)
    ) {
        TrainrTopBar(onBackClick = onBack, showLogo = false)
    }
}
