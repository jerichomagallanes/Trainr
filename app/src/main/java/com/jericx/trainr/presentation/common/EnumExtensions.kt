package com.jericx.trainr.presentation.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.*

/**
 * Extension functions to get localized strings for enums
 */

@Composable
fun Gender.getLocalizedName(): String = when (this) {
    Gender.MALE -> stringResource(R.string.male_gender)
    Gender.FEMALE -> stringResource(R.string.female_gender)
    Gender.NON_BINARY -> stringResource(R.string.other_gender)
    Gender.PREFER_NOT_TO_SAY -> stringResource(R.string.other_gender)
}

@Composable
fun ExperienceLevel.getLocalizedName(): String = when (this) {
    ExperienceLevel.BEGINNER -> stringResource(R.string.beginner_level)
    ExperienceLevel.INTERMEDIATE -> stringResource(R.string.intermediate_level)
    ExperienceLevel.ADVANCED -> stringResource(R.string.advanced_level)
}

@Composable
fun FitnessGoal.getLocalizedName(): String = when (this) {
    FitnessGoal.WEIGHT_LOSS -> stringResource(R.string.lose_weight_goal)
    FitnessGoal.MUSCLE_GAIN -> stringResource(R.string.build_muscle_goal)
    FitnessGoal.STRENGTH -> stringResource(R.string.get_stronger_goal)
    FitnessGoal.ENDURANCE -> stringResource(R.string.improve_endurance_goal)
    FitnessGoal.GENERAL_FITNESS -> stringResource(R.string.general_fitness_goal)
    FitnessGoal.FLEXIBILITY -> stringResource(R.string.flexibility_mobility_goal)
}

@Composable
fun WorkoutLocation.getLocalizedName(): String = when (this) {
    WorkoutLocation.HOME -> stringResource(R.string.home_location)
    WorkoutLocation.GYM -> stringResource(R.string.gym_location)
    WorkoutLocation.BOTH -> stringResource(R.string.both_location)
}

@Composable
fun Equipment.getLocalizedName(): String = when (this) {
    Equipment.NONE -> stringResource(R.string.bodyweight_only)
    Equipment.DUMBBELLS -> stringResource(R.string.dumbbells)
    Equipment.BARBELL -> stringResource(R.string.barbell_plates)
    Equipment.BENCH -> stringResource(R.string.bench)
    Equipment.RESISTANCE_BANDS -> stringResource(R.string.resistance_bands)
    Equipment.PULL_UP_BAR -> stringResource(R.string.pull_up_bar)
    Equipment.KETTLEBELLS -> stringResource(R.string.kettlebells)
    Equipment.SQUAT_RACK -> stringResource(R.string.squat_rack)
    Equipment.CABLE_MACHINE -> stringResource(R.string.cable_machine)
    Equipment.CARDIO_MACHINES -> stringResource(R.string.cardio_equipment)
    Equipment.OTHERS -> stringResource(R.string.others)
}

@Composable
fun WorkoutType.getLocalizedName(): String = when (this) {
    WorkoutType.STRENGTH -> stringResource(R.string.strength_training_style)
    WorkoutType.CARDIO -> stringResource(R.string.cardio_style)
    WorkoutType.HIIT -> stringResource(R.string.hiit_style)
    WorkoutType.YOGA -> stringResource(R.string.flexibility_mobility_style)
    WorkoutType.MIXED -> stringResource(R.string.mixed_balanced_style)
}

@Composable
fun WorkoutTime.getLocalizedName(): String = when (this) {
    WorkoutTime.EARLY_MORNING -> stringResource(R.string.early_morning)
    WorkoutTime.MORNING -> stringResource(R.string.morning)
    WorkoutTime.AFTERNOON -> stringResource(R.string.afternoon)
    WorkoutTime.EVENING -> stringResource(R.string.evening)
    WorkoutTime.ANYTIME -> stringResource(R.string.flexible_anytime_time)
}