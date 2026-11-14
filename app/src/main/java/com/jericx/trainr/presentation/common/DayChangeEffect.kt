package com.jericx.trainr.presentation.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

// A screen is composed once and then keeps whatever the calendar said at the
// time. Coming back to the app and the date rolling over under an open screen
// are the two moments that answer goes stale without anything being touched.
@Composable
fun DayChangeEffect(onDayMayHaveChanged: () -> Unit) {
    val current by rememberUpdatedState(onDayMayHaveChanged)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { current() }

    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = current()
        }
        val filter = IntentFilter(Intent.ACTION_DATE_CHANGED).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        onDispose { context.unregisterReceiver(receiver) }
    }
}
