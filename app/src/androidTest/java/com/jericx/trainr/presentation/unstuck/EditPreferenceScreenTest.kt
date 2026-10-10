package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class EditPreferenceScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setScreen(
        state: EditPreferenceUiState = SamplePreferenceStates.editing,
        onSelectMinutes: (Int) -> Unit = {},
        onSave: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                EditPreferenceScreen(
                    state = state,
                    onSelectMinutes = onSelectMinutes,
                    onSave = onSave,
                    onCancel = onCancel
                )
            }
        }
    }

    @Test
    fun theHeadingNamesTheDayTheLimitBelongsTo() {
        setScreen()

        composeTestRule
            .onNodeWithText(string(R.string.weekday_time_limit_format, "Tuesday"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.time_for_whole_workout))
            .assertIsDisplayed()
    }

    @Test
    fun thePresetsComeFromTheState() {
        setScreen()

        SamplePreferenceStates.editing.presets.forEach {
            composeTestRule
                .onNodeWithText(string(R.string.minutes_short_format, it))
                .assertIsDisplayed()
        }
    }

    @Test
    fun choosingAPresetReportsIt() {
        var minutes: Int? = null
        setScreen(onSelectMinutes = { minutes = it })

        composeTestRule.onNodeWithText(string(R.string.minutes_short_format, 25)).performClick()

        assertThat(minutes).isEqualTo(25)
    }

    // Explicit rather than saved as it is typed: an edit is not a decision
    // until the person says so.
    @Test
    fun saveAndCancelAreBothOffered() {
        var saved = false
        var cancelled = false
        setScreen(onSave = { saved = true }, onCancel = { cancelled = true })

        composeTestRule.onNodeWithText(string(R.string.save).uppercase()).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.save).uppercase()).performClick()
        composeTestRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(saved).isTrue()
        assertThat(cancelled).isTrue()
    }

    @Test
    fun anUnsupportedValueExplainsTheRangeAndBlocksTheSave() {
        setScreen(state = SamplePreferenceStates.editingWithError)

        composeTestRule.onNodeWithText(string(R.string.adjust_time_range_error)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.save).uppercase()).assertIsNotEnabled()
    }

    @Test
    fun theEditIsSaidToApplyToLaterWorkoutsOnly() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.edit_applies_to_future))
            .assertIsDisplayed()
    }
}
