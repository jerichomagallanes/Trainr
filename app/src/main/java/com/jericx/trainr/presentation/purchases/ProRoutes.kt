package com.jericx.trainr.presentation.purchases

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity

// The store plumbing, kept out of the screens so they stay functions of state.
@Composable
fun ProPaywallRoute(reason: PaywallReason?, onClose: () -> Unit) {
    val viewModel: ProPaywallViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.load() }

    // Opened before the first entitlement read landed: a subscriber must not be
    // asked to pay again.
    LaunchedEffect(state.isPro) { if (state.isPro) onClose() }

    ProPaywallScreen(
        reason = reason,
        plans = state.plans,
        selectedId = state.selectedId,
        isWorking = state.isWorking,
        onSelect = viewModel::select,
        onBuy = { (context as? ComponentActivity)?.let(viewModel::buy) },
        onRestore = viewModel::restore,
        onClose = onClose,
        onOpenLink = { context.openLink(it) }
    )
}

// One route, because which of the two screens belongs here is the entitlement's
// answer and it can change while the app is open.
@Composable
fun ProRoute(onClose: () -> Unit) {
    val viewModel: ProPaywallViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.load() }

    if (state.isPro) {
        ProStatusScreen(
            isWorking = state.isWorking,
            onRestore = viewModel::restore,
            onOpenLink = { context.openLink(it) }
        )
    } else {
        ProPaywallScreen(
            reason = null,
            plans = state.plans,
            selectedId = state.selectedId,
            isWorking = state.isWorking,
            onSelect = viewModel::select,
            onBuy = { (context as? ComponentActivity)?.let(viewModel::buy) },
            onRestore = viewModel::restore,
            onClose = onClose,
            onOpenLink = { context.openLink(it) }
        )
    }
}

private fun android.content.Context.openLink(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
