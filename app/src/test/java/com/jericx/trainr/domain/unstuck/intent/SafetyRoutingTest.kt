package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class SafetyRoutingTest {

    @Test
    fun everyPainWordFlagsTheNote() {
        listOf(
            "my knee hurts today",
            "it hurt yesterday",
            "some pain in the shoulder",
            "painful wrist",
            "still sore from Monday",
            "I injured my back",
            "old injury acting up",
            "a sharp feeling in my elbow",
            "dull ache after the squats",
            "aching hips",
            "think I strain something",
            "might tweak my neck",
            "a twinge in the hamstring",
            "some discomfort when pressing",
            "PAIN in caps",
            "Hurts."
        ).forEach { note ->
            assertWithMessage(note).that(SafetyRouting.flagsPain(note)).isTrue()
        }
    }

    @Test
    fun onlyWholeWordsCount() {
        listOf(
            "I have 35 minutes for the whole workout today.",
            "the rack is taken",
            "painting the fence after",
            "no sharpie to log with",
            "strained relations with the gym staff",
            "spain trip next week",
            "tweaked the playlist"
        ).forEach { note ->
            assertWithMessage(note).that(SafetyRouting.flagsPain(note)).isFalse()
        }
    }
}
