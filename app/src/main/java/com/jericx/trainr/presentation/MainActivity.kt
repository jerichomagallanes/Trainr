package com.jericx.trainr.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.jericx.trainr.BuildConfig
import com.jericx.trainr.data.preferences.NavigationStateManager
import com.jericx.trainr.presentation.common.LocaleManager
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.onboarding.OnboardingViewModel
import com.jericx.trainr.presentation.onboarding.screens.BasicInfoScreen
import com.jericx.trainr.presentation.onboarding.screens.BodyMetricsScreen
import com.jericx.trainr.presentation.onboarding.screens.FitnessGoalScreen
import com.jericx.trainr.presentation.onboarding.screens.GeneratingScreen
import com.jericx.trainr.presentation.onboarding.screens.LimitationsScreen
import com.jericx.trainr.presentation.onboarding.screens.ReviewScreen
import com.jericx.trainr.presentation.onboarding.screens.WelcomeScreen
import com.jericx.trainr.presentation.onboarding.screens.WorkoutSetupScreen
import com.jericx.trainr.presentation.splash.SplashScreen
import com.jericx.trainr.presentation.workout.DayCompletedScreen
import com.jericx.trainr.presentation.workout.RoutineDetailRoute
import com.jericx.trainr.presentation.workout.WeeklyPlanRoute
import com.jericx.trainr.presentation.workout.WeekCompletedScreen
import com.jericx.trainr.presentation.workout.WeeklyProgressScreen
import com.jericx.trainr.presentation.workout.sample.SampleWeeklyProgress
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

private val editArguments = listOf(
    navArgument(Screen.EditableStep.ARG_EDIT) {
        type = NavType.BoolType
        defaultValue = false
    }
)

private val NavBackStackEntry.isEditing: Boolean
    get() = arguments?.getBoolean(Screen.EditableStep.ARG_EDIT) ?: false

private const val FORCED_LANGUAGE = "en"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // English-only for now, whatever the device or an old preference says:
        // strings AND locale-driven formatting (dates) stay consistent.
        LocaleManager.updateAppLocale(this, FORCED_LANGUAGE)

        enableEdgeToEdge()

        val versionName = BuildConfig.VERSION_NAME

        setContent {
            AppContent(versionName = versionName)
        }
    }
}

@Composable
fun AppContent(versionName: String) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()

    val splashScreenDuration = 2000L
    var showSplashScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (NavigationStateManager.isLanguageChangePending(context)) {
            val savedRoute = NavigationStateManager.getCurrentRoute(context)
            if (savedRoute != null && savedRoute != Screen.SplashScreen.route) {
                showSplashScreen = false
                navController.navigate(savedRoute) {
                    popUpTo(Screen.SplashScreen.route) { inclusive = true }
                }
                NavigationStateManager.clearNavigationState(context)
            }
        }
    }

    LaunchedEffect(showSplashScreen) {
        if (showSplashScreen) {
            delay(splashScreenDuration)
            // A returning user lands on their plan; onboarding is for the first
            // run. Resolved before showSplashScreen flips: that flip restarts
            // this effect, which would cancel a suspend call sitting after it.
            val destination = if (onboardingViewModel.hasCompletedOnboarding()) {
                Screen.Home.route
            } else {
                Screen.Welcome.route
            }
            showSplashScreen = false
            navController.navigate(destination) {
                popUpTo(Screen.SplashScreen.route) { inclusive = true }
            }
        }
    }

    TrainrTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navController = navController, startDestination = Screen.SplashScreen.route) {
                composable(route = Screen.SplashScreen.route) {
                    SplashScreen(versionName = versionName)
                }

                composable(Screen.Welcome.route) {
                    LaunchedEffect(Unit) {
                        NavigationStateManager.saveCurrentRoute(context, Screen.Welcome.route)
                    }
                    WelcomeScreen(
                        onGetStartedClick = {
                            NavigationStateManager.saveCurrentRoute(
                                context, Screen.BasicInfo.createRoute()
                            )
                            navController.navigate(Screen.BasicInfo.createRoute())
                        }
                    )
                }

                composable(
                    route = Screen.BasicInfo.route,
                    arguments = editArguments
                ) { entry ->
                    val editing = entry.isEditing
                    BasicInfoScreen(
                        initial = if (editing) onboardingState.userProfile else null,
                        isEditing = editing,
                        onNextClick = { firstName, age, gender, experience ->
                            onboardingViewModel.updateBasicInfo(firstName, age, gender, experience)
                            if (editing) {
                                // The review's Personal card covers both steps, so
                                // its edit walks basics then measurements before
                                // dropping back onto the review.
                                navController.navigate(
                                    Screen.BodyMetrics.createRoute(edit = true)
                                ) {
                                    popUpTo(Screen.BasicInfo.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.BodyMetrics.createRoute())
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.BodyMetrics.route,
                    arguments = editArguments
                ) { entry ->
                    val editing = entry.isEditing
                    BodyMetricsScreen(
                        initial = if (editing) onboardingState.userProfile else null,
                        isEditing = editing,
                        onNextClick = { height, weight ->
                            onboardingViewModel.updateBodyMetrics(height, weight)
                            if (editing) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.FitnessGoal.createRoute())
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.FitnessGoal.route,
                    arguments = editArguments
                ) { entry ->
                    val editing = entry.isEditing
                    FitnessGoalScreen(
                        initial = if (editing) onboardingState.userProfile else null,
                        isEditing = editing,
                        onNextClick = { goal ->
                            onboardingViewModel.updateFitnessGoal(goal)
                            if (editing) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.WorkoutSetup.createRoute())
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.WorkoutSetup.route,
                    arguments = editArguments
                ) { entry ->
                    val editing = entry.isEditing
                    WorkoutSetupScreen(
                        initial = if (editing) onboardingState.userProfile else null,
                        isEditing = editing,
                        onNextClick = { location, equipment, days, duration, time ->
                            onboardingViewModel.updateWorkoutSetup(location, equipment, days, duration, time)
                            if (editing) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.Limitations.createRoute())
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Limitations.route,
                    arguments = editArguments
                ) { entry ->
                    val editing = entry.isEditing
                    LimitationsScreen(
                        initial = if (editing) onboardingState.userProfile else null,
                        isEditing = editing,
                        onNextClick = { injuries, workoutType ->
                            onboardingViewModel.updateLimitations(injuries, workoutType)
                            if (editing) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.Review.createRoute())
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Review.route,
                    arguments = listOf(
                        navArgument(Screen.Review.ARG_FROM_PLAN) {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entry ->
                    val fromPlan =
                        entry.arguments?.getBoolean(Screen.Review.ARG_FROM_PLAN) ?: false
                    ReviewScreen(
                        userProfile = onboardingState.userProfile,
                        isRegenerating = fromPlan,
                        onConfirmClick = {
                            navController.navigate(Screen.Generating.route)
                        },
                        onBackClick = { navController.popBackStack() },
                        onEditPersonal = {
                            navController.navigate(Screen.BasicInfo.createRoute(edit = true))
                        },
                        onEditGoals = {
                            navController.navigate(Screen.FitnessGoal.createRoute(edit = true))
                        },
                        onEditSetup = {
                            navController.navigate(Screen.WorkoutSetup.createRoute(edit = true))
                        },
                        onEditLimitations = {
                            navController.navigate(Screen.Limitations.createRoute(edit = true))
                        }
                    )
                }

                composable(Screen.Generating.route) {
                    GeneratingScreen(
                        onGenerationComplete = {
                            onboardingViewModel.saveUserProfile {
                                // The new plan is a fresh start whichever door led
                                // here, so the whole back stack goes.
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.WeeklyProgress.route) {
                    WeeklyProgressScreen(
                        weeks = SampleWeeklyProgress.weeks,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.RoutineDetail.route,
                    arguments = listOf(
                        navArgument(Screen.RoutineDetail.ARG_DAY_NUMBER) { type = NavType.IntType }
                    )
                ) {
                    RoutineDetailRoute(
                        onBackClick = { navController.popBackStack() },
                        onDayCompleted = { dayNumber ->
                            navController.navigate(Screen.DayCompleted.createRoute(dayNumber)) {
                                popUpTo(Screen.RoutineDetail.route) { inclusive = true }
                            }
                        },
                        onWeekCompleted = { weekNumber ->
                            navController.navigate(Screen.WeekCompleted.createRoute(weekNumber)) {
                                popUpTo(Screen.RoutineDetail.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.DayCompleted.route,
                    arguments = listOf(
                        navArgument(Screen.DayCompleted.ARG_DAY_NUMBER) { type = NavType.IntType }
                    )
                ) { entry ->
                    DayCompletedScreen(
                        dayNumber = entry.arguments
                            ?.getInt(Screen.DayCompleted.ARG_DAY_NUMBER) ?: 1,
                        onBackClick = { navController.popBackStack() },
                        onViewProgressClick = {
                            navController.navigate(Screen.WeeklyProgress.route)
                        },
                        onBackToRoutineClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.WeekCompleted.route,
                    arguments = listOf(
                        navArgument(Screen.WeekCompleted.ARG_WEEK_NUMBER) { type = NavType.IntType }
                    )
                ) { entry ->
                    WeekCompletedScreen(
                        weekNumber = entry.arguments
                            ?.getInt(Screen.WeekCompleted.ARG_WEEK_NUMBER) ?: 1,
                        onBackClick = { navController.popBackStack() },
                        onViewProgressClick = {
                            navController.navigate(Screen.WeeklyProgress.route)
                        },
                        // There is no week-two plan yet, so this returns to the
                        // plan surface where next week will live.
                        onPreviewNextWeekClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    WeeklyPlanRoute(
                        onTrackProgressClick = {
                            navController.navigate(Screen.WeeklyProgress.route)
                        },
                        // Any day opens its routine — a finished one to look back
                        // at, a future one to read ahead or start early.
                        onDayClick = { day ->
                            navController.navigate(
                                Screen.RoutineDetail.createRoute(day.dayNumber)
                            )
                        },
                        onStartTodayClick = { day ->
                            navController.navigate(
                                Screen.RoutineDetail.createRoute(day.dayNumber)
                            )
                        },
                        // The plan stays underneath so the close button on the
                        // review is a real way back out of regenerating.
                        onLeavePlanConfirmed = {
                            navController.navigate(Screen.Review.createRoute(fromPlan = true))
                        }
                    )
                }
            }
        }
    }
}