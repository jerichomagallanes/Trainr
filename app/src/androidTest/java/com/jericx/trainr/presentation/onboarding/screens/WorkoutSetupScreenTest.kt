package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.set_up_your_workout))
            .assertIsDisplayed()
    }

    // "90 mins" once rendered as "90..." inside its fixed-width chip: the
    // laid-out text must never visually overflow.
    @Test
    fun everyDurationChipShowsItsWholeLabel() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        val notEllipsized = SemanticsMatcher("label is not ellipsized") { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action?.invoke(results)
            results.firstOrNull()?.isLineEllipsized(0) == false
        }

        listOf(30, 45, 60, 90).forEach { minutes ->
            composeTestRule.onNodeWithText(
                composeTestRule.activity.resources
                    .getQuantityString(R.plurals.minutes, minutes, minutes)
            ).performScrollTo().assert(notEllipsized)
        }
    }

    @Test
    fun displaysLocationOptions() {
        composeTestRule.setContent {
            TrainrTheme {
                WorkoutSetupScreen(
                    onNextClick = { _, _, _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.home)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.gym)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.both)).assertIsDisplayed()
    }
}
