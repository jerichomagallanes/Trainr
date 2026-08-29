package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseVideoCatalogTest {

    @Test
    fun everyEntryYieldsAUrlTheParserReadsBackToTheSameVideo() {
        ExerciseVideoCatalog.videoIds.forEach { (key, video) ->
            assertThat(YouTubeVideo.from(ExerciseVideoCatalog.urlFor(key))).isEqualTo(video)
        }
    }

    @Test
    fun everyKeyIsASlug() {
        ExerciseVideoCatalog.videoIds.keys.forEach {
            assertThat(it).matches("[a-z][a-z0-9_]*")
        }
    }

    @Test
    fun noTwoExercisesShareAVideo() {
        val ids = ExerciseVideoCatalog.videoIds.values

        assertThat(ids.toSet()).hasSize(ids.size)
    }

    @Test
    fun anUnknownKeyHasNoVideo() {
        assertThat(ExerciseVideoCatalog.urlFor("underwater_basket_weaving")).isNull()
    }
}
