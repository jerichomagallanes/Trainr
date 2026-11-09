package com.jericx.trainr.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.purchases.AdjustmentAllowance
import com.jericx.trainr.domain.purchases.AdjustmentGate
import com.jericx.trainr.domain.purchases.FreeGenerationAllowance
import com.jericx.trainr.domain.purchases.ProGate
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.HiltTestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// A configuration change recreates the Activity, and every screen but the first
// used to be thrown away two seconds later: the splash flag came back true and
// its effect pushed the plan or the welcome on top of wherever the person was.
// The Activity is launched rather than handed content, because a remembered
// flag and a saved one are indistinguishable until the Activity actually dies.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StaysWhereYouLeftItTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var users: UserRepository

    @Inject
    lateinit var database: TrainrDatabase

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var breadcrumbs: Breadcrumbs

    private var scenario: ActivityScenario<HiltTestActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        database.clearAllTables()
    }

    @After
    fun tearDown() {
        scenario?.close()
        HiltTestActivity.contentOnLaunch(null)
    }

    @Test
    fun aDayScreenSurvivesAConfigurationChangeWithItsCountdownStillRunning() {
        seedTodaysWorkout()
        startFromTheSplash()
        openTodaysWorkout()

        composeTestRule.onNodeWithText(string(R.string.start_timer)).performScrollTo().performClick()
        waitFor(string(R.string.exercise_in_progress))
        val reading = countdownReading()

        recreate()

        waitFor(TODAY_TITLE.uppercase())
        outlastTheSplashWait()
        composeTestRule.onNodeWithText(TODAY_TITLE.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.exercise_in_progress)).assertIsDisplayed()
        // Counting, not merely labelled: a countdown restarted at the top or
        // stopped where it stood would keep the label and read differently.
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { countdownReading() != reading }
    }

    @Test
    fun aConfigurationChangeAddsNothingToTheBackStackOfADayScreen() {
        seedTodaysWorkout()
        startFromTheSplash()
        openTodaysWorkout()

        recreate()
        waitFor(TODAY_TITLE.uppercase())
        outlastTheSplashWait()
        Espresso.pressBack()

        waitForDescription(string(R.string.plan_options))
        composeTestRule.onNodeWithContentDescription(string(R.string.plan_options))
            .assertIsDisplayed()
    }

    // The worst of the three: the welcome screen landed on top of a form the
    // person was half way through answering.
    @Test
    fun anOnboardingStepSurvivesAConfigurationChange() {
        startFromTheSplash()
        openOnboarding()

        recreate()

        waitFor(string(R.string.tell_us_about_yourself))
        outlastTheSplashWait()
        composeTestRule.onNodeWithText(string(R.string.tell_us_about_yourself)).assertIsDisplayed()
        composeTestRule.onNodeWithText(button(R.string.get_started)).assertDoesNotExist()
    }

    @Test
    fun aConfigurationChangeAddsNothingToTheBackStackOfOnboarding() {
        startFromTheSplash()
        openOnboarding()

        recreate()
        waitFor(string(R.string.tell_us_about_yourself))
        outlastTheSplashWait()
        Espresso.pressBack()

        waitFor(button(R.string.get_started))
        composeTestRule.onNodeWithText(button(R.string.get_started)).assertIsDisplayed()
    }

    // The plan screen is where the push used to go, so a second copy of it hid
    // the first and the next back press looked like it did nothing.
    @Test
    fun aConfigurationChangeOnThePlanScreenDoesNotStackASecondCopyOfIt() {
        seedTodaysWorkout()
        startFromTheSplash()
        waitForDescription(string(R.string.plan_options))

        recreate()
        waitForDescription(string(R.string.plan_options))
        outlastTheSplashWait()
        Espresso.pressBackUnconditionally()

        composeTestRule.waitForIdle()
        assertThat(scenario?.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    private fun startFromTheSplash() {
        HiltTestActivity.contentOnLaunch {
            AppContent(
                versionName = "1.0",
                themePreferences = themePreferences,
                proGate = openProGate(),
                adjustmentGate = openAdjustmentGate(),
                breadcrumbs = breadcrumbs
            )
        }
        scenario = ActivityScenario.launch(HiltTestActivity::class.java)
    }

    private fun openTodaysWorkout() {
        waitFor(string(R.string.start_todays_workout))
        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()
        waitFor(string(R.string.start_timer))
    }

    private fun openOnboarding() {
        waitFor(button(R.string.get_started))
        composeTestRule.onNodeWithText(button(R.string.get_started)).performClick()
        waitFor(string(R.string.tell_us_about_yourself))
    }

    private fun recreate() {
        checkNotNull(scenario).recreate()
        composeTestRule.waitForIdle()
    }

    // The push arrived two seconds after the change, so an assertion made any
    // sooner would pass on the broken build too. Driven off the test clock, or
    // the wait the splash is sitting in never elapses.
    private fun outlastTheSplashWait() {
        composeTestRule.mainClock.advanceTimeBy(SPLASH_WAIT_MILLIS + SETTLE_MILLIS)
        composeTestRule.waitForIdle()
    }

    private fun waitFor(text: String) {
        composeTestRule.waitUntil(TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForDescription(description: String) {
        composeTestRule.waitUntil(TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Read off the one node whose description carries it, found by the part of
    // the label that does not change.
    private fun countdownReading(): String =
        composeTestRule
            .onNodeWithContentDescription(string(R.string.timer_remaining, "").trim(), substring = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first()

    private fun string(id: Int, vararg formatArgs: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *formatArgs)

    // A button label is uppercased where it is drawn, not in strings.xml.
    private fun button(id: Int) = string(id).uppercase()

    private fun seedTodaysWorkout() = runBlocking {
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
        users.saveWeeklyWorkoutPlan(
            WeeklyWorkoutPlan(
                userId = userId,
                weekNumber = 1,
                title = "Week 1",
                startDateMillis = WorkoutWeek.startOfDay(),
                workoutDays = listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        title = TODAY_TITLE,
                        status = WorkoutStatus.NOT_STARTED,
                        duration = 10,
                        exerciseCount = 1,
                        equipment = emptyList(),
                        exercises = listOf(
                            WorkoutExercise(
                                exerciseKey = "plank",
                                name = "Plank",
                                measure = ExerciseMeasure.DURATION,
                                sets = listOf(
                                    ExerciseSet(setNumber = 1, targetSeconds = HOLD_SECONDS)
                                ),
                                durationMinutes = 1
                            )
                        )
                    )
                )
            )
        )
        Unit
    }

    private fun openProGate() = ProGate(
        isPro = { false },
        hasRead = { true },
        readFailed = { false },
        canSell = { true },
        allowance = object : FreeGenerationAllowance {
            override fun hasBeenUsed() = false
            override fun markUsed() = Unit
        }
    )

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
        const val TODAY_TITLE = "Core"
        const val HOLD_SECONDS = 600
        const val SPLASH_WAIT_MILLIS = 2_000L
        const val SETTLE_MILLIS = 1_500L
        const val TIMEOUT_MILLIS = 20_000L
    }
}
