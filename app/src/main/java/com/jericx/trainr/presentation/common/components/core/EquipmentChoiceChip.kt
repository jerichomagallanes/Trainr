package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.presentation.common.iconRes
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

private val DiscSize = 52.dp
private val IconSize = 32.dp
private val ChipHeight = 72.dp

// One chip for every place equipment is chosen, so the thumbnail, the disc and
// the row height cannot drift between the setup screen and Unstuck.
@Composable
fun EquipmentChoiceChip(
    equipment: Equipment,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TrainrToggleChip(
        text = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        height = ChipHeight,
        horizontalPadding = Spacing.tight,
        leading = { EquipmentDisc(equipment) }
    )
}

// Sunken, not white: in dark a white disc on a card reads as a hole punched
// through it, and the sunken surface is the one ground that is lighter than
// the card in both themes. The artwork carries its own light and dark colours.
@Composable
private fun EquipmentDisc(equipment: Equipment) {
    val colors = MaterialTheme.trainrColors
    Box(
        modifier = Modifier
            .size(DiscSize)
            .background(colors.surfaceSunken, CircleShape)
            .border(1.5.dp, colors.outlineControl, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(equipment.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(IconSize)
        )
    }
}
