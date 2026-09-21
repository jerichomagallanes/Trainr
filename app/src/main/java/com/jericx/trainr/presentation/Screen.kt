package com.jericx.trainr.presentation

import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.purchases.PaywallReason

sealed class Screen(val route: String) {
    data object SplashScreen : Screen("splash_screen")
    data object Welcome : Screen("welcome_screen")
    data object BasicInfo : EditableStep("basic_info_screen")
    data object BodyMetrics : EditableStep("body_metrics_screen")
    data object FitnessGoal : EditableStep("fitness_goal_screen")
    data object WorkoutSetup : EditableStep("workout_setup_screen")
    data object Limitations : EditableStep("limitations_screen")
    data object Review : Screen("review_screen?fromPlan={fromPlan}&profileOnly={profileOnly}") {
        const val ARG_FROM_PLAN = "fromPlan"

        // Editing the profile without regenerating: the review saves and
        // returns instead of leading into generation.
        const val ARG_PROFILE_ONLY = "profileOnly"

        fun createRoute(fromPlan: Boolean = false, profileOnly: Boolean = false) =
            "review_screen?fromPlan=$fromPlan&profileOnly=$profileOnly"
    }
    data object Generating : Screen("generating_screen")
    data object GeneratingNextWeek : Screen("generating_next_week_screen")
    data object RegeneratingWeek : Screen("regenerating_week_screen")

    // An onboarding step that the review screen can reopen on its own: in
    // edit mode, finishing the step returns to the review instead of walking
    // the rest of the flow.
    sealed class EditableStep(val baseRoute: String) : Screen("$baseRoute?edit={edit}") {
        fun createRoute(edit: Boolean = false) = "$baseRoute?edit=$edit"

        companion object {
            const val ARG_EDIT = "edit"
        }
    }
    // The reason someone reached a paid action, carried through so the paywall
    // can lead with it rather than with every feature at once.
    data object Paywall : Screen("paywall_screen?reason={reason}") {
        const val ARG_REASON = "reason"

        fun createRoute(reason: PaywallReason) = "paywall_screen?reason=${reason.name}"
    }

    // Whether this shows the offer or the subscription depends on the
    // entitlement, which is not the screen's identity.
    data object Pro : Screen("pro_screen")

    data object Home : Screen("home_screen")

    // One stored week, opened from Weekly Progress. Home always shows the
    // newest week, so this is the only way back into an earlier one.
    data object WeekPlan : Screen("week_plan_screen/{weekNumber}") {
        const val ARG_WEEK_NUMBER = "weekNumber"

        fun createRoute(weekNumber: Int) = "week_plan_screen/$weekNumber"
    }
    data object WeeklyProgress : Screen("weekly_progress_screen")
    data object RoutineDetail : Screen("routine_detail_screen/{dayNumber}?weekNumber={weekNumber}") {
        const val ARG_DAY_NUMBER = "dayNumber"
        const val ARG_WEEK_NUMBER = "weekNumber"

        // Days opened from home carry no week: they belong to the newest one.
        const val LATEST_WEEK = -1

        fun createRoute(dayNumber: Int, weekNumber: Int = LATEST_WEEK) =
            "routine_detail_screen/$dayNumber?weekNumber=$weekNumber"
    }
    // A nested graph so the draft lives exactly as long as the flow does: the
    // shared view model is scoped to this entry and dies when the graph pops.
    data object Adjust : Screen(
        "adjust_graph/{dayNumber}?weekNumber={weekNumber}&reason={reason}&exerciseId={exerciseId}"
    ) {
        const val ARG_DAY_NUMBER = "dayNumber"
        const val ARG_WEEK_NUMBER = "weekNumber"
        const val ARG_REASON = "reason"
        const val ARG_EXERCISE_ID = "exerciseId"

        const val NO_EXERCISE = -1L

        fun createRoute(
            dayNumber: Int,
            weekNumber: Int,
            reason: DirectReason,
            exerciseId: Long? = null
        ) = "adjust_graph/$dayNumber?weekNumber=$weekNumber&reason=${reason.name}" +
            "&exerciseId=${exerciseId ?: NO_EXERCISE}"
    }
    // The graph carries the arguments, so its first screen is picked from the
    // reason rather than from four graphs that differ only in where they open.
    data object AdjustEntry : Screen("adjust_entry")
    data object AdjustTime : Screen("adjust_time")
    data object AdjustEquipment : Screen("adjust_equipment")
    data object AdjustReview : Screen("adjust_review")
    data object AdjustContext : Screen("adjust_context")
    data object AdjustPain : Screen("adjust_pain")

    // The week travels with the day: the follow-up has to find the session
    // that was just saved, which need not be in the newest week.
    data object DayCompleted : Screen("day_completed_screen/{dayNumber}?weekNumber={weekNumber}") {
        const val ARG_DAY_NUMBER = "dayNumber"
        const val ARG_WEEK_NUMBER = "weekNumber"

        fun createRoute(dayNumber: Int, weekNumber: Int) =
            "day_completed_screen/$dayNumber?weekNumber=$weekNumber"
    }
    data object SessionSaved : Screen(
        "session_saved_screen/{dayNumber}?performed={performed}&planned={planned}" +
            "&weekNumber={weekNumber}"
    ) {
        const val ARG_DAY_NUMBER = "dayNumber"
        const val ARG_PERFORMED = "performed"
        const val ARG_PLANNED = "planned"
        const val ARG_WEEK_NUMBER = "weekNumber"

        fun createRoute(dayNumber: Int, performed: Int, planned: Int, weekNumber: Int) =
            "session_saved_screen/$dayNumber?performed=$performed&planned=$planned" +
                "&weekNumber=$weekNumber"
    }
    // The week is what is celebrated; the day is carried so the follow-up can
    // ask about the session that has just been saved.
    data object WeekCompleted : Screen("week_completed_screen/{weekNumber}?dayNumber={dayNumber}") {
        const val ARG_WEEK_NUMBER = "weekNumber"
        const val ARG_DAY_NUMBER = "dayNumber"

        const val NO_DAY = -1

        fun createRoute(weekNumber: Int, dayNumber: Int = NO_DAY) =
            "week_completed_screen/$weekNumber?dayNumber=$dayNumber"
    }

    // The question and its follow-up write against one adjustment, so the graph
    // owns the id and every step reads the same view model scoped to it.
    data object Feedback : Screen("feedback_graph/{adjustmentId}") {
        const val ARG_ADJUSTMENT_ID = "adjustmentId"

        fun createRoute(adjustmentId: Long) = "feedback_graph/$adjustmentId"
    }
    data object AdjustmentFeedback : Screen("adjustment_feedback")
    data object FeedbackDetail : Screen("feedback_detail")
    data object FeedbackOutcome : Screen("feedback_outcome")
    data object FeedbackPain : Screen("feedback_pain")
}
