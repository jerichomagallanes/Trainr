package com.jericx.trainr.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing

private val LogoWidth = 180.dp

@Composable
fun SplashScreen(versionName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The wordmark rather than the app's name in text: it is the same
            // mark the top bar carries, so the first screen and every screen
            // after it agree about what this app looks like.
            Image(
                painter = painterResource(id = R.drawable.img_trainr),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.width(LogoWidth)
            )
            Text(
                text = stringResource(R.string.version_format, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate800,
                modifier = Modifier.padding(top = Spacing.medium)
            )
        }
    }
}
