package com.jericx.trainr.presentation.purchases

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

// Where someone with Pro can see that they have it, restore a purchase after a
// reinstall, and reach the only place a subscription can actually be cancelled.
// The paywall cannot serve any of that: it closes itself for anyone who already
// has Pro.
@Composable
fun ProStatusScreen(
    isWorking: Boolean,
    isLifetime: Boolean,
    @StringRes noticeRes: Int?,
    onRestore: () -> Unit,
    onNoticeShown: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val colors = MaterialTheme.trainrColors

    TrainrScreenContent(modifier = Modifier.background(colors.surfacePage)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
            Text(
                text = stringResource(R.string.pro_name).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onBrand,
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.extraSmall))
                    .background(colors.brandLarge)
                    .padding(horizontal = Spacing.extraSmall, vertical = 3.dp)
            )
            TrainrScreenTitle(
                text = stringResource(
                    if (isLifetime) R.string.pro_active_lifetime else R.string.pro_active
                )
            )
        }
        Spacer(modifier = Modifier.height(Spacing.section))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            PaywallReason.entries.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(feature.icon),
                        contentDescription = null,
                        tint = colors.brandStrong,
                        modifier = Modifier.size(FEATURE_ICON)
                    )
                    Text(
                        text = stringResource(feature.heading),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sectionGap))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            if (!isLifetime) {
                Text(
                    text = stringResource(R.string.pro_manage),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brandStrong,
                    modifier = Modifier.clickable { onOpenLink(ProLinks.SUBSCRIPTIONS) }
                )
            }
            Text(
                text = stringResource(R.string.pro_restore),
                style = MaterialTheme.typography.labelMedium,
                color = colors.brandStrong,
                modifier = Modifier.clickable(enabled = !isWorking, onClick = onRestore)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                Text(
                    text = stringResource(R.string.pro_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.brandStrong,
                    modifier = Modifier.clickable { onOpenLink(ProLinks.TERMS) }
                )
                Text(
                    text = stringResource(R.string.pro_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.brandStrong,
                    modifier = Modifier.clickable { onOpenLink(ProLinks.PRIVACY) }
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.large))
        ProNoticeDialog(noticeRes = noticeRes, onDismiss = onNoticeShown)
    }
}

internal val FEATURE_ICON = 28.dp
