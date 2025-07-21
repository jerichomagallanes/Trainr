package com.jericx.trainr.presentation

sealed class Screen(val route: String) {
    data object SplashScreen : Screen("splash_screen")
    data object Welcome : Screen("welcome_screen")
    data object BasicInfo : Screen("basic_info_screen")
    data object BodyMetrics : Screen("body_metrics_screen")
    data object FitnessGoal : Screen("fitness_goal_screen")
    data object WorkoutSetup : Screen("workout_setup_screen")
    data object Limitations : Screen("limitations_screen")
    data object Review : Screen("review_screen")
    data object Generating : Screen("generating_screen")
    data object Home : Screen("home_screen")
}
