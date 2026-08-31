package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSubtitle
import com.jericx.trainr.presentation.common.getFocusPhrase
import com.jericx.trainr.presentation.common.getLocalizedName
import com.jericx.trainr.presentation.common.getProgramPhrase
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.theme.TextMuted
import com.jericx.trainr.presentation.onboarding.util.BodyMetricsConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    userProfile: UserProfile,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    isRegenerating: Boolean = false,
    // Saving the profile on its own: no plan is generated, so the button says
    // what it does and the subtitle explains when the change lands.
    isProfileUpdate: Boolean = false,
    onEditPersonal: () -> Unit = {},
    onEditGoals: () -> Unit = {},
    onEditSetup: () -> Unit = {},
    onEditLimitations: () -> Unit = {}
) {
    TrainrScaffold(
        onBackClick = onBackClick,
        topBar = {
            TrainrTopBar(
                onBackClick = onBackClick,
                showLogo = true,
                // Entered from the plan this is a detour, not a flow: X leads
                // back out; during onboarding the arrow steps back as usual.
                closeInsteadOfBack = isRegenerating || isProfileUpdate
            )
        },
        bottomButton = {
            TrainrButton(
                text = stringResource(
                    if (isProfileUpdate) R.string.save_profile
                    else R.string.generate_my_workout_plan
                ),
                onClick = onConfirmClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Reached from the plan — to change a detail or to start over —
            // there is no seven-step run to be six sevenths of.
            if (!isRegenerating && !isProfileUpdate) {
                TrainrProgress(
                    currentStep = 6,
                    totalSteps = 7,
                    modifier = Modifier.padding(horizontal = Spacing.large)
                )
            }

            TrainrScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrScreenTitle(text = stringResource(R.string.your_fitness_profile))

                Spacer(modifier = Modifier.height(Spacing.small))

                TrainrSubtitle(
                    text = stringResource(
                        if (isProfileUpdate) R.string.review_profile_description
                        else R.string.review_description
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                val genderText = userProfile.gender.getLocalizedName()
                val experienceText = userProfile.experienceLevel.getLocalizedName()

                // Read back in the units they were entered in. The profile is
                // stored in centimetres and kilograms whichever was typed, so
                // a client in pounds was being shown their own weight
                // converted into a number they had not used.
                val imperial = userProfile.unitSystem == UnitSystem.IMPERIAL
                val heightText = if (imperial) {
                    BodyMetricsConverter.convertHeightToImperial(
                        userProfile.height.toInt().toString()
                    )
                } else {
                    stringResource(R.string.height_cm_format, userProfile.height.toInt())
                }
                val weightText = if (imperial) {
                    stringResource(
                        R.string.weight_lbs_format,
                        BodyMetricsConverter.convertWeightToImperial(userProfile.weight.toString())
                    )
                } else {
                    stringResource(R.string.weight_kg_format, userProfile.weight)
                }
                
                ProfileSection(
                    title = stringResource(R.string.personal_information),
                    onEdit = onEditPersonal,
                    items = listOf(
                        stringResource(R.string.age_label) to pluralStringResource(R.plurals.years_old_format, userProfile.age, userProfile.age),
                        stringResource(R.string.gender_label) to genderText,
                        stringResource(R.string.height_label) to heightText,
                        stringResource(R.string.weight_label) to weightText,
                        stringResource(R.string.experience_label) to experienceText
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.screen))

                val fitnessGoalText = userProfile.fitnessGoal.getLocalizedName()
                val workoutTypeText = userProfile.workoutType.getLocalizedName()
                
                ProfileSection(
                    title = stringResource(R.string.fitness_goals_label),
                    onEdit = onEditGoals,
                    items = listOf(
                        stringResource(R.string.main_goal_label) to fitnessGoalText,
                        stringResource(R.string.workout_style_label) to workoutTypeText
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.screen))

                val locationText = userProfile.workoutLocation.getLocalizedName()
                val equipmentText = if (userProfile.availableEquipment.isEmpty() ||
                    userProfile.availableEquipment.contains(Equipment.NONE))
                    stringResource(R.string.bodyweight_only_label)
                else {
                    val equipmentNames = userProfile.availableEquipment.map { equipment ->
                        equipment.getLocalizedName()
                    }
                    equipmentNames.joinToString(", ")
                }
                val preferredTimeText = userProfile.preferredWorkoutTime.getLocalizedName()
                
                ProfileSection(
                    title = stringResource(R.string.workout_setup_label),
                    onEdit = onEditSetup,
                    items = listOf(
                        stringResource(R.string.location_label) to locationText,
                        stringResource(R.string.equipment_label_full) to equipmentText,
                        stringResource(R.string.schedule_label) to if (userProfile.workoutDaysPerWeek == 0)
                            stringResource(R.string.flexible_schedule)
                        else
                            pluralStringResource(
                                R.plurals.days_per_week_format,
                                userProfile.workoutDaysPerWeek,
                                userProfile.workoutDaysPerWeek
                            ),
                        stringResource(R.string.duration_label) to pluralStringResource(
                            R.plurals.duration_minutes_format,
                            userProfile.workoutDuration,
                            userProfile.workoutDuration
                        ),
                        stringResource(R.string.preferred_time_label) to preferredTimeText
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.screen))

                ProfileSection(
                    title = stringResource(R.string.limitations_label),
                    onEdit = onEditLimitations,
                    items = listOf(
                        stringResource(R.string.injuries_concerns_label) to
                            if (userProfile.injuries.isEmpty()) {
                                stringResource(R.string.none_label)
                            } else {
                                userProfile.injuries.joinToString(", ")
                            }
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                // The card promises a routine about to be written; saving the
                // profile alone writes none.
                if (!isProfileUpdate) {
                    AIPreviewCard(userProfile = userProfile)

                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    items: List<Pair<String, String>>,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(Spacing.card),
        verticalArrangement = Arrangement.spacedBy(Spacing.card)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.edit),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }

        items.forEach { (label, value) ->
            ProfileItem(label = label, value = value)
        }
    }
}

@Composable
private fun ProfileItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.card),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AIPreviewCard(userProfile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onBackground)
        ) {
            Row(
                modifier = Modifier.padding(Spacing.medium),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_smart_toy),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.background
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.ai_routine_preview_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.background
                    )
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    Text(
                        text = stringResource(
                            R.string.ai_routine_description,
                            if (userProfile.workoutDaysPerWeek == 0) {
                                stringResource(R.string.flexible_schedule)
                            } else {
                                stringResource(
                                    R.string.program_length_format,
                                    userProfile.workoutDaysPerWeek
                                )
                            },
                            userProfile.workoutType.getProgramPhrase(),
                            userProfile.fitnessGoal.getFocusPhrase()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}