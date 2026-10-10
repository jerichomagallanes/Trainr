package com.jericx.trainr.presentation.unstuck

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.model.ModelInstaller
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
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
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.intent.InterpreterAvailability
import com.jericx.trainr.domain.unstuck.intent.IntentInterpreter
import com.jericx.trainr.presentation.AppContent
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.HiltTestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UnstuckJourneyTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var users: UserRepository

    @Inject
    lateinit var adjustments: AdjustmentRepository

    @Inject
    lateinit var database: TrainrDatabase

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var breadcrumbs: Breadcrumbs

    @Inject
    lateinit var interpreter: IntentInterpreter

    @Inject
    lateinit var installer: ModelInstaller

    @Before
    fun setUp() {
        hiltRule.inject()
        assumeTrue("no model at ${installer.file.path}", installer.file.exists())
        assumeTrue(
            "interpreter is ${interpreter.availability}",
            interpreter.availability == InterpreterAvailability.READY
        )
        database.clearAllTables()
    }

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun button(id: Int) = composeTestRule.onNodeWithText(string(id).uppercase())

    private fun plural(id: Int, quantity: Int) =
        composeTestRule.activity.resources.getQuantityString(id, quantity, quantity)

    @Test
    fun aNoteAboutTimeBecomesAnAppliedShorterSession() {
        val dayId = seedToday()
        startApp()

        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()
        waitFor(string(R.string.adjust_today))
        composeTestRule.onNodeWithText(string(R.string.adjust_today)).performClick()
        waitFor(string(R.string.adjust_reason_other))
        composeTestRule.onNodeWithText(string(R.string.adjust_reason_other)).performClick()
        waitFor(string(R.string.context_title))

        composeTestRule.onNode(hasSetTextAction()).performTextInput(NOTE)
        button(R.string.context_use_note).assertIsEnabled().performClick()

        waitFor(string(R.string.adjust_time_title), timeoutMillis = 60_000)
        composeTestRule.onNodeWithText(string(R.string.adjust_time_whole_session)).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction() and hasText("35")).assertIsDisplayed()
        button(R.string.show_recommendation).assertIsEnabled()
        assertNothingPersisted(dayId)

        button(R.string.show_recommendation).performClick()
        waitFor(string(R.string.adjust_review_time_title))
        composeTestRule.onNodeWithText(plural(R.plurals.adjust_review_time_line_format, 35))
            .assertIsDisplayed()
        button(R.string.continue_workout).assertDoesNotExist()
        button(R.string.use_this_workout).assertIsDisplayed()
        assertNothingPersisted(dayId)

        button(R.string.use_this_workout).performClick()
        waitFor(string(R.string.adjusted_for_today))

        val applied = runBlocking { adjustments.getAdjustments(dayId) }.single()
        assertThat(applied.reason).isEqualTo(AdjustmentReason.LESS_TIME)
        assertThat(applied.isActive).isTrue()
        assertThat(runBlocking { adjustments.getNote(dayId) }?.text).isEqualTo(NOTE)
    }

    // Two sample days' unperformed exercises, so 35 minutes is a real cut.
    private fun seedToday(): Long = runBlocking {
        val userId = users.saveUser(
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
        val week = SampleWorkoutData.weekOne
        val exercises = (SampleWorkoutData.dayFor(5).exercises + SampleWorkoutData.dayFor(3).exercises)
            .filter { exercise -> exercise.sets.none { it.isCompleted } }
        val today = SampleWorkoutData.dayFor(5).copy(
            id = 0,
            dayNumber = 1,
            status = WorkoutStatus.NOT_STARTED,
            exerciseCount = exercises.size,
            exercises = exercises
        )
        users.saveWeeklyWorkoutPlan(
            week.copy(
                id = 0,
                userId = userId,
                startDateMillis = WorkoutWeek.startOfDay(),
                workoutDays = listOf(today)
            )
        )
        val stored = checkNotNull(users.getWeeklyWorkoutPlan(userId, week.weekNumber))
            .workoutDays
            .single()
        assertThat(stored.exercises.size).isAtLeast(3)
        assertThat(stored.exercises.flatMap { it.sets }.none { it.isCompleted }).isTrue()
        stored.id
    }

    private fun assertNothingPersisted(dayId: Long) = runBlocking {
        assertThat(adjustments.getAdjustments(dayId)).isEmpty()
        assertThat(adjustments.getNote(dayId)).isNull()
    }

    private fun startApp() {
        composeTestRule.setContent {
            AppContent(
                versionName = "1.0",
                themePreferences = themePreferences,
                proGate = openProGate(),
                adjustmentGate = openAdjustmentGate(),
                breadcrumbs = breadcrumbs,
                startDestination = Screen.Home.route
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithContentDescription(string(R.string.plan_options))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitFor(text: String, timeoutMillis: Long = 20_000) {
        composeTestRule.waitUntil(timeoutMillis) { nodesShowing(text) > 0 }
    }

    private fun nodesShowing(text: String): Int =
        composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun openProGate() = ProGate(
        isPro = { false },
        canSell = { true },
        allowance = object : FreeGenerationAllowance {
            override fun hasBeenUsed() = false
            override fun markUsed() = Unit
        }
    )

    // The device's stored allowance may already be spent; the paywall is not under test.
    private fun openAdjustmentGate() = AdjustmentGate(
        isPro = { false },
        canSell = { true },
        allowance = object : AdjustmentAllowance {
            override fun includedCycleId(): String? = null
            override fun consume(cycleId: String) = Unit
            override fun restore(cycleId: String) = Unit
        }
    )

    private companion object {
        const val NOTE = "I have 35 minutes for the whole workout today."
    }
}
