package com.jericx.trainr.presentation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jericx.trainr.BuildConfig
import com.jericx.trainr.common.Constants
import com.jericx.trainr.presentation.home.HomeScreen
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
import com.jericx.trainr.presentation.common.LanguagePreferences
import com.jericx.trainr.presentation.common.LocaleManager
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val languagePreferences = LanguagePreferences(this)
        val savedLanguageCode = languagePreferences.currentLanguage
        if (savedLanguageCode != "en") {
            LocaleManager.updateAppLocale(this, savedLanguageCode)
        }

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
    val sharedPreferences = context.getSharedPreferences(Constants.KEY_SHARED_PREF, Context.MODE_PRIVATE)
    val isDarkModeOn = remember {
        mutableStateOf(
            sharedPreferences.getBoolean(Constants.DARK_MODE, false)
        )
    }

    DisposableEffect(Unit) {
        val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Constants.DARK_MODE) {
                isDarkModeOn.value = sharedPreferences.getBoolean(Constants.DARK_MODE, false)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }

    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.onboardingState.collectAsState()

    val splashScreenDuration = 2000L
    var showSplashScreen by remember { mutableStateOf(true) }

    LaunchedEffect(showSplashScreen) {
        if (showSplashScreen) {
            delay(splashScreenDuration)
            showSplashScreen = false
            navController.navigate(Screen.Welcome.route) {
                popUpTo(Screen.SplashScreen.route) { inclusive = true }
            }
        }
    }

    TrainrTheme(darkTheme = isDarkModeOn.value) {
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
                    WelcomeScreen(
                        onGetStartedClick = {
                            navController.navigate(Screen.BasicInfo.route)
                        }
                    )
                }

                composable(Screen.BasicInfo.route) {
                    BasicInfoScreen(
                        onNextClick = { firstName, age, gender, experience ->
                            onboardingViewModel.updateBasicInfo(firstName, age, gender, experience)
                            navController.navigate(Screen.BodyMetrics.route)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.BodyMetrics.route) {
                    BodyMetricsScreen(
                        onNextClick = { height, weight ->
                            onboardingViewModel.updateBodyMetrics(height, weight)
                            navController.navigate(Screen.FitnessGoal.route)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.FitnessGoal.route) {
                    FitnessGoalScreen(
                        onNextClick = { goal ->
                            onboardingViewModel.updateFitnessGoal(goal)
                            navController.navigate(Screen.WorkoutSetup.route)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.WorkoutSetup.route) {
                    WorkoutSetupScreen(
                        onNextClick = { location, equipment, days, duration, time ->
                            onboardingViewModel.updateWorkoutSetup(location, equipment, days, duration, time)
                            navController.navigate(Screen.Limitations.route)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Limitations.route) {
                    LimitationsScreen(
                        onNextClick = { injuries, workoutType ->
                            onboardingViewModel.updateLimitations(injuries, workoutType)
                            navController.navigate(Screen.Review.route)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Review.route) {
                    ReviewScreen(
                        userProfile = onboardingState.userProfile,
                        onConfirmClick = {
                            navController.navigate(Screen.Generating.route)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Generating.route) {
                    GeneratingScreen(
                        onGenerationComplete = {
                            onboardingViewModel.saveUserProfile {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen()
                }
            }
        }
    }
}