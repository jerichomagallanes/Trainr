package com.jericx.trainr.presentation.workout

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.jericx.trainr.presentation.AppContent
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.HiltTestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TimerFinishTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var users: UserRepository

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

    // A hold is what the countdown measured, so that set may carry the time it
    // ran for. The tick stays the person's.
    @Test
    fun aCountdownRunningOutOnAHeldSetLogsTheTimeItMeasuredAndNothingElse() {
        val dayId = seedToday(plank())
        startTodaysWorkout()

        startTimer()
        waitForTheEnd(HOLD_SECONDS)
        composeTestRule.waitUntil(SETTLE_MILLIS) { storedSet(dayId).actualSeconds != null }

        val set = storedSet(dayId)
        assertThat(set.actualSeconds).isEqualTo(HOLD_SECONDS)
        assertThat(set.actualReps).isNull()
        assertThat(set.actualWeightKg).isNull()
        assertThat(set.isCompleted).isFalse()
        assertThat(storedExercise(dayId).isCompleted).isFalse()
    }

    // Running out is nobody doing the work: the numbers it used to write became
    // next week's anchor.
    @Test
    fun aCountdownRunningOutOnAnExerciseCountedInRepsLogsNothing() {
        val dayId = seedToday(curls())
        startTodaysWorkout()

        startTimer()
        waitForTheEnd(BLOCK_SECONDS)
        composeTestRule.waitForIdle()

        val set = storedSet(dayId)
        assertThat(set.actualReps).isNull()
        assertThat(set.actualWeightKg).isNull()
        assertThat(set.actualSeconds).isNull()
        assertThat(set.isCompleted).isFalse()
        assertThat(storedExercise(dayId).isCompleted).isFalse()
    }

    private fun plank() = WorkoutExercise(
        exerciseKey = "plank",
        name = "Plank",
        measure = ExerciseMeasure.DURATION,
        sets = listOf(ExerciseSet(setNumber = 1, targetSeconds = HOLD_SECONDS)),
        durationMinutes = 1
    )

    private fun curls() = WorkoutExercise(
        exerciseKey = "bicep_curl",
        name = "Bicep Curls",
        measure = ExerciseMeasure.REPS,
        sets = listOf(ExerciseSet(setNumber = 1, targetReps = 10)),
        durationMinutes = 1
    )

    private fun startTimer() {
        composeTestRule.onNodeWithText(string(R.string.start_timer)).performScrollTo().performClick()
    }

    private fun waitForTheEnd(countdownSeconds: Int) {
        composeTestRule.waitUntil(countdownSeconds * 1_000L + SETTLE_MILLIS) {
            composeTestRule.onAllNodesWithText(string(R.string.timer_finished))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun storedSet(dayId: Long) = storedExercise(dayId).sets.single()

    private fun storedExercise(dayId: Long) = runBlocking {
        checkNotNull(users.getWorkoutDay(dayId)).exercises.single()
    }

    private fun seedToday(exercise: WorkoutExercise): Long = runBlocking {
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
                        exercises = listOf(exercise)
                    )
                )
            )
        )
        checkNotNull(users.getWeeklyWorkoutPlan(userId, 1)).workoutDays.single().id
    }

    private fun startTodaysWorkout() {
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
        waitFor(string(R.string.start_todays_workout))
        composeTestRule.onNodeWithText(string(R.string.start_todays_workout)).performClick()
        waitFor(string(R.string.start_timer))
    }

    private fun waitFor(text: String) {
        composeTestRule.waitUntil(SETTLE_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun openProGate() = ProGate(
        isPro = { false },
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
        const val HOLD_SECONDS = 5
        const val BLOCK_SECONDS = 60
        const val SETTLE_MILLIS = 20_000L
    }
}
