package com.jericx.trainr.presentation.onboarding.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.common.Constants
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

    // iOS substitutes curly quotes as they are typed, so both apps accept them and
    // store the straight form. Typed here as the characters, not the keys.
    @Test
    fun imperialHeightAcceptsCurlyQuotesAndStoresThemStraight() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.imperial)).performClick()
        composeTestRule.onNodeWithText(string(R.string.height_placeholder_imperial))
            .performTextInput("5\u201910\u201D")

        composeTestRule.onNodeWithText("5'10\"").assertIsDisplayed()
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

    // The categories are the CDC's adult ones, which start at 20. A teenager
    // gets the measurements accepted and no label.
    @Test
    fun teenMeasurementsDoNotShowAnAdultBmi() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(age = 19, onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm))
            .performTextInput("175")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg))
            .performTextInput("72")

        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.bmi_label), substring = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("bmiSources").assertExists()
        composeTestRule.onNodeWithText(string(R.string.bmi_cdc_categories)).assertDoesNotExist()
    }

    @Test
    fun sourcesAreAvailableBeforeEnteringMeasurements() {
        composeTestRule.setContent {
            TrainrTheme(darkTheme = false) {
                BodyMetricsScreen(age = 20, onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }
        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
        composeTestRule.onNodeWithTag("bmiSources").assertIsDisplayed()
        screenshot("before-measurements")
        composeTestRule.onNodeWithTag("bmiSources").performClick()
        assertSheetSources(screenshots = true)
        composeTestRule.onNodeWithTag("bmiSourcesClose").performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    @Test
    fun theBmiSourcesAreOneTapFromTheAdultResult() {
        val opened = mutableListOf<String>()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                TrainrTheme(darkTheme = false) {
                    BodyMetricsScreen(age = 20, onNextClick = { _, _, _ -> }, onBackClick = {})
                }
            }
        }
        composeTestRule.onNodeWithText(string(R.string.height_placeholder_cm)).performTextInput("175")
        composeTestRule.onNodeWithText(string(R.string.weight_placeholder_kg)).performTextInput("72")
        composeTestRule.onNodeWithText(string(R.string.normal_weight)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.bmi_formula)).performScrollTo().assertIsDisplayed()
        for (source in listOf(BMISource.Calculation, BMISource.Categories)) {
            composeTestRule.onNodeWithText(string(source.title)).performScrollTo().performClick()
        }
        assertThat(opened).containsExactly(
            "https://www.cdc.gov/bmi/about/index.html",
            "https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html"
        ).inOrder()
        composeTestRule.onNodeWithTag("bmiSources").performScrollTo().performClick()
        assertSheetSources()
        composeTestRule.onNodeWithTag("bmiSourcesClose").performClick()
        composeTestRule.onNodeWithText("175").assertExists()
        composeTestRule.onNodeWithText("72").assertExists()
        composeTestRule.onNodeWithText(string(R.string.next)).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.bmi_cdc_categories)).performScrollTo().assertIsDisplayed()
        screenshot("result")
    }

    @Test
    fun citationsRemainReachableInDarkMode() {
        composeTestRule.setContent {
            TrainrTheme(darkTheme = true) {
                BodyMetricsScreen(age = 20, onNextClick = { _, _, _ -> }, onBackClick = {})
            }
        }
        composeTestRule.onNodeWithTag("bmiSources").performScrollTo().performClick()
        assertSheetSources(darkScreenshot = true)
        composeTestRule.onNodeWithTag("bmiSourcesClose").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(string(R.string.next)).assertIsNotEnabled()
    }

    private fun assertSheetSources(screenshots: Boolean = false, darkScreenshot: Boolean = false) {
        for (source in BMISource.entries) {
            composeTestRule.onNode(
                hasText(string(source.title)) and hasAnyAncestor(hasTestTag("bmiSourcesContent"))
            ).performScrollTo().assertIsDisplayed()
            if (screenshots) screenshot("sources-${source.name.lowercase()}")
            if (darkScreenshot && source == BMISource.Calculation) screenshot("dark-font-scale-${composeTestRule.activity.resources.configuration.fontScale}")
        }
    }

    private fun screenshot(name: String) {
        composeTestRule.waitForIdle()
        val image = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val activity = composeTestRule.activity
        // Gradle removes the test app after a successful run, including its cache.
        // MediaStore keeps these canned UI captures available for the PR evidence.
        val output = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "bmi-review-$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TrainrBMIReview")
            }
            val uri = requireNotNull(activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            requireNotNull(activity.contentResolver.openOutputStream(uri))
        } else {
            File(activity.externalCacheDir, "bmi-review-$name.png").outputStream()
        }
        output.use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    // An edit opened from the profile carries the stored age the same way.
    @Test
    fun aStoredAdultProfileShowsTheBmiWhenEditing() {
        composeTestRule.setContent {
            TrainrTheme {
                BodyMetricsScreen(
                    initial = UserProfile(
                        age = 34,
                        height = 175f,
                        weight = 72f,
                        bodyUnitSystem = UnitSystem.METRIC
                    ),
                    isEditing = true,
                    onNextClick = { _, _, _ -> },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.bmi_label), substring = true)
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("bmiSources").performScrollTo().performClick()
        assertSheetSources()
        composeTestRule.onNodeWithTag("bmiSourcesClose").performClick()
        composeTestRule.onNodeWithText("175").assertExists()
        composeTestRule.onNodeWithText("72").assertExists()
        composeTestRule.onNodeWithText(string(R.string.save).uppercase()).assertIsEnabled()
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
