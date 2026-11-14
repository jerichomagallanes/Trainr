package com.jericx.trainr.presentation.common.components.core

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.scaledWithText
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun TrainrPillButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val colors = MaterialTheme.trainrColors
    val contentColor = if (filled) colors.onBrand else colors.onSurface

    Row(
        modifier = modifier
            // A floor rather than a height: the label grows with the text
            // setting and would otherwise be cut off top and bottom.
            .heightIn(min = ComponentHeight.Pill)
            .clip(MaterialTheme.shapes.medium)
            .background(if (filled) colors.brandStrong else colors.surfaceRaised)
            .then(
                if (filled) Modifier
                else Modifier.border(1.5.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 5.dp, end = Spacing.card, top = PILL_INSET, bottom = PILL_INSET),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(PILL_ICON.scaledWithText())
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = Spacing.extraSmall)
        )
    }
}

private val PILL_ICON = 24.dp
private val PILL_INSET = 2.dp

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
