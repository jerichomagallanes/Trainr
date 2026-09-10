package com.jericx.trainr.presentation.onboarding.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.common.Constants
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.EquipmentChoices
import com.jericx.trainr.domain.model.equipmentFor
import com.jericx.trainr.domain.model.LoadedEquipment
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.presentation.common.getLocalizedName
import com.jericx.trainr.presentation.common.components.cards.TrainrLocationCard
import com.jericx.trainr.presentation.common.components.cards.TrainrSelectionCard
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrCheckboxChip
import com.jericx.trainr.presentation.common.components.core.TrainrDropdown
import com.jericx.trainr.presentation.common.components.core.TrainrMultiSelectChip
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.core.TrainrToggleChip
import com.jericx.trainr.presentation.common.components.layout.TrainrChipGroup
import com.jericx.trainr.presentation.common.components.layout.TrainrFlowRow
import com.jericx.trainr.presentation.common.components.layout.TrainrFormSection
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.theme.ComponentHeight
import com.jericx.trainr.presentation.common.theme.Spacing


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutSetupScreen(
    initial: UserProfile? = null,
    isEditing: Boolean = false,
    stockedEquipment: Set<Equipment> = EquipmentChoices.toSet(),
    onNextClick: (
        equipment: List<Equipment>,
        liftingUnits: UnitSystem?,
        daysPerWeek: Int,
        duration: Int
    ) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedEquipment by remember {
        mutableStateOf(initial?.availableEquipment?.toSet() ?: emptySet())
    }
    // Nothing starts chosen: a default the client walks past would become an
    // answer the plan is built around.
    var selectedDays by remember { mutableStateOf(initial?.workoutDaysPerWeek?.takeIf { it > 0 }) }
    var selectedDuration by remember { mutableStateOf(initial?.workoutDuration?.takeIf { it > 0 }) }
    var selectedLiftingUnits by remember { mutableStateOf(initial?.liftingUnitSystem) }

    // A bodyweight setup has no plates to read, so lifting units stay unasked
    // and null rather than taking a value.
    val hasLoadedEquipment = selectedEquipment.any { it in LoadedEquipment }

    TrainrScaffold(
        onBackClick = onBackClick,
        topBar = {
            TrainrTopBar(
                onBackClick = onBackClick,
                showLogo = true,
                closeInsteadOfBack = isEditing
            )
        },
        bottomButton = {
            TrainrButton(
                text = stringResource(if (isEditing) R.string.save else R.string.next),
                onClick = {
                    val days = selectedDays
                    val duration = selectedDuration
                    if (days != null && duration != null) {
                        onNextClick(
                            selectedEquipment.toList(),
                            if (hasLoadedEquipment) selectedLiftingUnits else null,
                            days,
                            duration
                        )
                    }
                },
                // An empty equipment set means unanswered, not "nothing
                // available": "bodyweight only" is itself one of the choices.
                enabled = selectedEquipment.isNotEmpty() &&
                    (!hasLoadedEquipment || selectedLiftingUnits != null) &&
                    selectedDays != null &&
                    selectedDuration != null
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
                    currentStep = 4,
                    totalSteps = 7,
                    modifier = Modifier.padding(horizontal = Spacing.large)
                )
            }

            TrainrScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrScreenTitle(text = stringResource(R.string.set_up_your_workout))

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrFormSection(
                    title = stringResource(R.string.available_equipment),
                    verticalPadding = 0.dp,
                    titleGap = Spacing.card
                ) {
                    val equipmentOptions = equipmentFor(stockedEquipment)
                        .map { it to it.getLocalizedName() }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                        verticalArrangement = Arrangement.spacedBy(Spacing.card)
                    ) {
                        equipmentOptions.forEach { (equipment, label) ->
                            TrainrToggleChip(
                                text = label,
                                selected = selectedEquipment.contains(equipment),
                                onClick = {
                                    // "No equipment" is an answer, not an
                                    // absence: it cannot share the row with kit.
                                    selectedEquipment = if (equipment == Equipment.NONE) {
                                        if (selectedEquipment.contains(equipment)) {
                                            emptySet()
                                        } else {
                                            setOf(Equipment.NONE)
                                        }
                                    } else {
                                        val newSet = selectedEquipment - Equipment.NONE
                                        if (newSet.contains(equipment)) {
                                            newSet - equipment
                                        } else {
                                            newSet + equipment
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (hasLoadedEquipment) {
                    Spacer(modifier = Modifier.height(Spacing.sectionGap))

                    TrainrFormSection(
                        title = stringResource(R.string.weights_marked_in),
                        verticalPadding = 0.dp,
                        titleGap = Spacing.card
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.card)
                        ) {
                            TrainrToggleChip(
                                text = stringResource(R.string.weight_column),
                                selected = selectedLiftingUnits == UnitSystem.METRIC,
                                onClick = { selectedLiftingUnits = UnitSystem.METRIC },
                                height = ComponentHeight.ChipTall,
                                horizontalPadding = 0.dp,
                                modifier = Modifier.weight(1f)
                            )
                            TrainrToggleChip(
                                text = stringResource(R.string.weight_column_lbs),
                                selected = selectedLiftingUnits == UnitSystem.IMPERIAL,
                                onClick = { selectedLiftingUnits = UnitSystem.IMPERIAL },
                                height = ComponentHeight.ChipTall,
                                horizontalPadding = 0.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sectionGap))

                TrainrFormSection(
                    title = stringResource(R.string.workout_days_per_week),
                    verticalPadding = 0.dp,
                    titleGap = Spacing.card
                ) {
                    // Matched by position, never by parsing the number back out
                    // of the label: another language need not carry a digit.
                    val dayOptions = Constants.Workout.DAYS_PER_WEEK_OPTIONS
                    val dayLabels = dayOptions.map {
                        pluralStringResource(R.plurals.workout_days_option, it, it)
                    }
                    TrainrDropdown(
                        selectedValue = selectedDays
                            ?.let { dayLabels.getOrNull(dayOptions.indexOf(it)) }
                            .orEmpty(),
                        options = dayLabels,
                        onSelectionChange = { selectedOption ->
                            selectedDays = dayOptions.getOrNull(dayLabels.indexOf(selectedOption))
                        },
                        placeholder = stringResource(R.string.select_days_placeholder)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sectionGap))

                TrainrFormSection(
                    title = stringResource(R.string.session_duration),
                    verticalPadding = 0.dp,
                    titleGap = Spacing.card
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.card)
                    ) {
                        Constants.Workout.DURATION_OPTIONS.forEach { duration ->
                            TrainrToggleChip(
                                text = pluralStringResource(R.plurals.minutes, duration, duration),
                                selected = selectedDuration == duration,
                                onClick = { selectedDuration = duration },
                                height = ComponentHeight.ChipTall,
                                // Equal shares, no padding: four fixed-width
                                // chips overflow narrow phones and ellipsise.
                                horizontalPadding = 0.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}