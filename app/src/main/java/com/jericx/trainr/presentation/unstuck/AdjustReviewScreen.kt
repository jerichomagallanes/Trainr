package com.jericx.trainr.presentation.unstuck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.InfeasibleReason
import com.jericx.trainr.domain.unstuck.ProposalKind
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.domain.unstuck.TradeoffCode
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrQuietButton
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.getLocalizedName
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
private val RuleWidth = 3.dp

@Composable
fun AdjustReviewScreen(
    review: ReviewUi,
    modifier: Modifier = Modifier,
    applyError: ApplyErrorUi? = null,
    onApply: () -> Unit = {},
    onKeepOriginal: () -> Unit = {},
    onFinishEarly: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    TrainrScaffold(
        onBackClick = onBack,
        bottomButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                ApplyErrorLine(applyError)
                when (review) {
                    is ReviewUi.Proposed -> {
                        TrainrButton(
                            text = stringResource(
                                if (applyError == ApplyErrorUi.NOT_APPLIED) {
                                    R.string.retry
                                } else {
                                    R.string.use_this_workout
                                }
                            ),
                            onClick = onApply
                        )
                        TrainrQuietButton(
                            text = stringResource(R.string.keep_original),
                            onClick = onKeepOriginal
                        )
                    }

                    is ReviewUi.NoChange -> TrainrButton(
                        text = stringResource(R.string.continue_workout),
                        onClick = onKeepOriginal
                    )

                    is ReviewUi.Infeasible -> {
                        TrainrButton(
                            text = stringResource(R.string.keep_original),
                            onClick = onKeepOriginal
                        )
                        TrainrButton(
                            text = stringResource(R.string.finish_early),
                            onClick = onFinishEarly,
                            isPrimary = false
                        )
                    }
                }
            }
        }
    ) { padding ->
        TrainrScreenContent(modifier = modifier.padding(padding)) {
            when (review) {
                is ReviewUi.Proposed -> ProposedContent(review)
                is ReviewUi.NoChange -> NoChangeContent(review)
                is ReviewUi.Infeasible -> InfeasibleContent(review)
            }
        }
    }
}

@Composable
private fun ColumnScope.ProposedContent(review: ReviewUi.Proposed) {
    val colors = MaterialTheme.trainrColors
    val rule = colors.brandLarge
    val locale = LocalLocale.current.platformLocale
    val priority = review.priorityName ?: stringResource(review.goalLabelRes)

    FeatureTitle(
        text = if (review.kind == ProposalKind.SUBSTITUTE) {
            stringResource(
                R.string.adjust_review_equipment_title_format,
                review.substituteEquipment?.getLocalizedName()
                    ?.lowercase(locale).orEmpty()
            )
        } else {
            stringResource(R.string.adjust_review_time_title)
        }
    )

    Text(
        text = stringResource(R.string.your_priority),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceMuted,
        modifier = Modifier.padding(top = Spacing.medium)
    )
    Text(
        text = priority,
        style = MaterialTheme.typography.titleLarge,
        color = colors.onSurface
    )

    review.budgetMinutes?.let { minutes ->
        Text(
            text = stringResource(
                if (review.scope == TimeScope.REMAINING) {
                    R.string.adjust_review_remaining_line_format
                } else {
                    R.string.adjust_review_time_line_format
                },
                minutes
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(top = Spacing.small)
        )
    }

    ScopeRow(
        trailing = stringResource(
            if (review.hasPerformedWork) {
                R.string.restore_remaining_plan
            } else {
                R.string.undo_available
            }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.medium)
            .border(1.dp, colors.outlineControl, MaterialTheme.shapes.medium)
            .padding(Spacing.card)
    ) {
        Text(
            text = if (review.priorityName != null) {
                stringResource(R.string.adjust_review_keep_format, review.priorityName)
            } else {
                stringResource(R.string.adjust_review_alternative)
            },
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
        Text(
            text = if (review.replacedFrom != null && review.replacedTo != null) {
                stringResource(
                    R.string.adjust_review_replace_body_format,
                    review.replacedFrom,
                    review.replacedTo
                )
            } else {
                stringResource(R.string.adjust_review_time_body_format, joinAnd(review.keptNames))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(top = Spacing.extraSmall)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.medium)
                .drawBehind { drawRect(rule, size = Size(RuleWidth.toPx(), size.height)) }
                .padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.tradeoff),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            review.tradeoffs.forEach { tradeoff ->
                Text(
                    text = tradeoff.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }
        }

        Disclosure(label = stringResource(R.string.see_exact_changes)) {
            review.rows.forEach { ChangeRowLine(it) }
            Text(
                text = stringResource(
                    if (review.kind == ProposalKind.SUBSTITUTE) {
                        R.string.changes_rest_kept_choose_weight
                    } else {
                        R.string.changes_rest_unchanged
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
    }

    if (review.kind == ProposalKind.SUBSTITUTE) {
        Disclosure(label = stringResource(R.string.how_to_choose_weight)) {
            Text(
                text = stringResource(R.string.choose_weight_body_1),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            Text(
                text = stringResource(R.string.choose_weight_body_2),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
    }

    Disclosure(label = stringResource(R.string.why_this_change)) {
        Text(
            text = if (review.kind == ProposalKind.SUBSTITUTE) {
                stringResource(R.string.why_equipment_format, review.replacedTo.orEmpty())
            } else {
                stringResource(R.string.why_time_format, priority)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface
        )
        Text(
            text = stringResource(R.string.missed_sets_not_added),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(top = Spacing.small)
        )
    }
}

@Composable
private fun ColumnScope.NoChangeContent(review: ReviewUi.NoChange) {
    val colors = MaterialTheme.trainrColors

    FeatureTitle(text = stringResource(R.string.keep_current_workout))
    Text(
        text = stringResource(R.string.your_priority),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceMuted,
        modifier = Modifier.padding(top = Spacing.medium)
    )
    Text(
        text = review.priorityName ?: stringResource(review.goalLabelRes),
        style = MaterialTheme.typography.titleLarge,
        color = colors.onSurface
    )
    Text(
        text = stringResource(R.string.already_fits),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurface,
        modifier = Modifier.padding(top = Spacing.small)
    )
    ScopeRow(
        leading = stringResource(
            R.string.original_workout_planned_format,
            review.plannedMinutes
        ),
        trailing = null
    )
}

@Composable
private fun ColumnScope.InfeasibleContent(review: ReviewUi.Infeasible) {
    // Only the too-short answer measured a minimum; the others would be a
    // number nobody worked out.
    val minimumMinutes = review.minimumMinutes
        ?.takeIf { review.reason == InfeasibleReason.TOO_SHORT_FOR_REQUIRED_WORK }

    when {
        review.reason == InfeasibleReason.NO_ELIGIBLE_SUBSTITUTE -> InfeasibleLines(
            title = stringResource(R.string.no_alternative_title),
            body = stringResource(R.string.no_alternative_body)
        )

        minimumMinutes == null -> InfeasibleLines(
            title = stringResource(R.string.no_adjustment_title),
            body = stringResource(R.string.no_adjustment_body)
        )

        else -> InfeasibleLines(
            title = stringResource(R.string.no_short_version_title),
            body = stringResource(R.string.no_short_version_body_format, minimumMinutes)
        )
    }
}

@Composable
private fun InfeasibleLines(title: String, body: String) {
    FeatureTitle(text = title)
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.trainrColors.onSurface,
        modifier = Modifier.padding(top = Spacing.medium)
    )
}

@Composable
private fun FeatureTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        color = MaterialTheme.trainrColors.onSurface
    )
}

@Composable
private fun ScopeRow(trailing: String?, leading: String = stringResource(R.string.today_only)) {
    val colors = MaterialTheme.trainrColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.medium)
            .background(colors.surfaceSunken, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = Spacing.tight),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = leading,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted
            )
        }
    }
}

@Composable
private fun ChangeRowLine(row: ChangeRowUi) {
    val colors = MaterialTheme.trainrColors

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when (row) {
                    is ChangeRowUi.Reduced -> Text(
                        text = row.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )

                    is ChangeRowUi.Omitted -> Text(
                        text = row.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )

                    is ChangeRowUi.Replaced -> {
                        Text(
                            text = row.fromName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = row.toName,
                            style = MaterialTheme.typography.bodyMedium
                                .copy(fontWeight = FontWeight.Bold),
                            color = colors.onSurface
                        )
                    }
                }
            }
            Text(
                text = when (row) {
                    is ChangeRowUi.Reduced ->
                        pluralStringResource(R.plurals.sets_from_to_format, row.toSets, row.fromSets, row.toSets)

                    is ChangeRowUi.Omitted -> stringResource(R.string.omit_today)
                    is ChangeRowUi.Replaced ->
                        stringResource(R.string.sets_times_reps_format, row.sets, row.reps)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
        HorizontalDivider(color = colors.outlineDivider)
    }
}

@Composable
private fun Disclosure(label: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.trainrColors
    var open by remember { mutableStateOf(false) }
    val expanded = stringResource(R.string.section_expanded)
    val collapsed = stringResource(R.string.section_collapsed)

    Column(modifier = Modifier.padding(top = Spacing.medium)) {
        HorizontalDivider(color = colors.outlineDivider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ComponentHeight.Medium)
                .clickable(role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) expanded else collapsed },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.brandStrong,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (open) 90f else 0f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.brandStrong
            )
        }
        AnimatedVisibility(visible = open) {
            Column(modifier = Modifier.padding(bottom = Spacing.small), content = content)
        }
    }
}

@Composable
private fun ApplyErrorLine(error: ApplyErrorUi?) {
    if (error == null) return

    Text(
        text = stringResource(
            if (error == ApplyErrorUi.STALE_REBUILT) {
                R.string.review_rebuilt
            } else {
                R.string.review_not_applied
            }
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.trainrColors.dangerInk
    )
}

@Composable
private fun TradeoffUi.text(): String = when (code) {
    TradeoffCode.LESS_WORK_FOR_REGIONS -> stringResource(
        R.string.tradeoff_less_work_format,
        joinAnd(regions.map { stringResource(it.labelRes) })
    )

    TradeoffCode.REDUCED_SESSION -> stringResource(R.string.tradeoff_reduced_session)
    TradeoffCode.DIFFERENT_RESISTANCE -> stringResource(R.string.tradeoff_different_resistance)
    TradeoffCode.LESS_BARBELL_PRACTICE ->
        stringResource(R.string.tradeoff_less_barbell_format, exerciseName.orEmpty())

    TradeoffCode.SEPARATE_LOAD_HISTORY -> stringResource(R.string.tradeoff_separate_history)
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun AdjustReviewProposedPreview() {
    TrainrTheme {
        AdjustReviewScreen(review = SampleAdjustmentStates.shorterReview)
    }
}

@Preview(showBackground = true, heightDp = 1200, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdjustReviewSubstitutePreview() {
    TrainrTheme(darkTheme = true) {
        AdjustReviewScreen(review = SampleAdjustmentStates.substituteReview)
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustReviewNoChangePreview() {
    TrainrTheme {
        AdjustReviewScreen(review = SampleAdjustmentStates.noChangeReview)
    }
}

@Preview(showBackground = true)
@Composable
private fun AdjustReviewInfeasiblePreview() {
    TrainrTheme {
        AdjustReviewScreen(review = SampleAdjustmentStates.infeasibleReview)
    }
}
