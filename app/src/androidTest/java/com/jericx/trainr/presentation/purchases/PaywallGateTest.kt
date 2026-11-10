package com.jericx.trainr.presentation.purchases

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.purchases.AdjustmentAllowance
import com.jericx.trainr.domain.purchases.AdjustmentGate
import com.jericx.trainr.domain.purchases.FreeGenerationAllowance
import com.jericx.trainr.domain.purchases.ProGate
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.AppContent
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.HiltTestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Every way to reach another week, walked as someone whose free week is gone.
// The gate is wired in the navigation, so a screen's own tests cannot see it:
// repeating a week was ungated for three releases because nothing came through
// here.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PaywallGateTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var repository: UserRepository

    @Inject
    lateinit var database: TrainrDatabase

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var breadcrumbs: Breadcrumbs

    @Before
    fun setUp() {
        hiltRule.inject()
        database.clearAllTables()
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    // A finished week is over and logged, so the menu offers the next one; a
    // week still being trained is this week, so it offers a rewrite instead.
    private fun seed(finished: Boolean) = runBlocking {
        val start = WorkoutWeek.startOfDay() - TimeUnit.DAYS.toMillis(if (finished) 9 else 2)
        val userId = repository.saveUser(UserProfile(firstName = "Jericho", age = 30))
        val status = if (finished) WorkoutStatus.COMPLETED else WorkoutStatus.NOT_STARTED
        repository.saveWeeklyWorkoutPlan(
            SampleWorkoutData.weekOne.copy(
                id = 0,
                userId = userId,
                startDateMillis = start,
                workoutDays = SampleWorkoutData.weekOne.workoutDays.map {
                    it.copy(status = status, completedAt = if (finished) start else null)
                }
            )
        )
    }

    private fun spentGate(used: Boolean = true, hasRead: Boolean = true) = ProGate(
        isPro = { false },
        hasRead = { hasRead },
        readFailed = { false },
        canSell = { true },
        allowance = object : FreeGenerationAllowance {
            override fun hasBeenUsed() = used
            override fun markUsed() = Unit
        }
    )

    // Nothing here exercises the adjustment allowance, so it stays open and
    // cannot turn an unrelated tap into a Pro prompt.
    private fun openAdjustmentGate(hasRead: Boolean = true) = AdjustmentGate(
        isPro = { false },
        hasRead = { hasRead },
        readFailed = { false },
        canSell = { true },
        allowance = object : AdjustmentAllowance {
            override fun includedCycleId(): String? = null
            override fun consume(cycleId: String) = Unit
            override fun restore(cycleId: String) = Unit
        }
    )

    private fun startApp(
        gate: ProGate = spentGate(),
        adjustmentGate: AdjustmentGate = openAdjustmentGate()
    ) {
        composeTestRule.setContent {
            AppContent(
                versionName = "1.0",
                themePreferences = themePreferences,
                proGate = gate,
                adjustmentGate = adjustmentGate,
                breadcrumbs = breadcrumbs,
                startDestination = Screen.Home.route
            )
        }
        waitFor(string(R.string.plan_options), isContentDescription = true)
    }

    private fun waitFor(text: String, isContentDescription: Boolean = false) {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            val nodes = if (isContentDescription) {
                composeTestRule.onAllNodesWithContentDescription(text)
            } else {
                composeTestRule.onAllNodesWithText(text)
            }
            nodes.fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openPlanMenu() {
        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options)).performClick()
    }

    private fun assertAsksForPro() {
        waitFor(string(R.string.pro_upgrade_title))
        composeTestRule.onNodeWithText(string(R.string.pro_upgrade_title)).assertIsDisplayed()
    }

    private fun assertDoesNotAskForPro() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(string(R.string.pro_upgrade_title)).assertDoesNotExist()
    }

    // The one that was missed: a copied week is built without a generator,
    // which was taken to mean it asks nothing of the client either.
    @Test
    fun repeatingAWeekAsksForPro() {
        seed(finished = true)
        startApp()

        openPlanMenu()
        composeTestRule.onNodeWithText(string(R.string.repeat_this_week)).performClick()

        assertAsksForPro()
    }

    @Test
    fun generatingNextWeekAsksForPro() {
        seed(finished = true)
        startApp()

        openPlanMenu()
        composeTestRule.onNodeWithText(string(R.string.generate_next_week)).performClick()

        assertAsksForPro()
    }

    @Test
    fun regeneratingThisWeekAsksForPro() {
        seed(finished = false)
        startApp()

        openPlanMenu()
        composeTestRule.onNodeWithText(string(R.string.regenerate_week)).performClick()

        assertAsksForPro()
    }

    // The worst of them: the alert said every week and everything logged would
    // be erased, and the gate only spoke after the whole profile was walked.
    @Test
    fun startingANewPlanAsksForProBeforeThreateningToEraseAnything() {
        seed(finished = true)
        startApp()

        openPlanMenu()
        composeTestRule.onNodeWithText(string(R.string.regenerate_plan)).performClick()

        assertAsksForPro()
        composeTestRule.onNodeWithText(string(R.string.leave_plan_title)).assertDoesNotExist()
    }

    // The gate has to let the first week through, or the test above would pass
    // on an app that asks everyone for money at every turn.
    @Test
    fun aClientWhoStillHasTheirFreeWeekIsNotAsked() {
        seed(finished = true)
        startApp(gate = spentGate(used = false))

        openPlanMenu()
        composeTestRule.onNodeWithText(string(R.string.repeat_this_week)).performClick()

        assertDoesNotAskForPro()
    }

    // Reading the plan is not a paid action, so the menu itself must stay open
    // to someone who has spent their week.
    @Test
    fun readingThePlanIsNeverAskedToPay() {
        seed(finished = true)
        startApp()

        composeTestRule.onNodeWithText(SampleWorkoutData.weekOne.workoutDays.first().title)
            .assertIsDisplayed()
        assertDoesNotAskForPro()
    }

    // A tap that lands before the store has answered. A subscriber reads as not
    // Pro for the whole of that gap, so this prompt was being shown to people
    // who had already paid. Neither branch is honest yet: letting it through
    // gives a week away instead. The person is told to try again.
    @Test
    fun aTapBeforeTheStoreHasAnsweredIsToldToTryAgain() {
        seed(finished = true)
        startApp(gate = spentGate(hasRead = false))

        openPlanMenu()
        composeTestRule.onNodeWithText(string(R.string.repeat_this_week)).performClick()

        waitFor(string(R.string.pro_still_checking))
        composeTestRule.onNodeWithText(string(R.string.pro_still_checking)).assertIsDisplayed()
        assertDoesNotAskForPro()
    }

    // Two sample days' unperformed exercises on today, so 35 minutes is a real
    // cut and the policy has something to propose.
    private fun seedLongDayToday() = runBlocking {
        val userId = repository.saveUser(
            UserProfile(
                firstName = "Jericho",
                age = 30,
                gender = Gender.MALE,
                weight = 80f,
                fitnessGoal = FitnessGoal.MUSCLE_GAIN,
                experienceLevel = ExperienceLevel.INTERMEDIATE,
                availableEquipment = listOf(Equipment.DUMBBELL),
                workoutDaysPerWeek = 3,
                workoutDuration = 40
            )
        )
        val exercises =
            (SampleWorkoutData.dayFor(5).exercises + SampleWorkoutData.dayFor(3).exercises)
                .filter { exercise -> exercise.sets.none { it.isCompleted } }
        repository.saveWeeklyWorkoutPlan(
            SampleWorkoutData.weekOne.copy(
                id = 0,
                userId = userId,
                startDateMillis = WorkoutWeek.startOfDay(),
                workoutDays = listOf(
                    SampleWorkoutData.dayFor(5).copy(
                        id = 0,
                        dayNumber = 1,
                        status = WorkoutStatus.NOT_STARTED,
                        exerciseCount = exercises.size,
                        exercises = exercises
                    )
                )
            )
        )
    }

    // The adjustment gate answers from its own graph, and the wait is threaded
    // through each step separately: a branch wired to one of them still passes
    // every unit test there is.
    @Test
    fun anAdjustmentBeforeTheStoreHasAnsweredIsToldToTryAgain() {
        seedLongDayToday()
        startApp(adjustmentGate = openAdjustmentGate(hasRead = false))

        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()
        waitFor(string(R.string.adjust_today))
        composeTestRule.onNodeWithText(string(R.string.adjust_today))
            .performScrollTo().performClick()
        waitFor(string(R.string.adjust_reason_time))
        composeTestRule.onNodeWithText(string(R.string.adjust_reason_time))
            .performScrollTo().performClick()
        waitFor(string(R.string.adjust_time_title))
        composeTestRule.onNode(hasSetTextAction()).performTextInput("35")
        composeTestRule.onNodeWithText(string(R.string.show_recommendation).uppercase())
            .performScrollTo().performClick()

        waitFor(string(R.string.pro_still_checking))
        composeTestRule.onNodeWithText(string(R.string.pro_still_checking)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.adjust_review_time_title))
            .assertDoesNotExist()
        assertDoesNotAskForPro()
    }
}
