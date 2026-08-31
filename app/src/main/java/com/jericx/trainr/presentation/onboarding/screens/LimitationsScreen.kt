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
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.common.theme.Spacing
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrCheckboxChip
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSubtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitationsScreen(
    initial: UserProfile? = null,
    isEditing: Boolean = false,
    onNextClick: (injuries: List<String>) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedInjuries by remember {
        mutableStateOf(initial?.injuries?.toSet() ?: emptySet())
    }
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

    TrainrScaffold(
        onBackClick = onBackClick,
        closeInsteadOfBack = isEditing,
        bottomButton = {
            TrainrButton(
                text = stringResource(if (isEditing) R.string.save else R.string.submit),
                onClick = {
                    val injuries = selectedInjuries.filter { it != noneLabel }.toList()
                    onNextClick(injuries)
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
            if (!isEditing) {
                TrainrProgress(
                    currentStep = 5,
                    totalSteps = 7,
                    modifier = Modifier.padding(horizontal = Spacing.large)
                )
            }

            TrainrScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrScreenTitle(text = stringResource(R.string.lets_keep_you_safe))

                Spacer(modifier = Modifier.height(Spacing.small))

                TrainrSubtitle(
                    text = stringResource(R.string.limitations_description)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                // Everything else in onboarding is needed, so the exception is the
                // thing worth marking. Asterisking eleven fields to flag one
                // would be noise.
                TrainrSectionTitle(
                    stringResource(
                        R.string.optional_label,
                        stringResource(R.string.any_injuries_or_areas)
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.card),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    injuryOptions.forEach { injury ->
                        val isNone = injury == noneOption

                        TrainrCheckboxChip(
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
            }
        }
    }
}