package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.model.YouTubeVideo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoTutorialTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setTutorial(
        isExpanded: Boolean,
        onToggle: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                VideoTutorial(
                    video = YouTubeVideo("kDPxFoCmb-w"),
                    isExpanded = isExpanded,
                    onToggle = onToggle
                )
            }
        }
    }

    @Test
    fun aCollapsedTutorialSaysOnlyThatItCanBeShown() {
        setTutorial(isExpanded = false)

        composeTestRule.onNodeWithText(string(R.string.show_video_tutorial)).assertIsDisplayed()
    }

    @Test
    fun anExpandedTutorialOffersToHideItself() {
        setTutorial(isExpanded = true)

        composeTestRule.onNodeWithText(string(R.string.hide_video_tutorial)).assertIsDisplayed()
    }

    @Test
    fun theToggleReportsItself() {
        var toggled = false
        setTutorial(isExpanded = false, onToggle = { toggled = true })

        composeTestRule.onNodeWithText(string(R.string.show_video_tutorial)).performClick()

        assertThat(toggled).isTrue()
    }


    private class FakeLifecycleOwner : LifecycleOwner {
        // createUnsafe: the test drives the state from its own thread.
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
    }

    // Load-bearing for the Play Store, not just for tidiness: a player that
    // outlives the screen keeps playing while the app is in the background,
    // which is a Device and Network Abuse suspension. The player is bound to
    // the screen's lifecycle so that leaving the app pauses it and leaving the
    // screen releases it — this pins that binding to the player's presence.
    @Test
    fun thePlayerIsBoundToTheScreenAndLetGoWithIt() {
        val owner = FakeLifecycleOwner()
        var expanded by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TrainrTheme {
                    VideoTutorial(
                        video = YouTubeVideo("kDPxFoCmb-w"),
                        isExpanded = expanded,
                        onToggle = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        val withoutPlayer = owner.lifecycle.observerCount

        expanded = true
        composeTestRule.waitForIdle()
        assertThat(owner.lifecycle.observerCount).isGreaterThan(withoutPlayer)

        expanded = false
        composeTestRule.waitForIdle()
        assertThat(owner.lifecycle.observerCount).isEqualTo(withoutPlayer)
    }
}
