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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingButton
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingProgress
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScaffold
import com.jericx.trainr.presentation.onboarding.components.cards.OnboardingSelectionCard
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingRadioChip
import com.jericx.trainr.presentation.onboarding.components.core.OnboardingTextField
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingFormSection
import com.jericx.trainr.presentation.onboarding.components.layout.OnboardingScreenContent
import com.jericx.trainr.presentation.onboarding.components.typography.OnboardingScreenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    onNextClick: (firstName: String, age: Int, gender: Gender, experience: ExperienceLevel) -> Unit,
    onBackClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf<Gender?>(null) }
    var selectedExperience by remember { mutableStateOf<ExperienceLevel?>(null) }

    val isFormValid = firstName.isNotBlank() &&
            age.isNotBlank() &&
            (age.toIntOrNull() ?: 0) in 13..100 &&
            selectedGender != null &&
            selectedExperience != null

    OnboardingScaffold(
        onBackClick = onBackClick,
        bottomButton = {
            OnboardingButton(
                text = stringResource(R.string.next),
                onClick = {
                    val ageInt = age.toIntOrNull() ?: 0
                    selectedGender?.let { gender ->
                        selectedExperience?.let { experience ->
                            onNextClick(firstName, ageInt, gender, experience)
                        }
                    }
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
                currentStep = 1,
                totalSteps = 7,
                modifier = Modifier.padding(
                    horizontal = Spacing.large,
                    vertical = Spacing.medium
                )
            )

            OnboardingScreenContent {
                OnboardingScreenTitle(text = stringResource(R.string.tell_us_about_yourself))

                Spacer(modifier = Modifier.height(Spacing.large))

                OnboardingFormSection(title = stringResource(R.string.preferred_first_name)) {
                    OnboardingTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = stringResource(R.string.enter_your_first_name)
                    )
                }

                OnboardingFormSection(title = stringResource(R.string.age)) {
                    OnboardingTextField(
                        value = age,
                        onValueChange = { newAge ->
                            if (newAge.all { char -> char.isDigit() } && newAge.length <= 3) {
                                age = newAge
                            }
                        },
                        placeholder = stringResource(R.string.enter_your_age),
                        keyboardType = KeyboardType.Number
                    )
                }

                OnboardingFormSection(title = stringResource(R.string.gender)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        OnboardingRadioChip(
                            text = stringResource(R.string.male),
                            selected = selectedGender == Gender.MALE,
                            onClick = { selectedGender = Gender.MALE },
                            modifier = Modifier.weight(1f)
                        )
                        OnboardingRadioChip(
                            text = stringResource(R.string.female),
                            selected = selectedGender == Gender.FEMALE,
                            onClick = { selectedGender = Gender.FEMALE },
                            modifier = Modifier.weight(1f)
                        )
                        OnboardingRadioChip(
                            text = stringResource(R.string.other),
                            selected = selectedGender == Gender.NON_BINARY,
                            onClick = { selectedGender = Gender.NON_BINARY },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OnboardingFormSection(title = stringResource(R.string.fitness_experience)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        OnboardingSelectionCard(
                            title = stringResource(R.string.beginner),
                            description = stringResource(R.string.beginner_description),
                            isSelected = selectedExperience == ExperienceLevel.BEGINNER,
                            onClick = { selectedExperience = ExperienceLevel.BEGINNER }
                        )

                        OnboardingSelectionCard(
                            title = stringResource(R.string.intermediate),
                            description = stringResource(R.string.intermediate_description),
                            isSelected = selectedExperience == ExperienceLevel.INTERMEDIATE,
                            onClick = { selectedExperience = ExperienceLevel.INTERMEDIATE }
                        )

                        OnboardingSelectionCard(
                            title = stringResource(R.string.advanced),
                            description = stringResource(R.string.advanced_description),
                            isSelected = selectedExperience == ExperienceLevel.ADVANCED,
                            onClick = { selectedExperience = ExperienceLevel.ADVANCED }
                        )
                    }
                }
            }
        }
    }
}