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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.common.Constants
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.util.BodyMetricsConverter
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingButton
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingProgress
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingTextField
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingToggleChip
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingFormSection
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScaffold
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScreenContent
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingScreenTitle
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingSubtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    onNextClick: (height: Float, weight: Float) -> Unit,
    onBackClick: () -> Unit
) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var useMetric by remember { mutableStateOf(true) }

    val isFormValid = height.isNotBlank() && weight.isNotBlank()

    OnboardingScaffold(
        onBackClick = onBackClick,
        bottomButton = {
            OnboardingButton(
                text = stringResource(R.string.next),
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
            OnboardingProgress(
                currentStep = 2,
                totalSteps = 7,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )

            OnboardingScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingScreenTitle(text = stringResource(R.string.your_measurements))

                Spacer(modifier = Modifier.height(Spacing.small))

                OnboardingSubtitle(
                    text = stringResource(R.string.measurements_description)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    OnboardingToggleChip(
                        text = stringResource(R.string.metric),
                        selected = useMetric,
                        onClick = {
                            if (!useMetric) {
                                height = BodyMetricsConverter.convertHeightToMetric(height)
                                weight = BodyMetricsConverter.convertWeightToMetric(weight)
                                useMetric = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OnboardingToggleChip(
                        text = stringResource(R.string.imperial),
                        selected = !useMetric,
                        onClick = {
                            if (useMetric) {
                                height = BodyMetricsConverter.convertHeightToImperial(height)
                                weight = BodyMetricsConverter.convertWeightToImperial(weight)
                                useMetric = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingFormSection(
                    title = if (useMetric) stringResource(R.string.height_cm) else stringResource(R.string.height_ft_in)
                ) {
                    OnboardingTextField(
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

                OnboardingFormSection(
                    title = if (useMetric) stringResource(R.string.weight_kg) else stringResource(R.string.weight_lbs)
                ) {
                    OnboardingTextField(
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

@Composable
private fun BMICard(bmi: Float) {
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
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        append(stringResource(R.string.bmi_label) + " ")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append(String.format("%.1f", bmi))
                    }
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = getBMICategory(bmi),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
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