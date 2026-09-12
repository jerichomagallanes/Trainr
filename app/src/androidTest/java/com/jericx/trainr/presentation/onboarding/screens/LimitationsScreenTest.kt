package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LimitationsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(onNextClick = {}, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.lets_keep_you_safe))
            .assertIsDisplayed()
    }

    @Test
    fun submitFiresWithDefaultsWhenNoSelectionsMade() {
        var capturedInjuries: List<Injury>? = null

        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(
                    onNextClick = { injuries -> capturedInjuries = injuries },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.submit)).performClick()

        assertThat(capturedInjuries).isEmpty()
    }

    // Chips carry the constant, never the words on them: a profile filled in
    // one language has to still read as an injury in another.
    @Test
    fun aCheckedChipComesBackAsAConstantAndNotItsLabel() {
        var capturedInjuries: List<Injury>? = null

        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(
                    onNextClick = { injuries -> capturedInjuries = injuries },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.lower_back_pain_injury)).performClick()
        composeTestRule.onNodeWithText(string(R.string.submit)).performClick()

        assertThat(capturedInjuries).containsExactly(Injury.LOWER_BACK)
    }

    // Picking None after picking an injury clears it, rather than sending both.
    @Test
    fun noneClearsWhatWasPickedBefore() {
        var capturedInjuries: List<Injury>? = null

        composeTestRule.setContent {
            TrainrTheme {
                LimitationsScreen(
                    onNextClick = { injuries -> capturedInjuries = injuries },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.knee_problems_injury))
            .performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.none_injury))
            .performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.submit)).performClick()

        assertThat(capturedInjuries).isEmpty()
    }

}
