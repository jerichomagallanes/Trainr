package com.jericx.trainr.presentation.splash

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun displaysAppName() {
        composeTestRule.setContent {
            TrainrTheme {
                SplashScreen(versionName = "1.0-test")
            }
        }

        composeTestRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
    }

    @Test
    fun rendersVersionString() {
        composeTestRule.setContent {
            TrainrTheme {
                SplashScreen(versionName = "9.9.9-test")
            }
        }

        composeTestRule.onNodeWithText(
            string(R.string.version_format).format("9.9.9-test")
        ).assertIsDisplayed()
    }
}
