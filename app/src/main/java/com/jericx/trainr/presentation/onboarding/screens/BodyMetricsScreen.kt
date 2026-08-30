package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.common.Constants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.TextMuted
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.core.TrainrTextField
import com.jericx.trainr.presentation.common.components.core.TrainrToggleChip
import com.jericx.trainr.presentation.common.components.layout.TrainrFormSection
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSubtitle
import com.jericx.trainr.presentation.onboarding.util.BodyMetricsConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    initial: UserProfile? = null,
    isEditing: Boolean = false,
    onNextClick: (height: Float, weight: Float) -> Unit,
    onBackClick: () -> Unit
) {
    var height by remember {
        mutableStateOf(initial?.height?.takeIf { it > 0f }?.toInt()?.toString().orEmpty())
    }
    var weight by remember {
        mutableStateOf(
            initial?.weight?.takeIf { it > 0f }
                ?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }
                .orEmpty()
        )
    }
    var useMetric by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current

    val isFormValid = height.isNotBlank() && weight.isNotBlank()

    // Switching units rewrites both field values, swaps the height keyboard
    // between decimal and text, and changes which input each field accepts.
    // Focus is cleared first so the user is never left with a caret sitting in
    // a value they did not type, in a field that now silently rejects most
    // keystrokes. Re-selecting the unit already in use is a no-op, so it does
    // not steal focus.
    fun switchUnits(toMetric: Boolean) {
        if (toMetric == useMetric) return

        focusManager.clearFocus()

        if (toMetric) {
            height = BodyMetricsConverter.convertHeightToMetric(height)
            weight = BodyMetricsConverter.convertWeightToMetric(weight)
        } else {
            height = BodyMetricsConverter.convertHeightToImperial(height)
            weight = BodyMetricsConverter.convertWeightToImperial(weight)
        }

        useMetric = toMetric
    }

    TrainrScaffold(
        onBackClick = onBackClick,
        closeInsteadOfBack = isEditing,
        bottomButton = {
            TrainrButton(
                text = stringResource(if (isEditing) R.string.save else R.string.next),
                onClick = {
                    val (h, w) = BodyMetricsConverter.parseMetrics(height, weight, useMetric)
                    onNextClick(h, w)
                },
                enabled = isFormValid
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TrainrProgress(
                currentStep = 2,
                totalSteps = 7,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )

            TrainrScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrScreenTitle(text = stringResource(R.string.your_measurements))

                Spacer(modifier = Modifier.height(Spacing.small))

                TrainrSubtitle(
                    text = stringResource(R.string.measurements_description)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.card)
                ) {
                    UnitTab(
                        text = stringResource(R.string.metric),
                        selected = useMetric,
                        onClick = { switchUnits(toMetric = true) },
                        modifier = Modifier.weight(1f)
                    )
                    UnitTab(
                        text = stringResource(R.string.imperial),
                        selected = !useMetric,
                        onClick = { switchUnits(toMetric = false) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrFormSection(
                    title = if (useMetric) stringResource(R.string.height_cm) else stringResource(R.string.height_ft_in)
                ) {
                    TrainrTextField(
                        value = height,
                        onValueChange = {
                            if (useMetric) {
                                if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) {
                                    height = it
                                }
                            } else {
                                if (it.matches(Regex("^\\d{0,1}'?\\d{0,2}\"?$"))) {
                                    height = it
                                }
                            }
                        },
                        placeholder = if (useMetric) stringResource(R.string.height_placeholder_cm) else stringResource(R.string.height_placeholder_imperial),
                        keyboardType = if (useMetric) KeyboardType.Decimal else KeyboardType.Text
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrFormSection(
                    title = if (useMetric) stringResource(R.string.weight_kg) else stringResource(R.string.weight_lbs)
                ) {
                    TrainrTextField(
                        value = weight,
                        onValueChange = {
                            if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) {
                                weight = it
                            }
                        },
                        placeholder = if (useMetric) stringResource(R.string.weight_placeholder_kg) else stringResource(R.string.weight_placeholder_lbs),
                        keyboardType = KeyboardType.Decimal
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                val bmi = BodyMetricsConverter.calculateBMI(height, weight, useMetric)
                if (bmi != null) {
                    BMICard(bmi = bmi)
                }

                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}

// The frames draw the unit switch as tabs: square-bottomed segments that sit
// on the fields they control, not free-floating chips.
@Composable
private fun UnitTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 10.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(ComponentHeight.ChipTall),
        shape = shape,
        color = if (selected)
            MaterialTheme.colorScheme.onBackground
        else
            MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected)
                    MaterialTheme.colorScheme.background
                else
                    TextMuted
            )
        }
    }
}

@Composable
private fun BMICard(bmi: Float) {
    // Read through the composition local so the value re-formats if the locale changes.
    val locale = LocalLocale.current.platformLocale

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        append(stringResource(R.string.bmi_label) + " ")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append(String.format(locale, "%.1f", bmi))
                    }
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = getBMICategory(bmi),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun getBMICategory(bmi: Float): String {
    return when {
        bmi < Constants.Workout.BMI_UNDERWEIGHT_THRESHOLD -> stringResource(R.string.underweight)
        bmi < Constants.Workout.BMI_NORMAL_THRESHOLD -> stringResource(R.string.normal_weight)
        bmi < Constants.Workout.BMI_OVERWEIGHT_THRESHOLD -> stringResource(R.string.overweight)
        else -> stringResource(R.string.obese)
    }
}