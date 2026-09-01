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
import com.jericx.trainr.common.Constants
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrFieldError
import com.jericx.trainr.presentation.common.components.core.touchedOnBlur
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.cards.TrainrSelectionCard
import com.jericx.trainr.presentation.common.components.core.TrainrRadioChip
import com.jericx.trainr.presentation.common.components.core.TrainrTextField
import com.jericx.trainr.presentation.common.components.layout.TrainrFormSection
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    initial: UserProfile? = null,
    isEditing: Boolean = false,
    onNextClick: (firstName: String, age: Int, gender: Gender, experience: ExperienceLevel) -> Unit,
    onBackClick: () -> Unit
) {
    var firstName by remember { mutableStateOf(initial?.firstName.orEmpty()) }
    var age by remember { mutableStateOf(initial?.age?.takeIf { it > 0 }?.toString().orEmpty()) }
    var selectedGender by remember { mutableStateOf(initial?.gender) }
    var selectedExperience by remember { mutableStateOf(initial?.experienceLevel) }

    // Required is only reported once a field has been left, never while it is
    // still being filled in.
    var nameTouched by remember { mutableStateOf(false) }
    var ageTouched by remember { mutableStateOf(false) }

    val ageRange = Constants.Workout.MIN_AGE..Constants.Workout.MAX_AGE
    val ageIsUsable = (age.toIntOrNull() ?: 0) in ageRange

    val isFormValid = firstName.isNotBlank() &&
            ageIsUsable &&
            selectedGender != null &&
            selectedExperience != null

    TrainrScaffold(
        onBackClick = onBackClick,
        closeInsteadOfBack = isEditing,
        bottomButton = {
            TrainrButton(
                text = stringResource(if (isEditing) R.string.save else R.string.next),
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
            // The bar counts the way through first-time setup. Coming back
            // to change one answer is not a seventh of anything, so it says
            // nothing then.
            if (!isEditing) {
                TrainrProgress(
                    currentStep = 1,
                    totalSteps = 7,
                    modifier = Modifier.padding(
                        horizontal = Spacing.large,
                        vertical = Spacing.medium
                    )
                )
            }

            TrainrScreenContent {
                TrainrScreenTitle(text = stringResource(R.string.tell_us_about_yourself))

                Spacer(modifier = Modifier.height(Spacing.large))

                TrainrFormSection(title = stringResource(R.string.preferred_first_name)) {
                    TrainrTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = stringResource(R.string.enter_your_first_name),
                        modifier = Modifier.touchedOnBlur { nameTouched = true }
                    )

                    // Nothing beyond "there is something here". A name is
                    // whatever its owner says it is, and rules about their
                    // shape lock real people out of the product.
                    TrainrFieldError(
                        message = stringResource(R.string.error_enter_name)
                            .takeIf { firstName.isBlank() && nameTouched }
                    )
                }

                TrainrFormSection(title = stringResource(R.string.age)) {
                    TrainrTextField(
                        value = age,
                        onValueChange = { newAge ->
                            if (newAge.all { char -> char.isDigit() } && newAge.length <= 3) {
                                age = newAge
                            }
                        },
                        placeholder = stringResource(R.string.enter_your_age),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.touchedOnBlur { ageTouched = true }
                    )

                    // States the bound rather than showing a specimen age. An
                    // example in an error reads as the answer that was wanted.
                    TrainrFieldError(
                        message = when {
                            age.isBlank() && ageTouched ->
                                stringResource(R.string.error_enter_age)
                            age.isNotBlank() && !ageIsUsable -> stringResource(
                                R.string.value_range_hint,
                                stringResource(R.string.age_label),
                                ageRange.first.toString(),
                                ageRange.last.toString()
                            )
                            else -> null
                        }
                    )
                }

                TrainrFormSection(title = stringResource(R.string.gender)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.card)
                    ) {
                        TrainrRadioChip(
                            text = stringResource(R.string.male),
                            selected = selectedGender == Gender.MALE,
                            onClick = { selectedGender = Gender.MALE },
                            modifier = Modifier.weight(1f),
                            height = ComponentHeight.Field,
                            mutedWhenUnselected = true
                        )
                        TrainrRadioChip(
                            text = stringResource(R.string.female),
                            selected = selectedGender == Gender.FEMALE,
                            onClick = { selectedGender = Gender.FEMALE },
                            modifier = Modifier.weight(1f),
                            height = ComponentHeight.Field,
                            mutedWhenUnselected = true
                        )
                        TrainrRadioChip(
                            text = stringResource(R.string.other),
                            selected = selectedGender == Gender.NON_BINARY,
                            onClick = { selectedGender = Gender.NON_BINARY },
                            modifier = Modifier.weight(1f),
                            height = ComponentHeight.Field,
                            mutedWhenUnselected = true
                        )
                    }
                }

                TrainrFormSection(title = stringResource(R.string.fitness_experience)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.card)
                    ) {
                        TrainrSelectionCard(
                            title = stringResource(R.string.beginner),
                            description = stringResource(R.string.beginner_description),
                            isSelected = selectedExperience == ExperienceLevel.BEGINNER,
                            onClick = { selectedExperience = ExperienceLevel.BEGINNER }
                        )

                        TrainrSelectionCard(
                            title = stringResource(R.string.intermediate),
                            description = stringResource(R.string.intermediate_description),
                            isSelected = selectedExperience == ExperienceLevel.INTERMEDIATE,
                            onClick = { selectedExperience = ExperienceLevel.INTERMEDIATE }
                        )

                        TrainrSelectionCard(
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