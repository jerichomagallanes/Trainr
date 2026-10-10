package com.jericx.trainr.presentation.workout.components

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

// A countdown that ends while the phone is in a pocket has to be felt as well
// as heard, and the tone is the one the phone already uses for something
// finishing, so a silenced phone stays silent.
@Composable
fun rememberTimerAlert(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    return remember(haptics, context) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            playFinishedTone(context)
        }
    }
}

private fun playFinishedTone(context: Context) {
    val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
    runCatching { RingtoneManager.getRingtone(context, tone)?.play() }
}
