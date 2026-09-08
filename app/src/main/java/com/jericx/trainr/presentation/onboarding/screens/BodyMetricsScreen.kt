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
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.common.Constants
import com.jericx.trainr.presentation.common.components.core.touchedOnBlur
import com.jericx.trainr.presentation.common.components.core.TrainrFieldError
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.trainrColors
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
    onNextClick: (height: Float, weight: Float, units: UnitSystem) -> Unit,
    onBackClick: () -> Unit
) {
    // The profile stores centimetres and kilograms whichever units were typed,
    // so seeded values have to be converted for a client who reads pounds.
    val startsImperial = initial?.bodyUnitSystem == UnitSystem.IMPERIAL
    var height by remember {
        mutableStateOf(
            initial?.height?.takeIf { it > 0f }
                ?.toInt()
                ?.toString()
                ?.let { cm ->
                    if (startsImperial) BodyMetricsConverter.convertHeightToImperial(cm) else cm
                }
                .orEmpty()
        )
    }
    var weight by remember {
        mutableStateOf(
            initial?.weight?.takeIf { it > 0f }
                ?.let { kg ->
                    if (startsImperial) {
                        BodyMetricsConverter.convertWeightToImperial(kg.toString())
                    } else {
                        BodyMetricsConverter.formatKilograms(kg)
                    }
                }
                .orEmpty()
        )
    }
    var useMetric by remember { mutableStateOf(!startsImperial) }

    val focusManager = LocalFocusManager.current

    // Validated on what the text parses to, not on whether anything was typed:
    // the imperial filter allows "595", which parses to a height of zero.
    val (parsedHeightCm, parsedWeightKg) =
        BodyMetricsConverter.parseMetrics(height, weight, useMetric)
    val heightIsUsable = parsedHeightCm in
        Constants.Workout.MIN_HEIGHT_CM..Constants.Workout.MAX_HEIGHT_CM
    val weightIsUsable = parsedWeightKg in
        Constants.Workout.MIN_WEIGHT_KG..Constants.Workout.MAX_WEIGHT_KG
    val cmUnit = stringResource(R.string.unit_cm)
    val kgUnit = stringResource(R.string.weight_column)
    val lbsUnit = stringResource(R.string.weight_column_lbs)

    val isFormValid = heightIsUsable && weightIsUsable

    var heightTouched by remember { mutableStateOf(false) }
    var weightTouched by remember { mutableStateOf(false) }

    val heightBounds = with(Constants.Workout) {
        if (useMetric) {
            MIN_HEIGHT_CM.toInt().toString() to
                "${MAX_HEIGHT_CM.toInt()} ${cmUnit}"
        } else {
            BodyMetricsConverter.convertHeightToImperial(MIN_HEIGHT_CM.toInt().toString()) to
                BodyMetricsConverter.convertHeightToImperial(MAX_HEIGHT_CM.toInt().toString())
        }
    }
    val weightBounds = with(Constants.Workout) {
        if (useMetric) {
            MIN_WEIGHT_KG.toInt().toString() to "${MAX_WEIGHT_KG.toInt()} ${kgUnit}"
        } else {
            BodyMetricsConverter.convertWeightToImperial(MIN_WEIGHT_KG.toString()) to
                "${BodyMetricsConverter.convertWeightToImperial(MAX_WEIGHT_KG.toString())} ${lbsUnit}"
        }
    }

    // Focus is cleared first because the switch rewrites both values and
    // changes which keystrokes each field accepts; re-selecting the unit
    // already in use is a no-op so it does not steal focus.
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
                    onNextClick(
                        h,
                        w,
                        if (useMetric) UnitSystem.METRIC else UnitSystem.IMPERIAL
                    )
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
            if (!isEditing) {
                TrainrProgress(
                    currentStep = 2,
                    totalSteps = 7,
                    modifier = Modifier.padding(horizontal = Spacing.large)
                )
            }

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
                            BodyMetricsConverter.acceptedHeight(it, useMetric)?.let { accepted ->
                                height = accepted
                            }
                        },
                        placeholder = if (useMetric) stringResource(R.string.height_placeholder_cm) else stringResource(R.string.height_placeholder_imperial),
                        keyboardType = if (useMetric) KeyboardType.Decimal else KeyboardType.Text,
                        modifier = Modifier.touchedOnBlur { heightTouched = true }
                    )

                    TrainrFieldError(
                        message = fieldMessage(
                            label = stringResource(R.string.height_label),
                            missing = stringResource(R.string.error_enter_height),
                            value = height,
                            touched = heightTouched,
                            usable = heightIsUsable,
                            bounds = heightBounds
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrFormSection(
                    title = if (useMetric) stringResource(R.string.weight_kg) else stringResource(R.string.weight_lbs)
                ) {
                    TrainrTextField(
                        value = weight,
                        onValueChange = {
                            // Four digits: the upper bound is 650 kg, which is
                            // 1433 lbs, and three digits could not reach it.
                            if (it.matches(Regex("^\\d{0,4}(\\.\\d{0,1})?$"))) {
                                weight = it
                            }
                        },
                        placeholder = if (useMetric) stringResource(R.string.weight_placeholder_kg) else stringResource(R.string.weight_placeholder_lbs),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.touchedOnBlur { weightTouched = true }
                    )

                    TrainrFieldError(
                        message = fieldMessage(
                            label = stringResource(R.string.weight_label),
                            missing = stringResource(R.string.error_enter_weight),
                            value = weight,
                            touched = weightTouched,
                            usable = weightIsUsable,
                            bounds = weightBounds
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                // Only for accepted measurements: a refused 300 cm and 2 kg
                // otherwise gets a BMI of 0.2 labelled "Underweight".
                val bmi = BodyMetricsConverter.calculateBMI(height, weight, useMetric)
                    ?.takeIf { isFormValid }
                if (bmi != null) {
                    BMICard(bmi = bmi)
                }

                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}

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
            MaterialTheme.trainrColors.surfaceSelected
        else
            MaterialTheme.trainrColors.surfaceCard,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.trainrColors.outlineControl)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected)
                    MaterialTheme.trainrColors.onSurfaceSelected
                else
                    MaterialTheme.trainrColors.onSurfaceMuted
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
        color = MaterialTheme.trainrColors.surfaceSunken
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.trainrColors.onSurface
                        )
                    ) {
                        append(stringResource(R.string.bmi_label) + " ")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.trainrColors.brandStrong
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
                color = MaterialTheme.trainrColors.brandStrong
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

@Composable
private fun fieldMessage(
    label: String,
    missing: String,
    value: String,
    touched: Boolean,
    usable: Boolean,
    bounds: Pair<String, String>
): String? = when {
    value.isBlank() && touched -> missing
    value.isNotBlank() && !usable ->
        stringResource(R.string.value_range_hint, label, bounds.first, bounds.second)
    else -> null
}
