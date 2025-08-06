package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.common.Constants
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.cards.OnboardingIconCard
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingButton
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingCheckboxChip
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingProgress
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingFormSection
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScaffold
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScreenContent
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingScreenTitle
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingSectionTitle
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingSubtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitationsScreen(
    onNextClick: (injuries: List<String>, workoutType: WorkoutType) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedInjuries by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.MIXED) }

    val injuryOptions = listOf(
        stringResource(R.string.lower_back_pain_injury),
        stringResource(R.string.knee_problems_injury),
        stringResource(R.string.shoulder_injury_injury),
        stringResource(R.string.wrist_pain_injury),
        stringResource(R.string.ankle_issues_injury),
        stringResource(R.string.hip_problems_injury),
        stringResource(R.string.neck_pain_injury),
        stringResource(R.string.none_injury)
    )
    val noneOption = stringResource(R.string.none_injury)
    val noneLabel = stringResource(R.string.none)

    OnboardingScaffold(
        onBackClick = onBackClick,
        bottomButton = {
            OnboardingButton(
                text = stringResource(R.string.submit),
                onClick = {
                    val injuries = selectedInjuries.filter { it != noneLabel }.toList()
                    onNextClick(injuries, selectedWorkoutType)
                },
                enabled = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OnboardingProgress(
                currentStep = 5,
                totalSteps = 7,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )

            OnboardingScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingScreenTitle(text = stringResource(R.string.lets_keep_you_safe))

                Spacer(modifier = Modifier.height(Spacing.small))

                OnboardingSubtitle(
                    text = stringResource(R.string.limitations_description)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingSectionTitle(stringResource(R.string.any_injuries_or_areas))

                Spacer(modifier = Modifier.height(Spacing.medium))

                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    injuryOptions.forEach { injury ->
                        val isNone = injury == noneOption

                        OnboardingCheckboxChip(
                            text = injury,
                            checked = selectedInjuries.contains(injury),
                            onCheckedChange = { isChecked ->
                                selectedInjuries = if (isNone) {
                                    if (isChecked) {
                                        setOf(injury)
                                    } else {
                                        emptySet()
                                    }
                                } else {
                                    val withoutNone = selectedInjuries - noneOption
                                    if (isChecked) {
                                        withoutNone + injury
                                    } else {
                                        withoutNone - injury
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingFormSection(
                    title = stringResource(R.string.preferred_workout_style)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        OnboardingIconCard(
                            iconRes = R.drawable.ic_exercise,
                            title = stringResource(R.string.strength_training),
                            description = stringResource(R.string.strength_training_description),
                            isSelected = selectedWorkoutType == WorkoutType.STRENGTH,
                            onClick = { selectedWorkoutType = WorkoutType.STRENGTH }
                        )

                        OnboardingIconCard(
                            iconRes = R.drawable.ic_directions_run,
                            title = stringResource(R.string.cardio),
                            description = stringResource(R.string.cardio_description),
                            isSelected = selectedWorkoutType == WorkoutType.CARDIO,
                            onClick = { selectedWorkoutType = WorkoutType.CARDIO }
                        )

                        OnboardingIconCard(
                            iconRes = R.drawable.ic_electric_bolt,
                            title = stringResource(R.string.hiit),
                            description = stringResource(R.string.hiit_description),
                            isSelected = selectedWorkoutType == WorkoutType.HIIT,
                            onClick = { selectedWorkoutType = WorkoutType.HIIT }
                        )

                        OnboardingIconCard(
                            iconRes = R.drawable.ic_emoji_people,
                            title = stringResource(R.string.mixed_balanced),
                            description = stringResource(R.string.mixed_balanced_description),
                            isSelected = selectedWorkoutType == WorkoutType.MIXED,
                            onClick = { selectedWorkoutType = WorkoutType.MIXED }
                        )
                    }
                }
            }
        }
    }
}