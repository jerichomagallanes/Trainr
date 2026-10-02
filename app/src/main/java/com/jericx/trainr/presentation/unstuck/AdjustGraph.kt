package com.jericx.trainr.presentation.unstuck

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
import com.jericx.trainr.domain.purchases.AdjustmentGate
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.domain.unstuck.intent.UnstuckRoute
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.purchases.PaywallReason

const val FINISH_EARLY_REQUEST = "finishEarly"

fun NavGraphBuilder.adjustGraph(
    navController: NavHostController,
    adjustmentGate: AdjustmentGate,
    onAskForPro: (PaywallReason) -> Unit
) {
    navigation(
        route = Screen.Adjust.route,
        startDestination = Screen.AdjustEntry.route,
        arguments = listOf(
            navArgument(Screen.Adjust.ARG_DAY_NUMBER) { type = NavType.IntType },
            navArgument(Screen.Adjust.ARG_WEEK_NUMBER) {
                type = NavType.IntType
                defaultValue = Screen.RoutineDetail.LATEST_WEEK
            },
            navArgument(Screen.Adjust.ARG_REASON) {
                type = NavType.StringType
                defaultValue = DirectReason.OTHER.name
            },
            navArgument(Screen.Adjust.ARG_EXERCISE_ID) {
                type = NavType.LongType
                defaultValue = Screen.Adjust.NO_EXERCISE
            }
        )
    ) {
        composable(Screen.AdjustEntry.route) { entry ->
            val graph = navController.adjustEntry(entry)
            when (graph.arguments.reason()) {
                DirectReason.LESS_TIME -> TimeStep(navController, graph, adjustmentGate, onAskForPro)
                DirectReason.EQUIPMENT ->
                    EquipmentStep(navController, graph, adjustmentGate, onAskForPro)

                DirectReason.PAIN -> PainStep(navController, graph)
                else -> ContextStep(navController, graph)
            }
        }

        composable(Screen.AdjustTime.route) { entry ->
            TimeStep(
                navController,
                navController.adjustEntry(entry),
                adjustmentGate,
                onAskForPro
            )
        }

        composable(Screen.AdjustEquipment.route) { entry ->
            EquipmentStep(
                navController,
                navController.adjustEntry(entry),
                adjustmentGate,
                onAskForPro
            )
        }

        composable(Screen.AdjustContext.route) { entry ->
            ContextStep(navController, navController.adjustEntry(entry))
        }

        composable(Screen.AdjustPain.route) { entry ->
            PainStep(navController, navController.adjustEntry(entry))
        }

        composable(Screen.AdjustReview.route) { entry ->
            val graph = navController.adjustEntry(entry)
            val viewModel: AdjustmentViewModel = hiltViewModel(graph)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Spent only once a change has actually landed, and before the
            // flow leaves, so a cancelled apply costs the included cycle nothing.
            LaunchedEffect(viewModel) {
                viewModel.appliedEvents.collect { proposalId ->
                    adjustmentGate.spend(proposalId)
                    navController.leaveAdjustment()
                }
            }

            val review = state.review
            if (review == null) {
                // The answer lives in memory only: restored after process death
                // there is nothing to show, and the step behind asks again.
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                AdjustReviewScreen(
                    review = review,
                    applyError = state.applyError,
                    onApply = {
                        if (state.applyError == ApplyErrorUi.NOT_APPLIED) {
                            viewModel.retryApply()
                        } else {
                            viewModel.apply()
                        }
                    },
                    onKeepOriginal = { navController.leaveAdjustment() },
                    onFinishEarly = { navController.requestFinishEarly() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun TimeStep(
    navController: NavHostController,
    graph: NavBackStackEntry,
    adjustmentGate: AdjustmentGate,
    onAskForPro: (PaywallReason) -> Unit
) {
    val viewModel: AdjustmentViewModel = hiltViewModel(graph)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AdjustTimeScreen(
        state = state,
        onSelectMinutes = viewModel::selectMinutes,
        onTypeMinutes = viewModel::typeMinutes,
        onShowRecommendation = { navController.review(viewModel, adjustmentGate, onAskForPro) },
        onKeepPlan = { navController.leaveAdjustment() },
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun EquipmentStep(
    navController: NavHostController,
    graph: NavBackStackEntry,
    adjustmentGate: AdjustmentGate,
    onAskForPro: (PaywallReason) -> Unit
) {
    val viewModel: AdjustmentViewModel = hiltViewModel(graph)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AdjustEquipmentScreen(
        state = state,
        onSelectExercise = viewModel::selectExercise,
        onToggleEquipment = viewModel::toggleEquipment,
        onShowRecommendation = { navController.review(viewModel, adjustmentGate, onAskForPro) },
        onKeepPlan = { navController.leaveAdjustment() },
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun ContextStep(navController: NavHostController, graph: NavBackStackEntry) {
    val viewModel: AdjustmentViewModel = hiltViewModel(graph)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.routeEvents.collect { route ->
            when (route) {
                UnstuckRoute.TIME -> navController.navigate(Screen.AdjustTime.route)
                UnstuckRoute.EQUIPMENT -> navController.navigate(Screen.AdjustEquipment.route)
                UnstuckRoute.PAIN -> navController.navigate(Screen.AdjustPain.route)
                else -> Unit
            }
        }
    }

    AdjustContextScreen(
        note = state.note,
        onTypeNote = viewModel::typeNote,
        onChoose = viewModel::chooseFromContext,
        onBack = { navController.leaveAdjustment() }
    )
}

@Composable
private fun PainStep(navController: NavHostController, graph: NavBackStackEntry) {
    // Read so the step shares the flow's view model rather than starting a
    // second one; pain itself asks the policy nothing.
    hiltViewModel<AdjustmentViewModel>(graph)

    AdjustPainScreen(
        onSaveAndFinishEarly = { navController.requestFinishEarly() },
        onReturn = { navController.leaveAdjustment() },
        onBack = { navController.leaveAdjustment() }
    )
}

@Composable
private fun NavHostController.adjustEntry(entry: NavBackStackEntry): NavBackStackEntry =
    remember(entry) { getBackStackEntry(Screen.Adjust.route) }

private fun NavHostController.review(
    viewModel: AdjustmentViewModel,
    adjustmentGate: AdjustmentGate,
    onAskForPro: (PaywallReason) -> Unit
) {
    val proposalId = viewModel.showRecommendation()
    if (viewModel.uiState.value.review == null) return
    // Nothing was proposed, so there is nothing to sell: the gate answers for
    // a change to today's plan, not for being told the plan already fits.
    val ask = proposalId != null &&
        adjustmentGate.decide(proposalId) == AdjustmentGate.Decision.ASK

    // The draft stays in the graph's view model, so the answer is still there
    // when the paywall is dismissed.
    if (ask) onAskForPro(PaywallReason.ADJUST) else navigate(Screen.AdjustReview.route)
}

private fun NavHostController.leaveAdjustment() {
    popBackStack(Screen.RoutineDetail.route, inclusive = false)
}

private fun NavHostController.requestFinishEarly() {
    getBackStackEntry(Screen.RoutineDetail.route)
        .savedStateHandle[FINISH_EARLY_REQUEST] = true
    leaveAdjustment()
}

private fun android.os.Bundle?.reason(): DirectReason =
    this?.getString(Screen.Adjust.ARG_REASON)
        ?.let { name -> DirectReason.entries.firstOrNull { it.name == name } }
        ?: DirectReason.OTHER
