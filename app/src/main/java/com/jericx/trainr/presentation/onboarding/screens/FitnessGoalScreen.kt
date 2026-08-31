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
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.components.cards.TrainrIconCard
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.layout.TrainrFormSection
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSubtitle

// Goal and style are two different questions — the outcome wanted, and the kind
// of training wanted to get there — but they are the same kind of question, and
// the review shows them on one card. They are asked together so the card's Edit
// can reach both: style used to be collected on the limitations screen, which
// left it displayed under a button that could not change it.
@Composable
fun FitnessGoalScreen(
    initial: UserProfile? = null,
    isEditing: Boolean = false,
    onNextClick: (FitnessGoal, WorkoutType) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedGoal by remember { mutableStateOf(initial?.fitnessGoal) }

    // Shown as chosen rather than left blank. The profile falls back to mixed
    // either way, and a list with nothing selected that quietly means "mixed"
    // decides for the client without telling them.
    var selectedStyle by remember { mutableStateOf(initial?.workoutType ?: WorkoutType.MIXED) }

    TrainrScaffold(
        onBackClick = onBackClick,
        closeInsteadOfBack = isEditing,
        bottomButton = {
            TrainrButton(
                text = stringResource(if (isEditing) R.string.save else R.string.next),
                onClick = {
                    selectedGoal?.let { onNextClick(it, selectedStyle) }
                },
                // Only the goal is required. Style arrives already answered, so
                // gating on it would block a client who agrees with the default.
                enabled = selectedGoal != null
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
                    currentStep = 3,
                    totalSteps = 7,
                    modifier = Modifier.padding(horizontal = Spacing.large)
                )
            }

            TrainrScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrScreenTitle(text = stringResource(R.string.your_fitness_goals))

                Spacer(modifier = Modifier.height(Spacing.small))

                TrainrSubtitle(
                    text = stringResource(R.string.goal_description)
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                // Titled with the same words the review uses for the row it
                // fills in, so the screen answers the line that was tapped.
                TrainrFormSection(title = stringResource(R.string.main_goal_label)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.card)
                    ) {
                        TrainrIconCard(
                            iconRes = R.drawable.ic_mode_heat,
                            title = stringResource(R.string.lose_weight),
                            description = stringResource(R.string.lose_weight_description),
                            isSelected = selectedGoal == FitnessGoal.WEIGHT_LOSS,
                            onClick = { selectedGoal = FitnessGoal.WEIGHT_LOSS }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_exercise,
                            title = stringResource(R.string.build_muscle),
                            description = stringResource(R.string.build_muscle_description),
                            isSelected = selectedGoal == FitnessGoal.MUSCLE_GAIN,
                            onClick = { selectedGoal = FitnessGoal.MUSCLE_GAIN }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_electric_bolt,
                            title = stringResource(R.string.get_stronger),
                            description = stringResource(R.string.get_stronger_description),
                            isSelected = selectedGoal == FitnessGoal.STRENGTH,
                            onClick = { selectedGoal = FitnessGoal.STRENGTH }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_directions_run,
                            title = stringResource(R.string.improve_endurance),
                            description = stringResource(R.string.improve_endurance_description),
                            isSelected = selectedGoal == FitnessGoal.ENDURANCE,
                            onClick = { selectedGoal = FitnessGoal.ENDURANCE }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_emoji_people,
                            title = stringResource(R.string.general_fitness),
                            description = stringResource(R.string.general_fitness_description),
                            isSelected = selectedGoal == FitnessGoal.GENERAL_FITNESS,
                            onClick = { selectedGoal = FitnessGoal.GENERAL_FITNESS }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_self_improvement,
                            title = stringResource(R.string.flexibility_mobility),
                            description = stringResource(R.string.flexibility_mobility_description),
                            isSelected = selectedGoal == FitnessGoal.FLEXIBILITY,
                            onClick = { selectedGoal = FitnessGoal.FLEXIBILITY }
                        )
                    }
                }

                TrainrFormSection(
                    title = stringResource(R.string.preferred_workout_style)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.card)
                    ) {
                        TrainrIconCard(
                            iconRes = R.drawable.ic_exercise,
                            title = stringResource(R.string.strength_training),
                            description = stringResource(R.string.strength_training_description),
                            isSelected = selectedStyle == WorkoutType.STRENGTH,
                            onClick = { selectedStyle = WorkoutType.STRENGTH }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_directions_run,
                            title = stringResource(R.string.cardio),
                            description = stringResource(R.string.cardio_description),
                            isSelected = selectedStyle == WorkoutType.CARDIO,
                            onClick = { selectedStyle = WorkoutType.CARDIO }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_electric_bolt,
                            title = stringResource(R.string.hiit),
                            description = stringResource(R.string.hiit_description),
                            isSelected = selectedStyle == WorkoutType.HIIT,
                            onClick = { selectedStyle = WorkoutType.HIIT }
                        )

                        TrainrIconCard(
                            iconRes = R.drawable.ic_emoji_people,
                            title = stringResource(R.string.mixed_balanced),
                            description = stringResource(R.string.mixed_balanced_description),
                            isSelected = selectedStyle == WorkoutType.MIXED,
                            onClick = { selectedStyle = WorkoutType.MIXED }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))
            }
        }
    }
}
