package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.cards.OnboardingIconCard
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingButton
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingProgress
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScaffold
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScreenContent
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingScreenTitle
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingSubtitle

@Composable
fun FitnessGoalScreen(
    onNextClick: (FitnessGoal) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedGoal by remember { mutableStateOf<FitnessGoal?>(null) }

    OnboardingScaffold(
        onBackClick = onBackClick,
        bottomButton = {
            OnboardingButton(
                text = stringResource(R.string.next),
                onClick = {
                    selectedGoal?.let { onNextClick(it) }
                },
                enabled = selectedGoal != null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OnboardingProgress(
                currentStep = 3,
                totalSteps = 7,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )

            OnboardingScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingScreenTitle(text = stringResource(R.string.main_fitness_goal))

                Spacer(modifier = Modifier.height(Spacing.small))

                OnboardingSubtitle(
                    text = stringResource(R.string.goal_description)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    OnboardingIconCard(
                        iconRes = R.drawable.ic_mode_heat,
                        title = stringResource(R.string.lose_weight),
                        description = stringResource(R.string.lose_weight_description),
                        isSelected = selectedGoal == FitnessGoal.WEIGHT_LOSS,
                        onClick = { selectedGoal = FitnessGoal.WEIGHT_LOSS }
                    )

                    OnboardingIconCard(
                        iconRes = R.drawable.ic_exercise,
                        title = stringResource(R.string.build_muscle),
                        description = stringResource(R.string.build_muscle_description),
                        isSelected = selectedGoal == FitnessGoal.MUSCLE_GAIN,
                        onClick = { selectedGoal = FitnessGoal.MUSCLE_GAIN }
                    )

                    OnboardingIconCard(
                        iconRes = R.drawable.ic_electric_bolt,
                        title = stringResource(R.string.get_stronger),
                        description = stringResource(R.string.get_stronger_description),
                        isSelected = selectedGoal == FitnessGoal.STRENGTH,
                        onClick = { selectedGoal = FitnessGoal.STRENGTH }
                    )

                    OnboardingIconCard(
                        iconRes = R.drawable.ic_directions_run,
                        title = stringResource(R.string.improve_endurance),
                        description = stringResource(R.string.improve_endurance_description),
                        isSelected = selectedGoal == FitnessGoal.ENDURANCE,
                        onClick = { selectedGoal = FitnessGoal.ENDURANCE }
                    )

                    OnboardingIconCard(
                        iconRes = R.drawable.ic_emoji_people,
                        title = stringResource(R.string.general_fitness),
                        description = stringResource(R.string.general_fitness_description),
                        isSelected = selectedGoal == FitnessGoal.GENERAL_FITNESS,
                        onClick = { selectedGoal = FitnessGoal.GENERAL_FITNESS }
                    )

                    OnboardingIconCard(
                        iconRes = R.drawable.ic_self_improvement,
                        title = stringResource(R.string.flexibility_mobility),
                        description = stringResource(R.string.flexibility_mobility_description),
                        isSelected = selectedGoal == FitnessGoal.FLEXIBILITY,
                        onClick = { selectedGoal = FitnessGoal.FLEXIBILITY }
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))
            }
        }
    }
}