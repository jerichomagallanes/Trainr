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

    val fullBodyStrength: RoutineUi = RoutineUi(
        title = "Full Body Strength",
        exercises = listOf(
            ExerciseUi(
                position = 1,
                name = "Goblet Squats",
                description = "Squat holding a dumbbell at your chest to build the legs and brace the core.",
                minutes = 8,
                detail = "3 sets of 12 reps",
                isCompleted = true
            ),
            ExerciseUi(
                position = 2,
                name = "Dumbbell Floor Press",
                description = "Press dumbbells from the floor to work the chest, shoulders and triceps.",
                minutes = 8,
                detail = "3 sets of 10 reps",
                isCompleted = true
            ),
            ExerciseUi(
                position = 3,
                name = "Bent-Over Rows",
                description = "Hinge at the hips and row dumbbells to your ribs for a stronger back.",
                minutes = 8,
                detail = "3 sets of 12 reps",
                isCompleted = true
            ),
            ExerciseUi(
                position = 4,
                name = "Overhead Press",
                description = "Press dumbbells overhead to build shoulder strength and stability.",
                minutes = 7,
                detail = "3 sets of 10 reps",
                isCompleted = true
            ),
            ExerciseUi(
                position = 5,
                name = "Romanian Deadlifts",
                description = "Hinge with soft knees to load the hamstrings and glutes.",
                minutes = 8,
                detail = "3 sets of 12 reps",
                isCompleted = true
            ),
            ExerciseUi(
                position = 6,
                name = "Plank",
                description = "Hold a straight line from head to heels to brace the whole core.",
                minutes = 6,
                detail = "3 sets of 45 seconds",
                isCompleted = true
            )
        )
    )

    val lowerBodyPower: RoutineUi = RoutineUi(
        title = "Lower Body Power",
        exercises = listOf(
            ExerciseUi(
                position = 1,
                name = "Jump Squats",
                description = "Explode upward out of a squat to build lower-body power.",
                minutes = 10,
                detail = "4 sets of 12 reps"
            ),
            ExerciseUi(
                position = 2,
                name = "Walking Lunges",
                description = "Step forward into deep lunges to work quads, glutes and balance.",
                minutes = 10,
                detail = "3 sets of 20 steps"
            ),
            ExerciseUi(
                position = 3,
                name = "Dumbbell Step-Ups",
                description = "Drive through the leading leg onto a step to build single-leg strength.",
                minutes = 10,
                detail = "3 sets of 10 reps"
            ),
            ExerciseUi(
                position = 4,
                name = "Glute Bridges",
                description = "Lift the hips from the floor to switch on the glutes and hamstrings.",
                minutes = 10,
                detail = "3 sets of 15 reps"
            )
        )
    )

    // Video tutorials are only written for Cardio & Core; the rest arrive with
    // the generated routines, and the card simply omits the section without one.
    fun forDay(dayNumber: Int): RoutineUi = when (dayNumber) {
        1 -> fullBodyStrength
        DAY_NUMBER -> cardioAndCore
        else -> lowerBodyPower
    }
}
