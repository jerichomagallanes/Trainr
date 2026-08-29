package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.presentation.workout.model.ExerciseUi
import com.jericx.trainr.presentation.workout.model.RoutineUi

object SampleRoutine {

    const val DAY_NUMBER = 3

    val cardioAndCore: RoutineUi = RoutineUi(
        title = "Cardio & Core",
        exercises = listOf(
            ExerciseUi(
                position = 1,
                name = "Warm-up jog",
                description = "Light jogging in place to get your heart rate up and muscles warm.",
                minutes = 5,
                detail = "5 minutes",
                videoUrl = "https://www.youtube.com/watch?v=xmkYBO85leM",
                isCompleted = true
            ),
            ExerciseUi(
                position = 2,
                name = "High-Intensity Intervals",
                description = "Quick bursts of intense effort with short rest to boost cardio, " +
                    "burn fat, and build endurance.",
                minutes = 10,
                detail = "5 sets of 1 minute",
                videoUrl = "https://www.youtube.com/watch?v=WofWmk-4qU4"
            ),
            ExerciseUi(
                position = 3,
                name = "Bicycle Crunches",
                description = "Core exercise with alternating elbow-to-knee twists to target abs and obliques.",
                minutes = 5,
                detail = "3 sets of 20 reps",
                videoUrl = "https://www.youtube.com/watch?v=kDPxFoCmb-w"
            ),
            ExerciseUi(
                position = 4,
                name = "Russian Twists",
                description = "Seated core exercise involving torso rotation to engage abs and obliques.",
                minutes = 4,
                detail = "3 sets of 15 reps",
                videoUrl = "https://www.youtube.com/watch?v=IJDOoVyVjhc"
            ),
            ExerciseUi(
                position = 5,
                name = "Leg Raises",
                description = "Lying core exercise that lifts legs to strengthen lower abs and hip flexors.",
                minutes = 4,
                detail = "3 sets of 12 reps",
                videoUrl = "https://www.youtube.com/watch?v=0tzBVqiDwSs"
            )
        )
    )
}
