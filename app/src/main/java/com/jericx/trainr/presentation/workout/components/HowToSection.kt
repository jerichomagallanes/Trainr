package com.jericx.trainr.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

private val ToggleHeight = 36.dp
private val NumberColumn = 24.dp

// One place to learn the movement: the written steps the catalog owns, and the
// tutorial for it, behind a single disclosure. Two rows saying "how do I do
// this" read as two different answers when there is only one.
@Composable
fun HowToSection(
    steps: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    video: @Composable () -> Unit = {}
) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.card)
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surfaceSunken)
                .clickable(role = Role.Button, onClick = onToggle)
                .height(ToggleHeight)
                .padding(horizontal = Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (isExpanded) R.string.hide_how_to_perform
                    else R.string.show_how_to_perform
                ),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface
            )
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(20.dp)
                    .rotate(if (isExpanded) 0f else 180f)
            )
        }

        if (!isExpanded) return@Column

        // Numbered here rather than in the data: the catalog stores the step,
        // not its position, so reordering one never leaves two step threes.
        steps.forEachIndexed { index, step ->
            Row(modifier = Modifier.padding(start = Spacing.extraSmall)) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceMuted,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.width(NumberColumn)
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = colors.onSurface
                )
            }
        }

        video()
    }
}
