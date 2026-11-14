package com.jericx.trainr.presentation.workout.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VideoCueTest {

    private val cued = mutableListOf<String>()
    private val cue = VideoCue()

    private fun playerReady() = cue.playerReady { cued += it }

    @Test
    fun aClipAskedForBeforeThePlayerExistsIsCuedOnceItDoes() {
        cue.show("first")

        assertThat(cued).isEmpty()

        playerReady()

        assertThat(cued).containsExactly("first")
    }

    @Test
    fun aDifferentClipIsCuedOnThePlayerAlreadyThere() {
        cue.show("first")
        playerReady()

        cue.show("second")

        assertThat(cued).containsExactly("first", "second").inOrder()
    }

    @Test
    fun theSameClipIsNotCuedAgainWhenTheScreenRecomposes() {
        cue.show("first")
        playerReady()

        cue.show("first")
        cue.show("first")

        assertThat(cued).containsExactly("first")
    }

    @Test
    fun aReplacementPlayerIsGivenTheClipTheScreenIsShowing() {
        cue.show("first")
        playerReady()
        cue.show("second")
        cue.playerGone()

        playerReady()

        assertThat(cued).containsExactly("first", "second", "second").inOrder()
    }

    @Test
    fun aPlayerThatIsGoneIsNotCuedAgain() {
        playerReady()
        cue.playerGone()

        cue.show("first")

        assertThat(cued).isEmpty()
    }
}
