package com.jericx.trainr.presentation

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
    data object DayCompleted : Screen("day_completed_screen/{dayNumber}") {
        const val ARG_DAY_NUMBER = "dayNumber"

        fun createRoute(dayNumber: Int) = "day_completed_screen/$dayNumber"
    }
    data object WeekCompleted : Screen("week_completed_screen/{weekNumber}") {
        const val ARG_WEEK_NUMBER = "weekNumber"

        fun createRoute(weekNumber: Int) = "week_completed_screen/$weekNumber"
    }
}
