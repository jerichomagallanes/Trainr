package com.jericx.trainr.presentation.onboarding

import android.os.Bundle
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.data.local.TrainrDatabase
import com.jericx.trainr.data.preferences.ThemePreferences
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.purchases.AdjustmentAllowance
import com.jericx.trainr.domain.purchases.AdjustmentGate
import com.jericx.trainr.domain.purchases.FreeGenerationAllowance
import com.jericx.trainr.domain.purchases.ProGate
import com.jericx.trainr.presentation.AppContent
import com.jericx.trainr.testing.HiltTestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Toggling the system theme, the display font size or the language recreates
// the Activity, and every onboarding answer used to go with it. The Activity is
// launched rather than handed content, because only a real recreation tells a
// remembered answer from a kept one.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KeepsWhatYouTypedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

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
    fun theFirstStepKeepsWhatWasTypedAcrossARecreation() {
        openOnboarding()
        answerBasicInfo(submit = false)

        recreate(onto = string(R.string.tell_us_about_yourself))

        composeTestRule.onNodeWithText(NAME).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(AGE).performScrollTo().assertIsDisplayed()
        // Enabled only with all four answered, so the gender and the experience
        // came back too.
        composeTestRule.onNodeWithText(button(R.string.next)).assertIsEnabled()
    }

    @Test
    fun theMeasurementsStepKeepsWhatWasTypedAcrossARecreation() {
        openOnboarding()
        answerBasicInfo()
        answerMeasurements(submit = false)

        recreate(onto = string(R.string.your_measurements))

        composeTestRule.onNodeWithText(HEIGHT).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(WEIGHT).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theGoalStepKeepsWhatWasChosenAcrossARecreation() {
        openOnboarding()
        answerBasicInfo()
        answerMeasurements()
        answerGoal(submit = false)

        recreate(onto = string(R.string.your_fitness_goals))

        composeTestRule.onNodeWithText(button(R.string.next)).assertIsEnabled()
    }

    @Test
    fun theSetupStepKeepsWhatWasChosenAcrossARecreation() {
        openOnboarding()
        answerBasicInfo()
        answerMeasurements()
        answerGoal()
        answerSetup(submit = false)

        recreate(onto = string(R.string.set_up_your_workout))

        composeTestRule.onNodeWithText(threeDays()).performScrollTo().assertIsDisplayed()
        // Only shown for kit that has weights on it, so the equipment answer
        // came back too.
        composeTestRule.onNodeWithText(string(R.string.weights_marked_in)).assertExists()
        // Enabled only with equipment, days and a session length answered.
        composeTestRule.onNodeWithText(button(R.string.next)).assertIsEnabled()
    }

    @Test
    fun theLimitationsStepKeepsWhatWasTickedAcrossARecreation() {
        openOnboarding()
        answerBasicInfo()
        answerMeasurements()
        answerGoal()
        answerSetup()
        waitFor(string(R.string.lets_keep_you_safe))
        composeTestRule.onNodeWithText(string(R.string.knee_problems_injury))
            .performScrollTo()
            .performClick()

        recreate(onto = string(R.string.lets_keep_you_safe))

        composeTestRule.onNodeWithText(button(R.string.submit)).performClick()
        waitFor(string(R.string.your_fitness_profile))
        composeTestRule.onNodeWithText(string(R.string.knee_problems_injury))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // The other half of the fix: a menu is not an answer, and one left open is
    // meant to be gone.
    @Test
    fun anOpenDropdownClosesOnARecreationWithItsChoiceKept() {
        openOnboarding()
        answerBasicInfo()
        answerMeasurements()
        answerGoal()
        answerSetup(submit = false)
        composeTestRule.onNodeWithText(string(R.string.workout_days_per_week))
            .performScrollTo()
        composeTestRule.onNodeWithText(threeDays()).performClick()
        composeTestRule.onAllNodesWithText(threeDays()).assertCountEquals(2)

        recreate(onto = string(R.string.set_up_your_workout))

        // One left: the field, not the menu over it.
        composeTestRule.onAllNodesWithText(threeDays()).assertCountEquals(1)
    }

    @Test
    fun anOpenSheetClosesOnARecreation() {
        openOnboarding()
        answerBasicInfo()
        waitFor(string(R.string.your_measurements))
        composeTestRule.onNodeWithTag("bmiSources").performScrollTo().performClick()
        waitFor(string(R.string.bmi_limitations))

        recreate(onto = string(R.string.your_measurements))

        composeTestRule.onNodeWithText(string(R.string.bmi_limitations)).assertDoesNotExist()
    }

    // The lifetime a view model buys over a remember, which a recreation alone
    // would not show: a step left before NEXT is still the person's answer when
    // they come back to it.
    @Test
    fun aStepLeftBeforeNextKeepsWhatWasTypedOnTheWayBack() {
        openOnboarding()
        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name))
            .performTextInput(NAME)

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).performClick()
        waitFor(button(R.string.get_started))
        composeTestRule.onNodeWithText(button(R.string.get_started)).performClick()
        waitFor(string(R.string.tell_us_about_yourself))

        composeTestRule.onNodeWithText(NAME).performScrollTo().assertIsDisplayed()
    }

    // Saved state is the only thing a killed process leaves behind, so the name
    // being absent from it is the whole of the guarantee: made saveable instead,
    // the form would come back onto a profile nobody ever saved. The relaunch
    // says what the person gets instead, which is the flow from the start.
    @Test
    fun aHalfAnsweredFormIsGoneOnceTheProcessIs() {
        openOnboarding()
        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name))
            .performTextInput(NAME)
        composeTestRule.waitForIdle()

        assertThat(whatTheSystemWouldKeep()).doesNotContain(NAME)

        scenario?.close()
        scenario = null
        startFromTheSplash()

        waitFor(button(R.string.get_started))
        composeTestRule.onNodeWithText(button(R.string.get_started)).assertIsDisplayed()
        composeTestRule.onNodeWithText(NAME).assertDoesNotExist()
    }

    private fun whatTheSystemWouldKeep(): String {
        val saved = Bundle()
        checkNotNull(scenario).onActivity { activity ->
            InstrumentationRegistry.getInstrumentation()
                .callActivityOnSaveInstanceState(activity, saved)
        }
        return flattened(saved)
    }

    @Suppress("DEPRECATION")
    private fun flattened(value: Any?): String = when (value) {
        is Bundle -> value.keySet().joinToString { "$it=" + flattened(value.get(it)) }
        is Iterable<*> -> value.joinToString { flattened(it) }
        is Array<*> -> value.joinToString { flattened(it) }
        else -> value.toString()
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

    private fun openOnboarding() {
        startFromTheSplash()
        waitFor(button(R.string.get_started))
        composeTestRule.onNodeWithText(button(R.string.get_started)).performClick()
        waitFor(string(R.string.tell_us_about_yourself))
    }

    private fun answerBasicInfo(submit: Boolean = true) {
        composeTestRule.onNodeWithText(string(R.string.enter_your_first_name))
            .performTextInput(NAME)
        composeTestRule.onNodeWithText(string(R.string.enter_your_age)).performTextInput(AGE)
        // The keyboard shortens the form, so an answer below it has to be
        // scrolled back into view before it can be tapped.
        composeTestRule.onNodeWithText(string(R.string.male)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.beginner)).performScrollTo().performClick()
        if (submit) {
            composeTestRule.onNodeWithText(button(R.string.next)).performClick()
            waitFor(string(R.string.your_measurements))
        }
    }

    private fun answerMeasurements(submit: Boolean = true) {
        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performScrollTo()
            .performTextInput(HEIGHT)
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performScrollTo()
            .performTextInput(WEIGHT)
        if (submit) {
            composeTestRule.onNodeWithText(button(R.string.next)).performClick()
            waitFor(string(R.string.your_fitness_goals))
        }
    }

    private fun answerGoal(submit: Boolean = true) {
        composeTestRule.onNodeWithText(string(R.string.build_muscle))
            .performScrollTo()
            .performClick()
        if (submit) {
            composeTestRule.onNodeWithText(button(R.string.next)).performClick()
            waitFor(string(R.string.set_up_your_workout))
        }
    }

    private fun answerSetup(submit: Boolean = true) {
        composeTestRule.onNodeWithText(string(R.string.equipment_dumbbell))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.select_days_placeholder))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(threeDays()).performClick()
        // The menu has to be gone before the next tap, or the tap that closes
        // it is the one the chip never gets.
        composeTestRule.waitUntil(TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(threeDays()).fetchSemanticsNodes().size == 1
        }
        composeTestRule.onNodeWithText(fortyFiveMinutes()).performScrollTo().performClick()
        if (submit) {
            composeTestRule.onNodeWithText(button(R.string.next)).performClick()
            waitFor(string(R.string.lets_keep_you_safe))
        }
    }

    private fun recreate(onto: String) {
        composeTestRule.waitForIdle()
        checkNotNull(scenario).recreate()
        composeTestRule.waitForIdle()
        waitFor(onto)
        // The splash resolves two seconds after a launch, so an assertion made
        // any sooner would pass on a build that walks over the step afterwards.
        composeTestRule.mainClock.advanceTimeBy(SPLASH_WAIT_MILLIS + SETTLE_MILLIS)
        composeTestRule.waitForIdle()
    }

    private fun waitFor(text: String) {
        composeTestRule.waitUntil(TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    // A button label is uppercased where it is drawn, not in strings.xml.
    private fun button(id: Int) = string(id).uppercase()

    private fun threeDays() = plural(R.plurals.workout_days_option, DAYS)

    private fun fortyFiveMinutes() = plural(R.plurals.minutes, DURATION)

    private fun plural(id: Int, quantity: Int) = InstrumentationRegistry.getInstrumentation()
        .targetContext.resources.getQuantityString(id, quantity, quantity)

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
        hasRead = { true },
        readFailed = { false },
        canSell = { true },
        allowance = object : AdjustmentAllowance {
            override fun includedCycleId(): String? = null
            override fun consume(cycleId: String) = Unit
            override fun restore(cycleId: String) = Unit
        }
    )

    private companion object {
        const val NAME = "Jericho"
        const val AGE = "31"
        const val HEIGHT = "178"
        const val WEIGHT = "72"
        const val DAYS = 3
        const val DURATION = 45
        const val SPLASH_WAIT_MILLIS = 2_000L
        const val SETTLE_MILLIS = 1_500L
        const val TIMEOUT_MILLIS = 20_000L
    }
}
