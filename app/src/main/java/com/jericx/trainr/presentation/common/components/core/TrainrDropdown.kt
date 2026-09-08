package com.jericx.trainr.presentation.common.components.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors

@Composable
fun TrainrDropdown(
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Shown, muted, when nothing has been chosen yet, the same way the text
    // fields show theirs. A dropdown that opens on a real-looking value has
    // answered the question on the client's behalf.
    placeholder: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.trainrColors

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentHeight.Medium)
                .clickable { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.outlineControl)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = selectedValue.ifBlank { placeholder },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedValue.isBlank()) {
                        colors.onSurfaceMuted
                    } else {
                        colors.onSurface
                    }
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.dropdown_content_description),
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = colors.onSurfaceMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            containerColor = colors.surfaceRaised,
            border = BorderStroke(1.dp, colors.raisedEdge)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                    },
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
} 

@Preview(showBackground = true)
@Composable
private fun TrainrDropdownPreview() {
    TrainrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium)
        ) {
            val selected = remember { mutableStateOf("Beginner") }
            TrainrDropdown(
                selectedValue = selected.value,
                options = listOf("Beginner", "Intermediate", "Advanced"),
                onSelectionChange = { selected.value = it }
            )
        }
    }
}
