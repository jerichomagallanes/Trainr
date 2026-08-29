package com.jericx.trainr.presentation.common.components.core

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Orange500
import com.jericx.trainr.presentation.common.theme.OutlineGray
import com.jericx.trainr.presentation.common.theme.Slate800
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme

@Composable
fun TrainrPillButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val contentColor = if (filled) Color.White else Slate800

    Row(
        modifier = modifier
            .height(ComponentHeight.Pill)
            .clip(MaterialTheme.shapes.medium)
            .background(if (filled) Orange500 else Color.White)
            .then(
                if (filled) Modifier
                else Modifier.border(1.5.dp, OutlineGray, MaterialTheme.shapes.medium)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 5.dp, end = Spacing.card),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(start = Spacing.extraSmall)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainrPillButtonPreview() {
    TrainrTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            modifier = Modifier.padding(Spacing.screen)
        ) {
            TrainrPillButton(text = "Pause", iconRes = R.drawable.ic_pause, onClick = {})
            TrainrPillButton(
                text = "Stop",
                iconRes = R.drawable.ic_stop,
                onClick = {},
                filled = false
            )
        }
    }
}
