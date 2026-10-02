package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustTodaySheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setSheet(
        onChoose: (DirectReason) -> Unit = {},
        onShowHowTo: (Int) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustTodaySheet(
                    dayTitle = "Upper Body",
                    exercises = listOf("Bench Press", "Bent Over Row"),
                    onChoose = onChoose,
                    onShowHowTo = onShowHowTo,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun theSheetOffersEveryReason() {
        setSheet()

        composeTestRule.onNodeWithText(string(R.string.adjust_sheet_title)).assertIsDisplayed()
        listOf(
            R.string.adjust_reason_time,
            R.string.adjust_reason_equipment,
            R.string.adjust_reason_guidance,
            R.string.adjust_reason_pain,
            R.string.adjust_reason_other
        ).forEach {
            composeTestRule.onNodeWithText(string(it)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun choosingAReasonReportsIt() {
        var chosen: DirectReason? = null
        setSheet(onChoose = { chosen = it })

        composeTestRule.onNodeWithText(string(R.string.adjust_reason_pain)).performScrollTo()
            .performClick()

        assertThat(chosen).isEqualTo(DirectReason.PAIN)
    }

    // Show me how never leaves the session: it picks an exercise and opens the
    // tutorial already on its card.
    @Test
    fun showMeHowPicksAnExerciseInsteadOfARoute() {
        var position: Int? = null
        var chosen: DirectReason? = null
        setSheet(onChoose = { chosen = it }, onShowHowTo = { position = it })

        composeTestRule.onNodeWithText(string(R.string.adjust_reason_guidance)).performClick()
        composeTestRule.onNodeWithText(string(R.string.guide_pick_exercise)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Bent Over Row").performClick()

        assertThat(position).isEqualTo(2)
        assertThat(chosen).isNull()
    }

    @Test
    fun keepingTodaysPlanDismisses() {
        var dismissed = false
        setSheet(onDismiss = { dismissed = true })

        composeTestRule.onNodeWithText(string(R.string.keep_todays_plan)).performScrollTo()
            .performClick()

        assertThat(dismissed).isTrue()
    }
}
