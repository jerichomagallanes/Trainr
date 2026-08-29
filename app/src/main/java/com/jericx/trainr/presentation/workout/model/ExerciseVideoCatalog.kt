package com.jericx.trainr.presentation.workout.model

// Hand-verified tutorials keyed by exerciseKey. The generator never writes
// video URLs — a model can only invent ID-shaped strings — so the app owns
// this lookup, and an exercise with no entry simply renders no tutorial.
object ExerciseVideoCatalog {

    fun urlFor(exerciseKey: String): String? =
        videoIds[exerciseKey]?.let { "https://www.youtube.com/watch?v=${it.id}" }

    internal val videoIds = mapOf(
        "bent_over_row" to YouTubeVideo("c-gt-zzoa_A"),
        "bicycle_crunch" to YouTubeVideo("kDPxFoCmb-w"),
        "dumbbell_floor_press" to YouTubeVideo("vagdk94bFn4"),
        "dumbbell_step_up" to YouTubeVideo("DxUNi119Qzs"),
        "glute_bridge" to YouTubeVideo("wPM8icPu6H8"),
        "goblet_squat" to YouTubeVideo("6mf0oa2GGUc"),
        "high_intensity_intervals" to YouTubeVideo("WofWmk-4qU4"),
        "jump_squat" to YouTubeVideo("tZSYZdtbONc"),
        "leg_raise" to YouTubeVideo("0tzBVqiDwSs"),
        "overhead_press" to YouTubeVideo("e_f5oodNEcI"),
        "plank" to YouTubeVideo("mwlp75MS6Rg"),
        "romanian_deadlift" to YouTubeVideo("aa57T45iFSE"),
        "russian_twist" to YouTubeVideo("IJDOoVyVjhc"),
        "walking_lunge" to YouTubeVideo("vYfp2t4XgqQ"),
        "warm_up_jog" to YouTubeVideo("xmkYBO85leM")
    )
}
