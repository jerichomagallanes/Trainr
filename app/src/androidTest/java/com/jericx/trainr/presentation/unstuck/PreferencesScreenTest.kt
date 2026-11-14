package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun plural(id: Int, count: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(id, count, *args)

    private fun setScreen(
        state: PreferencesUiState = SamplePreferenceStates.filled,
        onEdit: (Long) -> Unit = {},
        onForget: (Long) -> Unit = {},
        onDeleteNote: (Long) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                PreferencesScreen(
                    state = state,
                    onEdit = onEdit,
                    onForget = onForget,
                    onDeleteNote = onDeleteNote
                )
            }
        }
    }

    @Test
    fun nothingRememberedSaysSoRatherThanShowingAnEmptyList() {
        setScreen(state = SamplePreferenceStates.empty)

        composeTestRule.onNodeWithText(string(R.string.no_preferences_yet)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.forget)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.delete_note)).assertDoesNotExist()
    }

    @Test
    fun aStoredLimitNamesItsDayItsLengthAndWhoConfirmedIt() {
        setScreen()

        composeTestRule
            .onNodeWithText(string(R.string.weekday_time_limit_format, "Tuesday"))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(plural(R.plurals.minutes_for_whole_session_format, 35, 35))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.confirmed_by_you_format, "12 Sept 2026"))
            .assertIsDisplayed()
    }

    @Test
    fun aStoredLimitCanBeEditedOrForgotten() {
        var edited: Long? = null
        var forgotten: Long? = null
        setScreen(onEdit = { edited = it }, onForget = { forgotten = it })

        composeTestRule.onNodeWithText(string(R.string.edit)).performClick()
        composeTestRule.onNodeWithText(string(R.string.forget)).performClick()

        assertThat(edited).isEqualTo(1L)
        assertThat(forgotten).isEqualTo(1L)
    }

    @Test
    fun aNoteIsShownAsWrittenAndCanBeDeleted() {
        var deleted: Long? = null
        setScreen(onDeleteNote = { deleted = it })

        composeTestRule.onNodeWithText(string(R.string.your_session_note)).assertIsDisplayed()
        composeTestRule.onNodeWithText("I had to leave early for work.").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.delete_note)).performClick()

        assertThat(deleted).isEqualTo(1L)
    }

    @Test
    fun todaysAdjustmentIsNamedAndMarkedAsTodayOnly() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.todays_adjustment)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.adjustment_shorter_today))
            .assertIsDisplayed()
    }

    @Test
    fun noAdjustmentTodaySaysSo() {
        setScreen(state = SamplePreferenceStates.empty)

        composeTestRule.onNodeWithText(string(R.string.no_adjustment_applied)).assertIsDisplayed()
    }

    // Forgetting stops future use and nothing else; the screen has to say so.
    @Test
    fun forgettingIsExplainedBeforeItIsOffered() {
        setScreen()

        composeTestRule
            .onNodeWithText(string(R.string.forget_does_not_remove_workouts))
            .assertIsDisplayed()
    }
}
