package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        isPlaying: Boolean = false,
        onToggle: () -> Unit = {},
        onPlay: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TrainrTheme {
                VideoTutorial(
                    video = YouTubeVideo("kDPxFoCmb-w"),
                    isExpanded = isExpanded,
                    isPlaying = isPlaying,
                    onToggle = onToggle,
                    onPlay = onPlay
                )
            }
        }
    }

    @Test
    fun aCollapsedTutorialOffersToShowItselfAndNothingElse() {
        setTutorial(isExpanded = false)

        composeTestRule.onNodeWithText(string(R.string.show_video_tutorial)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.play_video_tutorial))
            .assertDoesNotExist()
    }

    @Test
    fun anExpandedTutorialOffersToHideItselfAndToPlay() {
        setTutorial(isExpanded = true)

        composeTestRule.onNodeWithText(string(R.string.hide_video_tutorial)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.play_video_tutorial))
            .assertIsDisplayed()
    }

    @Test
    fun theToggleReportsItself() {
        var toggled = false
        setTutorial(isExpanded = false, onToggle = { toggled = true })

        composeTestRule.onNodeWithText(string(R.string.show_video_tutorial)).performClick()

        assertThat(toggled).isTrue()
    }

    @Test
    fun tappingTheThumbnailAsksToPlay() {
        var played = false
        setTutorial(isExpanded = true, onPlay = { played = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.play_video_tutorial))
            .performClick()

        assertThat(played).isTrue()
    }

    // Once it is playing the poster is gone, so there is nothing left to tap.
    @Test
    fun aPlayingTutorialShowsNoThumbnail() {
        setTutorial(isExpanded = true, isPlaying = true)

        composeTestRule.onNodeWithContentDescription(string(R.string.play_video_tutorial))
            .assertDoesNotExist()
    }
}
