package com.jericx.trainr.presentation.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.*

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

// The routine sentence needs a gerund for the goal and a bare noun for the
// style; neither display label fits that shape.
@Composable
fun FitnessGoal.getFocusPhrase(): String = when (this) {
    FitnessGoal.WEIGHT_LOSS -> stringResource(R.string.goal_focus_lose_weight)
    FitnessGoal.MUSCLE_GAIN -> stringResource(R.string.goal_focus_build_muscle)
    FitnessGoal.STRENGTH -> stringResource(R.string.goal_focus_get_stronger)
    FitnessGoal.ENDURANCE -> stringResource(R.string.goal_focus_improve_endurance)
    FitnessGoal.GENERAL_FITNESS -> stringResource(R.string.goal_focus_general_fitness)
    FitnessGoal.FLEXIBILITY -> stringResource(R.string.goal_focus_flexibility)
}

@Composable
fun Equipment.getLocalizedName(): String = when (this) {
    Equipment.NONE -> stringResource(R.string.bodyweight_only)
    Equipment.BARBELL -> stringResource(R.string.equipment_barbell)
    Equipment.DUMBBELL -> stringResource(R.string.equipment_dumbbell)
    Equipment.KETTLEBELL -> stringResource(R.string.equipment_kettlebell)
    Equipment.MACHINE -> stringResource(R.string.equipment_machine)
    Equipment.PLATE -> stringResource(R.string.equipment_plate)
    Equipment.RESISTANCE_BAND -> stringResource(R.string.equipment_resistance_band)
    Equipment.SUSPENSION_BAND -> stringResource(R.string.equipment_suspension_band)
    Equipment.OTHER -> stringResource(R.string.equipment_other)
}

@Composable
fun Injury.cautionText(): String = when (this) {
    Injury.LOWER_BACK -> stringResource(R.string.caution_lower_back)
    Injury.KNEE -> stringResource(R.string.caution_knee)
    Injury.SHOULDER -> stringResource(R.string.caution_shoulder)
    Injury.WRIST -> stringResource(R.string.caution_wrist)
    Injury.ANKLE -> stringResource(R.string.caution_ankle)
    Injury.HIP -> stringResource(R.string.caution_hip)
    Injury.NECK -> stringResource(R.string.caution_neck)
}

@Composable
fun Injury.getLocalizedName(): String = when (this) {
    Injury.LOWER_BACK -> stringResource(R.string.lower_back_pain_injury)
    Injury.KNEE -> stringResource(R.string.knee_problems_injury)
    Injury.SHOULDER -> stringResource(R.string.shoulder_injury_injury)
    Injury.WRIST -> stringResource(R.string.wrist_pain_injury)
    Injury.ANKLE -> stringResource(R.string.ankle_issues_injury)
    Injury.HIP -> stringResource(R.string.hip_problems_injury)
    Injury.NECK -> stringResource(R.string.neck_pain_injury)
}