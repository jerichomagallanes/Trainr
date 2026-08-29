package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class YouTubeVideoTest {

    private fun idOf(url: String?) = YouTubeVideo.from(url)?.id

    @Test
    fun readsAWatchLink() {
        assertThat(idOf("https://www.youtube.com/watch?v=kDPxFoCmb-w")).isEqualTo("kDPxFoCmb-w")
    }

    @Test
    fun readsAShortLink() {
        assertThat(idOf("https://youtu.be/kDPxFoCmb-w")).isEqualTo("kDPxFoCmb-w")
    }

    @Test
    fun readsAnEmbedLink() {
        assertThat(idOf("https://www.youtube.com/embed/kDPxFoCmb-w")).isEqualTo("kDPxFoCmb-w")
    }

    @Test
    fun readsAShortsLink() {
        assertThat(idOf("https://www.youtube.com/shorts/hP-ol0LxLZ8")).isEqualTo("hP-ol0LxLZ8")
    }

    @Test
    fun readsAMobileLink() {
        assertThat(idOf("https://m.youtube.com/watch?v=kDPxFoCmb-w")).isEqualTo("kDPxFoCmb-w")
    }

    // The model rarely returns a bare link: expect timestamps, playlists and
    // share tracking hung off the end.
    @Test
    fun ignoresTrailingParameters() {
        assertThat(idOf("https://www.youtube.com/watch?v=kDPxFoCmb-w&t=42s")).isEqualTo("kDPxFoCmb-w")
        assertThat(idOf("https://youtu.be/kDPxFoCmb-w?si=AbC123")).isEqualTo("kDPxFoCmb-w")
        assertThat(idOf("https://www.youtube.com/watch?app=desktop&v=kDPxFoCmb-w"))
            .isEqualTo("kDPxFoCmb-w")
    }

    @Test
    fun ignoresLeadingParameters() {
        assertThat(idOf("https://www.youtube.com/watch?list=PL123&v=kDPxFoCmb-w"))
            .isEqualTo("kDPxFoCmb-w")
    }

    @Test
    fun rejectsWhatIsNotAVideoLink() {
        assertThat(idOf(null)).isNull()
        assertThat(idOf("")).isNull()
        assertThat(idOf("   ")).isNull()
        assertThat(idOf("https://example.com/some-exercise")).isNull()
        assertThat(idOf("https://www.youtube.com/@somechannel")).isNull()
    }

    // A truncated or padded id is not an id; better no video than the wrong one.
    @Test
    fun rejectsAnIdOfTheWrongLength() {
        assertThat(idOf("https://www.youtube.com/watch?v=tooShort")).isNull()
    }

    @Test
    fun buildsTheThumbnailUrlYouTubeAlwaysServes() {
        assertThat(YouTubeVideo("kDPxFoCmb-w").thumbnailUrl)
            .isEqualTo("https://img.youtube.com/vi/kDPxFoCmb-w/hqdefault.jpg")
    }
}
