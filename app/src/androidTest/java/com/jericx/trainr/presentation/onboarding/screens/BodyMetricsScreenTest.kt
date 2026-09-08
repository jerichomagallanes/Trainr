package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.common.Constants
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile

@RunWith(AndroidJUnit4::class)
class BodyMetricsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun theChosenUnitsTravelWithTheMeasurements() {
        var captured: UnitSystem? = null
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(
                    onNextClick = { _, _, units -> captured = units },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()
        composeTestRule.onNodeWithText(string(R.string.height_placeholder_imperial))
            .performTextInput("5'10\"")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_lbs))
            .performTextInput("165")
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(captured).isEqualTo(UnitSystem.IMPERIAL)
    }

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.your_measurements))
            .assertIsDisplayed()
    }

    @Test
    fun nextDisabledInitiallyAndEnabledAfterFillingHeightAndWeight() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }

    @Test
    fun nextEmitsParsedMetricValues() {
        var capturedHeight = 0f
        var capturedWeight = 0f

        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(
                    onNextClick = { h, w, _ -> capturedHeight = h; capturedWeight = w },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")
        composeTestRule.onNodeWithText(string(R.string.next)).performClick()

        assertThat(capturedHeight).isEqualTo(170f)
        assertThat(capturedWeight).isEqualTo(70f)
    }

    @Test
    fun togglingToImperialChangesUnitLabels() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText(string(R.string.height_ft_in)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.weight_lbs)).assertIsDisplayed()
    }

    @Test
    fun togglingUnitsConvertsAlreadyEnteredValues() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText("5'7\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("154").assertIsDisplayed()
    }

    // Switching units rewrites the field, its keyboard and its filter, so focus must not stay behind.
    @Test
    fun switchingUnitsClearsFocusFromTheFieldBeingEdited() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText("170").assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText("5'7\"").assertIsNotFocused()
    }

    @Test
    fun switchingUnitsClearsFocusFromTheWeightFieldToo() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("70")
        composeTestRule.onNodeWithText("70").assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()

        composeTestRule.onNodeWithText("154").assertIsNotFocused()
    }

    @Test
    fun reselectingTheCurrentUnitKeepsFocus() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("170")
        composeTestRule.onNodeWithText("170").assertIsFocused()

        composeTestRule.onNodeWithText(string(R.string.metric)).performClick()

        composeTestRule.onNodeWithText("170").assertIsFocused()
    }

    // The profile is stored in centimetres and kilograms whichever units were typed.
    @Test
    fun reopeningInPoundsShowsTheStoredMeasurementsConverted() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(
                    initial = UserProfile(
                        height = 177.8f,
                        weight = 69.85331f,
                        bodyUnitSystem = UnitSystem.IMPERIAL
                    ),
                    isEditing = true,
                    onNextClick = { _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("5'10\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("154").assertIsDisplayed()
    }

    @Test
    fun reopeningInKilogramsShowsThemAsTyped() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(
                    initial = UserProfile(
                        height = 175f,
                        weight = 70f,
                        bodyUnitSystem = UnitSystem.METRIC
                    ),
                    isEditing = true,
                    onNextClick = { _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("175").assertIsDisplayed()
        composeTestRule.onNodeWithText("70").assertIsDisplayed()
    }

    // The imperial filter makes the apostrophe optional, so "595" must not parse as a height.
    @Test
    fun aHeightThatIsNotFeetAndInchesCannotBeSubmitted() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()
        composeTestRule.onNodeWithText(string(R.string.height_placeholder_imperial))
            .performTextInput("595")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_lbs))
            .performTextInput("154")

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    // Name the limits, not an example: an example reads as the answer expected of the body.
    @Test
    fun aHeightOutsideTheLimitsNamesTheLimits() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("300")

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                R.string.value_range_hint,
                composeTestRule.activity.getString(R.string.height_label),
                Constants.Workout.MIN_HEIGHT_CM.toInt().toString(),
                "${Constants.Workout.MAX_HEIGHT_CM.toInt()} " +
                    composeTestRule.activity.getString(R.string.unit_cm)
            )
        ).assertIsDisplayed()
    }

    @Test
    fun properFeetAndInchesAreAccepted() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()
        composeTestRule.onNodeWithText(string(R.string.height_placeholder_imperial))
            .performTextInput("5'10\"")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_lbs))
            .performTextInput("154")

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
    }

    @Test
    fun anImplausibleWeightCannotBeSubmitted() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("175")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("2")

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    @Test
    fun noBmiIsShownForMeasurementsThatWereRefused() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("300")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("2")

        composeTestRule.onNodeWithText(string(R.string.underweight)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.bmi_label), substring = true)
            .assertDoesNotExist()
    }
}
