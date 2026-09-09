package com.jericx.trainr.presentation.purchases

import androidx.compose.foundation.background
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.trainrColors

// Ours rather than the prebuilt one, so it uses the app's own tokens and is
// correct in light and dark for free, and so the full renewal price can be the
// largest element on each row — which Play requires in size as well as position.
@Composable
fun ProPaywallScreen(
    reason: PaywallReason?,
    plans: List<PaywallPlan>,
    selectedId: String?,
    isWorking: Boolean,
    onSelect: (String) -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val colors = MaterialTheme.trainrColors
    val selected = plans.firstOrNull { it.id == selectedId }

    // Paints its own ground: as a destination of its own there is nothing behind
    // it but the window, and the window does not follow the app's theme.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfacePage)
    ) {
        TrainrTopBar(onBackClick = onClose, showLogo = false)
        TrainrScreenContent(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pro_name).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onBrand,
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.extraSmall))
                    .background(colors.brandLarge)
                    .padding(horizontal = Spacing.extraSmall, vertical = 3.dp)
            )
            TrainrScreenTitle(text = stringResource(R.string.pro_full_access))
            Column(
                modifier = Modifier.padding(top = Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                if (reason != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                        TrainrSectionTitle(text = stringResource(reason.heading))
                        Text(
                            text = stringResource(reason.detail),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceMuted
                        )
                        Text(
                            text = stringResource(R.string.pro_free_limit),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceMuted
                        )
                    }
                    Text(
                        text = stringResource(R.string.pro_and_more),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface
                    )
                } else {
                    Text(
                        text = stringResource(R.string.pro_free_limit),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted
                    )
                }
                (reason?.others ?: PaywallReason.entries).forEach { feature ->
                    Feature(
                        heading = stringResource(feature.heading),
                        detail = stringResource(feature.detail)
                    )
                }
                Feature(
                    heading = stringResource(R.string.pro_feature_support_title),
                    detail = stringResource(R.string.pro_feature_support_body)
                )
            }
            Comparison()
            Questions()
            Column(
                modifier = Modifier.padding(top = Spacing.section),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                // The full disclosure lives here rather than under the button:
                // it runs to several lines, and the purchase bar is pinned.
                if (selected == null || selected.renews) {
                    Text(
                        text = stringResource(R.string.pro_renewal_google),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted
                    )
                }
                Text(
                    text = stringResource(R.string.pro_support_trouble),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    Text(
                        text = stringResource(R.string.pro_restore),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.brandStrong,
                        modifier = Modifier.clickable(enabled = !isWorking, onClick = onRestore)
                    )
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
        }
        PurchaseBar(
            plans = plans,
            selected = selected,
            isWorking = isWorking,
            onSelect = onSelect,
            onBuy = onBuy,
            onClose = onClose
        )
    }
}

// Free against Pro, line by line, for anyone who would rather read a table than
// a list of promises.
@Composable
private fun Comparison() {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = Modifier.padding(top = Spacing.section),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        TrainrSectionTitle(text = stringResource(R.string.pro_compare_title))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            ColumnHead(stringResource(R.string.pro_compare_free))
            ColumnHead(stringResource(R.string.pro_compare_pro))
        }
        COMPARISON.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(row.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MarkCell(row.free, isPro = false)
                MarkCell(row.pro, isPro = true)
            }
        }
    }
}

private data class ComparisonRow(
    @StringRes val label: Int,
    val free: Mark,
    val pro: Mark
)

private sealed interface Mark {
    data object Yes : Mark
    data object No : Mark
    data class Count(@StringRes val res: Int) : Mark
}

private val COMPARISON = listOf(
    ComparisonRow(R.string.pro_compare_logging, Mark.Yes, Mark.Yes),
    ComparisonRow(R.string.pro_compare_timer, Mark.Yes, Mark.Yes),
    ComparisonRow(R.string.pro_compare_history, Mark.Yes, Mark.Yes),
    ComparisonRow(R.string.pro_compare_repeat, Mark.Yes, Mark.Yes),
    ComparisonRow(
        R.string.pro_compare_generated,
        Mark.Count(R.string.pro_compare_one),
        Mark.Count(R.string.pro_compare_unlimited)
    ),
    ComparisonRow(
        R.string.pro_compare_rewrite,
        Mark.No,
        Mark.Count(R.string.pro_compare_unlimited)
    ),
    ComparisonRow(
        R.string.pro_compare_fresh,
        Mark.No,
        Mark.Count(R.string.pro_compare_unlimited)
    )
)

@Composable
private fun ColumnHead(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.trainrColors.onSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(MARK_WIDTH)
    )
}

@Composable
private fun MarkCell(mark: Mark, isPro: Boolean) {
    val colors = MaterialTheme.trainrColors
    Text(
        text = when (mark) {
            Mark.Yes -> "\u2713"
            Mark.No -> "\u2014"
            is Mark.Count -> stringResource(mark.res)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (isPro) colors.brandStrong else colors.onSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(MARK_WIDTH)
    )
}

// Answered on the paywall rather than in a support inbox: every one of these
// was a question someone would otherwise have to buy to find out.
@Composable
private fun Questions() {
    val colors = MaterialTheme.trainrColors
    var open by remember { mutableStateOf<Int?>(null) }
    val questions = listOf(
        R.string.pro_faq_includes_q to R.string.pro_faq_includes_a,
        R.string.pro_faq_free_q to R.string.pro_faq_free_a,
        R.string.pro_faq_human_q to R.string.pro_faq_human_a,
        R.string.pro_faq_renew_q to R.string.pro_faq_renew_a,
        R.string.pro_faq_cancel_q to R.string.pro_faq_cancel_a,
        R.string.pro_faq_devices_q to R.string.pro_faq_devices_a
    )

    Column(
        modifier = Modifier.padding(top = Spacing.section),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        TrainrSectionTitle(text = stringResource(R.string.pro_questions))
        questions.forEach { (question, answer) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Spacing.small))
                    .background(colors.surfaceSunken)
                    .clickable { open = if (open == question) null else question }
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(question),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface
                )
                if (open == question) {
                    Text(
                        text = stringResource(answer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceMuted
                    )
                }
            }
        }
    }
}

private val MARK_WIDTH = 64.dp

@Composable
private fun Feature(heading: String, detail: String) {
    val colors = MaterialTheme.trainrColors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted
        )
    }
}

@Composable
private fun PurchaseBar(
    plans: List<PaywallPlan>,
    selected: PaywallPlan?,
    isWorking: Boolean,
    onSelect: (String) -> Unit,
    onBuy: () -> Unit,
    onClose: () -> Unit
) {
    val colors = MaterialTheme.trainrColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfacePanel)
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        if (plans.isEmpty()) {
            Text(
                text = stringResource(R.string.pro_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isSelected = plan.id == selected?.id,
                        onClick = { onSelect(plan.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        TrainrButton(
            text = callToAction(selected),
            onClick = onBuy,
            enabled = selected != null && !isWorking,
            modifier = Modifier.fillMaxWidth()
        )
        // Nothing for a lifetime purchase, which never renews, and naming the
        // trial where one exists: an introductory offer has to say what it
        // costs once it ends.
        renewalNote(selected)?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = stringResource(R.string.pro_not_now),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClose)
        )
    }
}

@Composable
private fun callToAction(selected: PaywallPlan?): String = when {
    selected == null -> stringResource(R.string.pro_subscribe)
    !selected.renews -> stringResource(R.string.pro_buy_lifetime)
    else -> stringResource(R.string.pro_subscribe_to, stringResource(selected.termRes))
}

@Composable
private fun renewalNote(selected: PaywallPlan?): String? = when {
    selected != null && !selected.renews -> null
    selected?.trial != null -> stringResource(R.string.pro_trial_then, selected.trial, selected.price)
    else -> stringResource(R.string.pro_cancel_anytime)
}

@Composable
private fun PlanCard(
    plan: PaywallPlan,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.trainrColors
    val ink = if (isSelected) colors.onSurfaceSelected else colors.onSurface

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Spacing.small))
            .background(if (isSelected) colors.surfaceSelected else colors.surfaceCard)
            .border(1.dp, if (isSelected) colors.brandLarge else colors.outlineControl,
                RoundedCornerShape(Spacing.small))
            .clickable(onClick = onClick)
    ) {
        plan.savePercent?.let { saved ->
            Text(
                text = stringResource(R.string.pro_save_percent, saved),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onBrand,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.brandLarge)
                    .padding(vertical = 3.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.small, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(plan.termRes),
                style = MaterialTheme.typography.labelLarge,
                color = ink
            )
            Text(text = plan.price, style = MaterialTheme.typography.titleSmall, color = ink)
            Text(
                text = stringResource(plan.billingRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) colors.onSurfaceSelected else colors.onSurfaceMuted
            )
        }
    }
}
