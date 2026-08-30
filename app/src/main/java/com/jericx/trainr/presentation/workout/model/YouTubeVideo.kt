package com.jericx.trainr.presentation.workout.model

@JvmInline
value class YouTubeVideo(val id: String) {

    companion object {
        // Whatever shape the model hands back: watch links, short links, embeds,
        // Shorts, with or without trailing timestamps and tracking parameters.
        private val PATTERNS = listOf(
            Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
            Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""/embed/([A-Za-z0-9_-]{11})"""),
            Regex("""/shorts/([A-Za-z0-9_-]{11})""")
        )

        fun from(url: String?): YouTubeVideo? {
            if (url.isNullOrBlank()) return null

            return PATTERNS
                .firstNotNullOfOrNull { it.find(url)?.groupValues?.get(1) }
                ?.let(::YouTubeVideo)
        }
    }
}
