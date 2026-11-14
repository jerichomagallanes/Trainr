package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

// A number pad has no Return key, so a tap on anything that is not a control is
// the other way out of it. A child that takes the tap itself still wins.
@Composable
fun Modifier.dismissesKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
}
