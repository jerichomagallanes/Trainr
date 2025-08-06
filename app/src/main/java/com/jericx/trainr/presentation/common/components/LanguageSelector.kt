package com.jericx.trainr.presentation.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.LanguagePreferences
import com.jericx.trainr.presentation.common.theme.Spacing

/**
 * Language selector component following app design patterns.
 * Compact dropdown for language selection, designed to fit in corners/headers.
 */
@Composable
fun LanguageSelector(
    currentLanguage: LanguagePreferences.Language,
    onLanguageSelected: (LanguagePreferences.Language) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    val context = LocalContext.current
    val languagePreferences = remember { LanguagePreferences(context) }
    val availableLanguages = remember { languagePreferences.getAvailableLanguages(context) }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) Spacing.small else Spacing.medium,
                    vertical = if (compact) Spacing.extraSmall else Spacing.small
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.language_selection),
                    modifier = Modifier.size(if (compact) 16.dp else 20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!compact) {
                    Spacer(modifier = Modifier.width(Spacing.small))
                    
                    Text(
                        text = currentLanguage.nativeName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.dropdown_content_description),
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            availableLanguages.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (language.code == currentLanguage.code) 
                                        FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (language.code == currentLanguage.code)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (language.code != currentLanguage.code) {
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}