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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.common.Constants
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutTime
import com.jericx.trainr.presentation.common.components.cards.TrainrLocationCard
import com.jericx.trainr.presentation.common.components.cards.TrainrSelectionCard
import com.jericx.trainr.presentation.common.components.core.TrainrButton
import com.jericx.trainr.presentation.common.components.core.TrainrCheckboxChip
import com.jericx.trainr.presentation.common.components.core.TrainrDropdown
import com.jericx.trainr.presentation.common.components.core.TrainrMultiSelectChip
import com.jericx.trainr.presentation.common.components.core.TrainrProgress
import com.jericx.trainr.presentation.common.components.core.TrainrRadioChip
import com.jericx.trainr.presentation.common.components.core.TrainrToggleChip
import com.jericx.trainr.presentation.common.components.layout.TrainrChipGroup
import com.jericx.trainr.presentation.common.components.layout.TrainrFlowRow
import com.jericx.trainr.presentation.common.components.layout.TrainrFormSection
import com.jericx.trainr.presentation.common.components.layout.TrainrScaffold
import com.jericx.trainr.presentation.common.components.layout.TrainrScreenContent
import com.jericx.trainr.presentation.common.components.layout.TrainrTopBar
import com.jericx.trainr.presentation.common.components.typography.TrainrScreenTitle
import com.jericx.trainr.presentation.common.components.typography.TrainrSectionTitle
import com.jericx.trainr.presentation.common.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutSetupScreen(
    onNextClick: (
        location: WorkoutLocation,
        equipment: List<Equipment>,
        daysPerWeek: Int,
        duration: Int,
        preferredTime: WorkoutTime
    ) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf<WorkoutLocation?>(null) }
    var selectedEquipment by remember { mutableStateOf<Set<Equipment>>(emptySet()) }
    var selectedDays by remember { mutableIntStateOf(Constants.Workout.DEFAULT_WORKOUT_DAYS_PER_WEEK) }
    var selectedDuration by remember { mutableIntStateOf(Constants.Workout.DEFAULT_WORKOUT_DURATION) }
    var selectedTime by remember { mutableStateOf(WorkoutTime.MORNING) }

    TrainrScaffold(
        onBackClick = onBackClick,
        topBar = {
            TrainrTopBar(onBackClick = onBackClick, showLogo = true)
        },
        bottomButton = {
            TrainrButton(
                text = stringResource(R.string.next),
                onClick = {
                    selectedLocation?.let { location ->
                        val equipment = if (selectedEquipment.isEmpty()) {
                            listOf(Equipment.NONE)
                        } else {
                            selectedEquipment.toList()
                        }
                        onNextClick(location, equipment, selectedDays, selectedDuration, selectedTime)
                    }
                },
                enabled = selectedLocation != null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TrainrProgress(
                currentStep = 4,
                totalSteps = 7,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )

            TrainrScreenContent {
                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrScreenTitle(text = stringResource(R.string.set_up_your_workout))

                Spacer(modifier = Modifier.height(Spacing.extraLarge))

                TrainrSectionTitle(stringResource(R.string.where_will_you_work_out))

                Spacer(modifier = Modifier.height(Spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    TrainrLocationCard(
                        text = stringResource(R.string.home),
                        iconRes = R.drawable.ic_house,
                        isSelected = selectedLocation == WorkoutLocation.HOME,
                        onClick = {
                            selectedLocation = WorkoutLocation.HOME
                            selectedEquipment = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TrainrLocationCard(
                        text = stringResource(R.string.gym),
                        iconRes = R.drawable.ic_fitness_center,
                        isSelected = selectedLocation == WorkoutLocation.GYM,
                        onClick = {
                            selectedLocation = WorkoutLocation.GYM
                            selectedEquipment = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TrainrLocationCard(
                        text = stringResource(R.string.both),
                        iconRes = R.drawable.ic_sync_alt,
                        isSelected = selectedLocation == WorkoutLocation.BOTH,
                        onClick = {
                            selectedLocation = WorkoutLocation.BOTH
                            selectedEquipment = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedLocation != null) {
                    Spacer(modifier = Modifier.height(Spacing.large))

                    TrainrFormSection(title = stringResource(R.string.available_equipment)) {
                        val equipmentOptions = when (selectedLocation) {
                            WorkoutLocation.HOME -> listOf(
                                Equipment.NONE to stringResource(R.string.bodyweight_only),
                                Equipment.DUMBBELLS to stringResource(R.string.dumbbells),
                                Equipment.RESISTANCE_BANDS to stringResource(R.string.resistance_bands),
                                Equipment.PULL_UP_BAR to stringResource(R.string.pull_up_bar),
                                Equipment.KETTLEBELLS to stringResource(R.string.kettlebells)
                            )
                            WorkoutLocation.GYM, WorkoutLocation.BOTH -> listOf(
                                Equipment.BARBELL to stringResource(R.string.barbell_plates),
                                Equipment.BENCH to stringResource(R.string.bench),
                                Equipment.CARDIO_MACHINES to stringResource(R.string.cardio_equipment),
                                Equipment.CABLE_MACHINE to stringResource(R.string.cable_machine),
                                Equipment.DUMBBELLS to stringResource(R.string.dumbbells),
                                Equipment.SQUAT_RACK to stringResource(R.string.squat_rack),
                                Equipment.OTHERS to stringResource(R.string.others)
                            )
                            else -> emptyList()
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            equipmentOptions.forEach { (equipment, label) ->
                                TrainrToggleChip(
                                    text = label,
                                    selected = selectedEquipment.contains(equipment),
                                    onClick = {
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
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                TrainrFormSection(title = stringResource(R.string.workout_days_per_week)) {
                    TrainrDropdown(
                        selectedValue = selectedDays.toString(),
                        options = Constants.Workout.DAYS_PER_WEEK_OPTIONS.map { days ->
                            stringResource(
                                R.string.workout_days_option,
                                days,
                                if (days == 1) stringResource(R.string.day) else stringResource(R.string.days)
                            )
                        },
                        onSelectionChange = { selectedOption ->
                            selectedDays = selectedOption.split(" ").firstOrNull()?.toIntOrNull() ?: Constants.Workout.DEFAULT_WORKOUT_DAYS_PER_WEEK
                        }
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                TrainrFormSection(title = stringResource(R.string.session_duration)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Constants.Workout.DURATION_OPTIONS.forEach { duration ->
                            TrainrToggleChip(
                                text = pluralStringResource(R.plurals.minutes, duration, duration),
                                selected = selectedDuration == duration,
                                onClick = { selectedDuration = duration }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                TrainrFormSection(title = stringResource(R.string.preferred_workout_time)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        TrainrRadioChip(
                            text = stringResource(R.string.early_morning_time),
                            selected = selectedTime == WorkoutTime.EARLY_MORNING,
                            onClick = { selectedTime = WorkoutTime.EARLY_MORNING }
                        )
                        TrainrRadioChip(
                            text = stringResource(R.string.morning_time),
                            selected = selectedTime == WorkoutTime.MORNING,
                            onClick = { selectedTime = WorkoutTime.MORNING }
                        )
                        TrainrRadioChip(
                            text = stringResource(R.string.afternoon_time),
                            selected = selectedTime == WorkoutTime.AFTERNOON,
                            onClick = { selectedTime = WorkoutTime.AFTERNOON }
                        )
                        TrainrRadioChip(
                            text = stringResource(R.string.evening_time),
                            selected = selectedTime == WorkoutTime.EVENING,
                            onClick = { selectedTime = WorkoutTime.EVENING }
                        )
                        TrainrRadioChip(
                            text = stringResource(R.string.flexible_anytime),
                            selected = selectedTime == WorkoutTime.ANYTIME,
                            onClick = { selectedTime = WorkoutTime.ANYTIME }
                        )
                    }
                }
            }
        }
    }
}