package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
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
class HowToSectionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private val steps = listOf(
        "Lie back and grip the bar just wider than your shoulders.",
        "Pull your shoulder blades together and plant your feet flat."
    )

    private fun setSection(isExpanded: Boolean, onToggle: () -> Unit = {}) {
        composeTestRule.setContent {
            TrainrTheme {
                HowToSection(steps = steps, isExpanded = isExpanded, onToggle = onToggle) {
                    Text("Watch tutorial")
                }
            }
        }
    }

    @Test
    fun collapsedItShowsNothingButTheWayIn() {
        setSection(isExpanded = false)

        composeTestRule.onNodeWithText(string(R.string.show_how_to_perform)).assertIsDisplayed()
        steps.forEach {
            composeTestRule.onNodeWithText(it).assertDoesNotExist()
        }
        composeTestRule.onNodeWithText("Watch tutorial").assertDoesNotExist()
    }

    // Numbered by position rather than in the data, so a reordered step never
    // leaves two step twos.
    @Test
    fun expandedItNumbersEveryStepAndCarriesTheTutorial() {
        setSection(isExpanded = true)

        steps.forEachIndexed { index, step ->
            composeTestRule.onNodeWithText(step).assertIsDisplayed()
            composeTestRule.onNodeWithText("${index + 1}").assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("Watch tutorial").assertIsDisplayed()
    }

    @Test
    fun tappingTheRowReportsTheToggle() {
        var toggled = 0
        setSection(isExpanded = false) { toggled++ }

        composeTestRule.onNodeWithText(string(R.string.show_how_to_perform)).performClick()

        assertThat(toggled).isEqualTo(1)
    }
}
