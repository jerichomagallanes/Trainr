package com.jericx.trainr.presentation.unstuck.feedback

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.unstuck.AdjustPainScreen

const val HOW_TO_REQUEST = "howTo"

fun NavGraphBuilder.feedbackGraph(navController: NavHostController) {
    navigation(
        route = Screen.Feedback.route,
        startDestination = Screen.AdjustmentFeedback.route,
        arguments = listOf(
            navArgument(Screen.Feedback.ARG_ADJUSTMENT_ID) { type = NavType.LongType }
        )
    ) {
        composable(Screen.AdjustmentFeedback.route) { entry ->
            val viewModel = navController.feedbackViewModel(entry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AnswerNavigation(navController, viewModel)

            // Which question this is depends on the stored proposal, so nothing
            // is asked until it has been read.
            if (state.isLoaded) {
                AdjustmentFeedbackScreen(
                    state = state,
                    onAnswer = viewModel::answer,
                    onNotQuite = { navController.navigate(Screen.FeedbackDetail.route) },
                    onDiscomfort = { viewModel.answer(FeedbackAnswer.DISCOMFORT) },
                    onNotNow = viewModel::dismiss,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.FeedbackDetail.route) { entry ->
            val viewModel = navController.feedbackViewModel(entry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AnswerNavigation(
                navController,
                viewModel,
                debriefDayNumber = state.dayNumber,
                debriefWeekNumber = state.weekNumber
            )

            FeedbackDetailScreen(
                onAnswer = viewModel::answer,
                // Nothing is written, so the offer stays pending on the screen
                // that made it rather than nagging from somewhere else.
                onSaveForLater = { navController.closeFeedback() },
                onBack = { navController.popBackStack() }
            )
        }

        // The answer is recorded, so every way out of these two leads on rather
        // than back to a question that has already been answered.
        composable(Screen.FeedbackOutcome.route) { entry ->
            val viewModel = navController.feedbackViewModel(entry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            BackHandler { navController.leaveFeedback() }

            if (state.isLoaded) {
                FeedbackOutcomeScreen(
                    state = state,
                    onDone = { navController.leaveFeedback() },
                    onOpenGuidance = { state.guidanceKey?.let { navController.openGuidance(it) } },
                    onBack = { navController.leaveFeedback() }
                )
            }
        }

        composable(Screen.FeedbackPain.route) {
            BackHandler { navController.leaveFeedback() }

            AdjustPainScreen(
                canFinishEarly = false,
                onReturn = { navController.leaveFeedback() },
                onBack = { navController.leaveFeedback() }
            )
        }
    }
}

@Composable
private fun AnswerNavigation(
    navController: NavHostController,
    viewModel: AdjustmentFeedbackViewModel,
    debriefDayNumber: Int = AdjustmentFeedbackUiState.NO_DAY,
    debriefWeekNumber: Int = Screen.RoutineDetail.LATEST_WEEK
) {
    LaunchedEffect(viewModel, debriefDayNumber, debriefWeekNumber) {
        viewModel.savedEvents.collect { answer ->
            when {
                answer == null -> navController.leaveFeedback()

                answer == FeedbackAnswer.DISCOMFORT ->
                    navController.navigate(Screen.FeedbackPain.route)

                // The answer is already recorded, so their own words take the
                // place of the fixed outcome line rather than following it.
                answer == FeedbackAnswer.SOMETHING_ELSE &&
                    debriefDayNumber != AdjustmentFeedbackUiState.NO_DAY ->
                    navController.navigate(
                        Screen.Debrief.createRoute(debriefDayNumber, debriefWeekNumber)
                    ) {
                        popUpTo(Screen.Feedback.route) { inclusive = true }
                    }

                else -> navController.navigate(Screen.FeedbackOutcome.route)
            }
        }
    }
}

@Composable
private fun NavHostController.feedbackViewModel(
    entry: NavBackStackEntry
): AdjustmentFeedbackViewModel {
    val graph = remember(entry) { getBackStackEntry(Screen.Feedback.route) }
    return hiltViewModel(graph)
}

private fun NavHostController.leaveFeedback() {
    navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
}

private fun NavHostController.closeFeedback() {
    popBackStack(Screen.Feedback.route, inclusive = true)
}

private fun NavHostController.openGuidance(exerciseKey: String) {
    getBackStackEntry(Screen.RoutineDetail.route).savedStateHandle[HOW_TO_REQUEST] = exerciseKey
    popBackStack(Screen.RoutineDetail.route, inclusive = false)
}
