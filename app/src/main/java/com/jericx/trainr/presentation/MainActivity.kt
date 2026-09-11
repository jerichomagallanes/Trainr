package com.jericx.trainr.presentation

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.jericx.trainr.R
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import com.jericx.trainr.data.diagnostics.CrashlyticsBreadcrumbs
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.data.preferences.AppearanceMode
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.presentation.common.theme.DarkTrainrColors
import com.jericx.trainr.presentation.common.theme.LightTrainrColors
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.common.theme.trainrColors
import com.jericx.trainr.presentation.onboarding.OnboardingState
import com.jericx.trainr.presentation.onboarding.OnboardingStep
import com.jericx.trainr.presentation.onboarding.OnboardingViewModel
import com.jericx.trainr.presentation.onboarding.screens.BasicInfoScreen
import com.jericx.trainr.presentation.onboarding.screens.BodyMetricsScreen
import com.jericx.trainr.presentation.onboarding.screens.FitnessGoalScreen
import com.jericx.trainr.domain.purchases.ProGate
import com.jericx.trainr.presentation.onboarding.screens.GeneratingScreen
import com.jericx.trainr.presentation.purchases.PaywallReason
import com.jericx.trainr.presentation.purchases.ProPaywallRoute
import com.jericx.trainr.presentation.purchases.ProPromptSheet
import com.jericx.trainr.presentation.purchases.ProRoute
import com.jericx.trainr.presentation.onboarding.screens.LimitationsScreen
import com.jericx.trainr.presentation.onboarding.screens.ReviewScreen
import com.jericx.trainr.presentation.onboarding.screens.WelcomeScreen
import com.jericx.trainr.presentation.onboarding.screens.WorkoutSetupScreen
import com.jericx.trainr.presentation.splash.SplashScreen
import com.jericx.trainr.presentation.workout.DayCompletedScreen
import com.jericx.trainr.presentation.workout.RoutineDetailRoute
import com.jericx.trainr.presentation.workout.WeeklyPlanRoute
import com.jericx.trainr.presentation.workout.NextWeekViewModel
import com.jericx.trainr.presentation.workout.WeekCompletedScreen
import com.jericx.trainr.presentation.workout.WeeklyProgressRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject
import java.util.Locale

private val editArguments = listOf(
    navArgument(Screen.EditableStep.ARG_EDIT) {
        type = NavType.BoolType
        defaultValue = false
    }
)

// Seed a step only once answered: the profile's defaults are real values and
// would read as choices nobody made.
private fun OnboardingState.filledFor(
    step: OnboardingStep,
    editing: Boolean
): UserProfile? = if (editing || step in answeredSteps) userProfile else null

@Composable
private fun rememberBreadcrumbs(): Breadcrumbs = remember { CrashlyticsBreadcrumbs() }

private val NavBackStackEntry.isEditing: Boolean
    get() = arguments?.getBoolean(Screen.EditableStep.ARG_EDIT) ?: false

private fun AppearanceMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    AppearanceMode.SYSTEM -> systemInDarkTheme
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
}

@Composable
private fun AppearanceMode.resolvedToDark(): Boolean = isDark(isSystemInDarkTheme())

// The starting window is drawn from res/values-night before this app runs, so
// the night qualifier only follows the preference if the platform is told it.
// Below API 31 there is no such mechanism; the repaint in onCreate is the cure.
private fun Context.persistAppNightMode(appearance: AppearanceMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val uiModeManager = getSystemService(UiModeManager::class.java) ?: return
    uiModeManager.setApplicationNightMode(
        when (appearance) {
            AppearanceMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            AppearanceMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            AppearanceMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
    )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var proGate: ProGate

    // The app ships English copy only, so dates and numbers have to be English
    // too, whatever the device says. The configured context must become the
    // activity's base before any resources are read, so it cannot move to
    // onCreate.
    override fun attachBaseContext(newBase: Context) {
        Locale.setDefault(Locale.ENGLISH)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Repaint only when the preference disagrees with the qualifier the
        // starting window was drawn from: painting it either way replaces the
        // platform theme's background and shifts the navigation bar strip.
        startupOverride()?.let { window.setBackgroundDrawable(ColorDrawable(it)) }

        val versionName = BuildConfig.VERSION_NAME

        setContent {
            AppContent(
                versionName = versionName,
                themePreferences = themePreferences,
                proGate = proGate
            )
        }
    }

    private fun startupOverride(): Int? {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val qualifierIsNight = uiMode == Configuration.UI_MODE_NIGHT_YES
        val dark = themePreferences.appearance.value.isDark(qualifierIsNight)
        if (dark == qualifierIsNight) return null
        return if (dark) {
            DarkTrainrColors.surfacePage.toArgb()
        } else {
            LightTrainrColors.surfacePage.toArgb()
        }
    }
}

@Composable
fun AppContent(
    versionName: String,
    themePreferences: ThemePreferences,
    proGate: ProGate
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Route patterns only, never their filled-in arguments, so a crash report
    // carries no client data.
    val breadcrumbs = rememberBreadcrumbs()
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            entry.destination.route?.let { breadcrumbs.record("screen: ${it.substringBefore('?')}") }
        }
    }

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()

    val splashScreenDuration = 2000L
    var showSplashScreen by remember { mutableStateOf(true) }

    LaunchedEffect(showSplashScreen) {
        if (showSplashScreen) {
            delay(splashScreenDuration)
            // Resolved before showSplashScreen flips: that flip restarts this
            // effect and would cancel a suspend call sitting after it.
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

    // Asked for where the tap happened, so the limit is explained before anyone
    // is shown a price.
    var prompt by remember { mutableStateOf<PaywallReason?>(null) }

    fun askThen(reason: PaywallReason, action: () -> Unit) {
        when (proGate.decide()) {
            ProGate.Decision.ALLOWED -> action()
            ProGate.Decision.ASK -> prompt = reason
        }
    }

    val appearance by themePreferences.appearance.collectAsStateWithLifecycle()

    LaunchedEffect(appearance) { context.persistAppNightMode(appearance) }

    TrainrTheme(darkTheme = appearance.resolvedToDark()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.trainrColors.surfacePage
        ) {
            prompt?.let { reason ->
                ProPromptSheet(
                    reason = reason,
                    onContinue = {
                        prompt = null
                        navController.navigate(Screen.Paywall.createRoute(reason))
                    },
                    onDismiss = { prompt = null }
                )
            }

            NavHost(navController = navController, startDestination = Screen.SplashScreen.route) {
                composable(route = Screen.SplashScreen.route) {
                    SplashScreen(versionName = versionName)
                }

                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onGetStartedClick = {
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
                        initial = onboardingState.filledFor(OnboardingStep.BASIC_INFO, editing),
                        isEditing = editing,
                        onNextClick = { firstName, age, gender, experience ->
                            onboardingViewModel.updateBasicInfo(firstName, age, gender, experience)
                            if (editing) {
                                navController.popBackStack()
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
                        initial = onboardingState.filledFor(OnboardingStep.BODY_METRICS, editing),
                        isEditing = editing,
                        onNextClick = { height, weight, units ->
                            onboardingViewModel.updateBodyMetrics(height, weight, units)
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
                        initial = onboardingState.filledFor(OnboardingStep.GOALS, editing),
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
                        initial = onboardingState.filledFor(OnboardingStep.SETUP, editing),
                        isEditing = editing,
                        stockedEquipment = onboardingViewModel.stockedEquipment,
                        onNextClick = { equipment, liftingUnits, days, duration ->
                            onboardingViewModel.updateWorkoutSetup(
                                equipment, liftingUnits, days, duration
                            )
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
                        initial = onboardingState.filledFor(OnboardingStep.LIMITATIONS, editing),
                        isEditing = editing,
                        onNextClick = { injuries ->
                            onboardingViewModel.updateLimitations(injuries)
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
                        },
                        navArgument(Screen.Review.ARG_PROFILE_ONLY) {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entry ->
                    val fromPlan =
                        entry.arguments?.getBoolean(Screen.Review.ARG_FROM_PLAN) ?: false
                    val profileOnly =
                        entry.arguments?.getBoolean(Screen.Review.ARG_PROFILE_ONLY) ?: false
                    ReviewScreen(
                        userProfile = onboardingState.userProfile,
                        isRegenerating = fromPlan,
                        isProfileUpdate = profileOnly,
                        onConfirmClick = {
                            if (profileOnly) {
                                onboardingViewModel.updateProfileOnly {
                                    navController.popBackStack(
                                        Screen.Home.route,
                                        inclusive = false
                                    )
                                }
                            } else if (fromPlan) {
                                askThen(PaywallReason.FRESH_PLAN) { navController.navigate(Screen.Generating.route) }
                            } else {
                                navController.navigate(Screen.Generating.route)
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onEditPersonal = {
                            navController.navigate(Screen.BasicInfo.createRoute(edit = true))
                        },
                        onEditMeasurements = {
                            navController.navigate(Screen.BodyMetrics.createRoute(edit = true))
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
                        isReady = onboardingState.isCompleted,
                        onStart = { onboardingViewModel.saveUserProfile() },
                        onDone = {
                            // Spent here and nowhere earlier, so a failed
                            // generation costs nothing and the free week is
                            // still there to be used.
                            proGate.spend()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        failure = onboardingState.generationFailure,
                        builtInsteadOf = onboardingState.builtInsteadOf,
                        onRetry = { onboardingViewModel.saveUserProfile() },
                        onGiveUp = { navController.popBackStack() },
                        giveUpLabel = R.string.back_to_profile
                    )
                }

                composable(Screen.WeeklyProgress.route) {
                    WeeklyProgressRoute(
                        onBackClick = { navController.popBackStack() },
                        onWeekClick = { week ->
                            navController.navigate(Screen.WeekPlan.createRoute(week.weekNumber))
                        },
                        onLastWeekDeleted = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.WeekPlan.route,
                    arguments = listOf(
                        navArgument(Screen.WeekPlan.ARG_WEEK_NUMBER) { type = NavType.IntType }
                    )
                ) { entry ->
                    val weekNumber = entry.arguments
                        ?.getInt(Screen.WeekPlan.ARG_WEEK_NUMBER) ?: 1
                    val openDay = { day: WorkoutDay ->
                        navController.navigate(
                            Screen.RoutineDetail.createRoute(day.dayNumber, weekNumber)
                        )
                    }
                    val nextWeekViewModel: NextWeekViewModel = hiltViewModel()
                    val weekWasRepeated by nextWeekViewModel.isReady
                        .collectAsStateWithLifecycle()

                    LaunchedEffect(weekWasRepeated) {
                        if (weekWasRepeated) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    WeeklyPlanRoute(
                        onBackClick = { navController.popBackStack() },
                        onDayClick = openDay,
                        onStartTodayClick = openDay,
                        onRepeatWeekClick = {
                            askThen(PaywallReason.NEXT_WEEK) {
                                nextWeekViewModel.repeatWeek(weekNumber)
                            }
                        },
                        onRegenerateWeekClick = {
                            askThen(PaywallReason.REWRITE) { navController.navigate(Screen.RegeneratingWeek.route) }
                        }
                    )
                }

                composable(
                    route = Screen.RoutineDetail.route,
                    arguments = listOf(
                        navArgument(Screen.RoutineDetail.ARG_DAY_NUMBER) { type = NavType.IntType },
                        navArgument(Screen.RoutineDetail.ARG_WEEK_NUMBER) {
                            type = NavType.IntType
                            defaultValue = Screen.RoutineDetail.LATEST_WEEK
                        }
                    )
                ) {
                    RoutineDetailRoute(
                        onBackClick = { navController.popBackStack() },
                        // The session stays on the stack behind the
                        // congratulations, so back returns to the finished
                        // workout where a mistyped number gets corrected.
                        onDayCompleted = { dayNumber ->
                            navController.navigate(Screen.DayCompleted.createRoute(dayNumber))
                        },
                        onWeekCompleted = { weekNumber ->
                            navController.navigate(Screen.WeekCompleted.createRoute(weekNumber))
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
                        onPreviewNextWeekClick = {
                            askThen(PaywallReason.NEXT_WEEK) { navController.navigate(Screen.GeneratingNextWeek.route) }
                        }
                    )
                }

                composable(
                    route = Screen.Paywall.route,
                    arguments = listOf(
                        navArgument(Screen.Paywall.ARG_REASON) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val name = entry.arguments?.getString(Screen.Paywall.ARG_REASON)
                    ProPaywallRoute(
                        reason = PaywallReason.entries.firstOrNull { it.name == name },
                        onClose = { navController.popBackStack() }
                    )
                }

                composable(Screen.Pro.route) {
                    ProRoute(onClose = { navController.popBackStack() })
                }

                composable(Screen.RegeneratingWeek.route) {
                    val nextWeekViewModel: NextWeekViewModel = hiltViewModel()
                    val failure by nextWeekViewModel.failure.collectAsStateWithLifecycle()
                    val weekIsReady by nextWeekViewModel.isReady.collectAsStateWithLifecycle()
                    GeneratingScreen(
                        isReady = weekIsReady,
                        onStart = { nextWeekViewModel.regenerateThisWeek() },
                        onDone = {
                            // Spent here and nowhere earlier, so a failed
                            // generation costs nothing and the free week is
                            // still there to be used.
                            proGate.spend()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        failure = failure,
                        onRetry = { nextWeekViewModel.regenerateThisWeek() },
                        onGiveUp = { navController.popBackStack() }
                    )
                }

                composable(Screen.GeneratingNextWeek.route) {
                    val nextWeekViewModel: NextWeekViewModel = hiltViewModel()
                    val nextWeekFailure by nextWeekViewModel.failure.collectAsStateWithLifecycle()
                    val nextWeekBuiltInstead by nextWeekViewModel.builtInsteadOf.collectAsStateWithLifecycle()
                    val weekIsReady by nextWeekViewModel.isReady.collectAsStateWithLifecycle()
                    GeneratingScreen(
                        isReady = weekIsReady,
                        onStart = { nextWeekViewModel.generateNextWeek() },
                        onDone = {
                            // Spent here and nowhere earlier, so a failed
                            // generation costs nothing and the free week is
                            // still there to be used.
                            proGate.spend()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        failure = nextWeekFailure,
                        builtInsteadOf = nextWeekBuiltInstead,
                        onRetry = { nextWeekViewModel.generateNextWeek() },
                        onGiveUp = { navController.popBackStack() }
                    )
                }

                composable(Screen.Home.route) {
                    val nextWeekViewModel: NextWeekViewModel = hiltViewModel()
                    val weekWasRepeated by nextWeekViewModel.isReady
                        .collectAsStateWithLifecycle()

                    // Re-entering home rebuilds this entry and its view model,
                    // so the repeat cannot come round twice.
                    LaunchedEffect(weekWasRepeated) {
                        if (weekWasRepeated) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    WeeklyPlanRoute(
                        onTrackProgressClick = {
                            navController.navigate(Screen.WeeklyProgress.route)
                        },
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
                        onLeavePlanConfirmed = {
                            navController.navigate(Screen.Review.createRoute(fromPlan = true))
                        },
                        onUpdateProfileClick = {
                            navController.navigate(
                                Screen.Review.createRoute(fromPlan = true, profileOnly = true)
                            )
                        },
                        onOpenProClick = { navController.navigate(Screen.Pro.route) },
                        onStartNextWeekClick = {
                            askThen(PaywallReason.NEXT_WEEK) { navController.navigate(Screen.GeneratingNextWeek.route) }
                        },
                        onRepeatWeekClick = {
                            askThen(PaywallReason.NEXT_WEEK) { nextWeekViewModel.repeatWeek() }
                        },
                        onRegenerateWeekClick = {
                            askThen(PaywallReason.REWRITE) { navController.navigate(Screen.RegeneratingWeek.route) }
                        },
                        onCreatePlanClick = {
                            navController.navigate(Screen.Review.createRoute(fromPlan = true))
                        },
                        versionName = versionName,
                        appearance = appearance,
                        onAppearanceChange = themePreferences::setAppearance
                    )
                }
            }
        }
    }
}