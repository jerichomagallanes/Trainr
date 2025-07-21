package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingButton
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingProgress
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScaffold
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScreenContent
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingTopBar
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingScreenTitle
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingSubtitle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    userProfile: UserProfile,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit
) {
    OnboardingScaffold(
        onBackClick = onBackClick,
        topBar = {
            OnboardingTopBar(onBackClick = onBackClick, showLogo = true)
        },
        bottomButton = {
            OnboardingButton(
                text = stringResource(R.string.generate_my_workout_plan),
                onClick = onConfirmClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OnboardingProgress(
                currentStep = 6,
                totalSteps = 7,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )

            OnboardingScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                OnboardingScreenTitle(text = stringResource(R.string.your_fitness_profile))

                Spacer(modifier = Modifier.height(Spacing.small))

                OnboardingSubtitle(
                    text = stringResource(R.string.review_description)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                ProfileSection(
                    title = "Personal Information",
                    items = listOf(
                        "Age" to "${userProfile.age} years old",
                        "Gender" to userProfile.gender.name.replace("_", " ")
                            .lowercase()
                            .replaceFirstChar { it.titlecase() },
                        "Height" to "${userProfile.height.toInt()} cm",
                        "Weight" to "${userProfile.weight} kg",
                        "Experience" to userProfile.experienceLevel.name
                            .lowercase()
                            .replaceFirstChar { it.titlecase() }
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                ProfileSection(
                    title = "Fitness Goals",
                    items = listOf(
                        "Main Goal" to userProfile.fitnessGoal.name.replace("_", " ")
                            .lowercase()
                            .replaceFirstChar { it.titlecase() },
                        "Workout Style" to userProfile.workoutType.name
                            .lowercase()
                            .replaceFirstChar { it.titlecase() }
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                ProfileSection(
                    title = "Workout Setup",
                    items = listOf(
                        "Location" to userProfile.workoutLocation.name
                            .lowercase()
                            .replaceFirstChar { it.titlecase() },
                        "Equipment" to if (userProfile.availableEquipment.isEmpty() ||
                            userProfile.availableEquipment.contains(Equipment.NONE))
                            "Bodyweight only"
                        else
                            userProfile.availableEquipment.joinToString(", ") { equipment ->
                                equipment.name.replace("_", " ")
                                    .lowercase()
                                    .replaceFirstChar { it.titlecase() }
                            },
                        "Schedule" to if (userProfile.workoutDaysPerWeek == 0)
                            "Flexible"
                        else
                            "${userProfile.workoutDaysPerWeek} days/week",
                        "Duration" to "${userProfile.workoutDuration} minutes",
                        "Preferred Time" to userProfile.preferredWorkoutTime.name.replace("_", " ")
                            .lowercase()
                            .replaceFirstChar { it.titlecase() }
                    )
                )

                if (userProfile.injuries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.large))

                    ProfileSection(
                        title = "Limitations",
                        items = listOf(
                            "Injuries/Concerns" to userProfile.injuries.joinToString(", ")
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                AIPreviewCard(userProfile = userProfile)

                Spacer(modifier = Modifier.height(Spacing.medium))
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.small)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            items.forEach { (label, value) ->
                ProfileItem(label = label, value = value)
            }
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
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
                        text = "AI Routine Preview",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.background
                    )
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    Text(
                        text = "Based on your profile, we'll create a ${
                            if (userProfile.workoutDaysPerWeek == 0) "flexible" 
                            else "${userProfile.workoutDaysPerWeek}-day"
                        } strength program focused on general fitness.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
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