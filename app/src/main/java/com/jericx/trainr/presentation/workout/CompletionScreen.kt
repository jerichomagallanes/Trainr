package com.jericx.trainr.presentation.workout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing

@Composable
fun CompletionScreen(
    @DrawableRes iconRes: Int,
    iconSize: Dp,
    title: String,
    message: String,
    secondaryLabel: String,
    primaryLabel: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSecondaryClick: () -> Unit = {},
    onPrimaryClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        TrainrTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Spacing.section * 2))

            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Slate800,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.screen)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = Slate800,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.card)
            )
        }

        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.screen,
                vertical = Spacing.section * 2
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.card)
        ) {
            TrainrButton(text = secondaryLabel, onClick = onSecondaryClick, isPrimary = false)
            TrainrButton(text = primaryLabel, onClick = onPrimaryClick)
        }
    }
}
