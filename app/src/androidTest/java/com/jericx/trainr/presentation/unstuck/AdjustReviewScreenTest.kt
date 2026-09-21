package com.jericx.trainr.presentation.unstuck

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.unstuck.InfeasibleReason
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.testing.notEllipsized
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdjustReviewScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun plural(id: Int, count: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(id, count, *args)

    private fun setScreen(
        review: ReviewUi,
        applyError: ApplyErrorUi? = null,
        onApply: () -> Unit = {},
        onKeepOriginal: () -> Unit = {},
        onFinishEarly: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                AdjustReviewScreen(
                    review = review,
                    applyError = applyError,
                    onApply = onApply,
                    onKeepOriginal = onKeepOriginal,
                    onFinishEarly = onFinishEarly
                )
            }
        }
    }

    @Test
    fun aShorterSessionLeadsWithItsHeadingPriorityAndScope() {
        setScreen(SampleAdjustmentStates.shorterReview)

        composeTestRule.onNodeWithText(string(R.string.adjust_review_time_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.your_priority)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.today_only)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.undo_available)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(plural(R.plurals.adjust_review_time_line_format, 35, 35))
            .assertIsDisplayed()
    }

    @Test
    fun theTradeoffIsNamedInFull() {
        setScreen(SampleAdjustmentStates.shorterReview)

        composeTestRule.onNodeWithText(string(R.string.tradeoff)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                string(R.string.tradeoff_less_work_format, string(R.string.region_arms))
            )
            .assert(notEllipsized())
    }

    @Test
    fun theExactChangesOpenAndListEveryRow() {
        setScreen(SampleAdjustmentStates.shorterReview)

        composeTestRule.onNodeWithText(plural(R.plurals.sets_from_to_format, 2, 3, 2))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.see_exact_changes)).performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Dumbbell Curl").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.changes_rest_unchanged)).assertIsDisplayed()
    }

    @Test
    fun applyingAndKeepingReportBack() {
        var applied = false
        var kept = false
        setScreen(
            SampleAdjustmentStates.shorterReview,
            onApply = { applied = true },
            onKeepOriginal = { kept = true }
        )

        composeTestRule.onNodeWithText(string(R.string.use_this_workout).uppercase())
            .assert(notEllipsized())
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.keep_original)).performClick()

        assertThat(applied).isTrue()
        assertThat(kept).isTrue()
    }

    // The estimator is unvalidated, so the screen shows the budget that was
    // asked for and never a guess at how long the result takes.
    @Test
    fun noDurationEstimateIsShown() {
        setScreen(SampleAdjustmentStates.shorterReview)

        composeTestRule.onNodeWithText(plural(R.plurals.adjust_review_time_line_format, 28, 28))
            .assertDoesNotExist()
    }

    @Test
    fun aSubstituteNamesTheEquipmentAndOffersWeightGuidance() {
        setScreen(SampleAdjustmentStates.substituteReview)

        composeTestRule
            .onNodeWithText(
                string(
                    R.string.adjust_review_equipment_title_format,
                    string(R.string.equipment_dumbbell).lowercase()
                )
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.restore_remaining_plan)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.how_to_choose_weight)).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aPlanThatAlreadyFitsIsLeftAlone() {
        setScreen(SampleAdjustmentStates.noChangeReview)

        composeTestRule.onNodeWithText(string(R.string.keep_current_workout)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.already_fits)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.continue_workout).uppercase())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.use_this_workout).uppercase())
            .assertDoesNotExist()
    }

    @Test
    fun anImpossibleBudgetOffersFinishingEarly() {
        var finished = false
        setScreen(SampleAdjustmentStates.infeasibleReview, onFinishEarly = { finished = true })

        composeTestRule.onNodeWithText(string(R.string.no_short_version_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.finish_early).uppercase()).performClick()

        assertThat(finished).isTrue()
    }

    // No minimum was measured for this answer, so no minimum is printed.
    @Test
    fun anUnworkableRequestNamesNoMinutes() {
        setScreen(ReviewUi.Infeasible(InfeasibleReason.INVALID_MINUTES, null))

        composeTestRule.onNodeWithText(string(R.string.no_adjustment_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(plural(R.plurals.no_short_version_body_format, 0, 0))
            .assertDoesNotExist()
    }

    @Test
    fun anApplyThatFailedSaysSoAndOffersARetry() {
        setScreen(SampleAdjustmentStates.shorterReview, applyError = ApplyErrorUi.NOT_APPLIED)

        composeTestRule.onNodeWithText(string(R.string.review_not_applied)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.retry).uppercase()).assertIsDisplayed()
    }

    @Test
    fun aRebuiltPreviewSaysTheWorkoutChanged() {
        setScreen(SampleAdjustmentStates.shorterReview, applyError = ApplyErrorUi.STALE_REBUILT)

        composeTestRule.onNodeWithText(string(R.string.review_rebuilt)).assertIsDisplayed()
    }
}
