package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import java.util.Calendar

// Placeholder content until plan generation exists, written in the shape a
// generated plan arrives in so the screens map it the same way either source.
// Kept in the presentation layer so it needs no Room migration. dayNumber is
// the ISO day of week.
object SampleWorkoutData {

    const val DEFAULT_DAY_NUMBER = 3

    val weekStartMillis: Long get() = dateOf(1)

    val weekEndMillis: Long get() = dateOf(7)

    // Read per call rather than cached: the default time zone can be changed
    // after this object is first touched, which would otherwise freeze a stale date.
    fun dateOf(dayNumber: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2025, Calendar.JULY, 21)
        add(Calendar.DAY_OF_YEAR, dayNumber - 1)
    }.timeInMillis

    fun dayFor(dayNumber: Int): WorkoutDay =
        weekOne.workoutDays.firstOrNull { it.dayNumber == dayNumber }
            ?: weekOne.workoutDays.first()

    val weekOne: WeeklyWorkoutPlan
        get() = WeeklyWorkoutPlan(
            id = 1,
            userId = 1,
            weekNumber = 1,
            title = "Week 1",
            workoutDays = listOf(
                WorkoutDay(
                    id = 1,
                    dayNumber = 1,
                    title = "Full Body Strength",
                    status = WorkoutStatus.COMPLETED,
                    duration = 45,
                    exerciseCount = 6,
                    equipment = listOf("Dumbbells", "Yoga Mat"),
                    exercises = listOf(
                        WorkoutExercise(
                            name = "Goblet Squats",
                            durationMinutes = 8,
                            prescription = "3 sets of 12 reps",
                            instructions = "Squat holding a dumbbell at your chest to build the legs and brace the core.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            name = "Dumbbell Floor Press",
                            durationMinutes = 8,
                            prescription = "3 sets of 10 reps",
                            instructions = "Press dumbbells from the floor to work the chest, shoulders and triceps.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            name = "Bent-Over Rows",
                            durationMinutes = 8,
                            prescription = "3 sets of 12 reps",
                            instructions = "Hinge at the hips and row dumbbells to your ribs for a stronger back.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            name = "Overhead Press",
                            durationMinutes = 7,
                            prescription = "3 sets of 10 reps",
                            instructions = "Press dumbbells overhead to build shoulder strength and stability.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            name = "Romanian Deadlifts",
                            durationMinutes = 8,
                            prescription = "3 sets of 12 reps",
                            instructions = "Hinge with soft knees to load the hamstrings and glutes.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            name = "Plank",
                            durationMinutes = 6,
                            prescription = "3 sets of 45 seconds",
                            instructions = "Hold a straight line from head to heels to brace the whole core.",
                            isCompleted = true
                        )
                    ),
                    completedAt = dateOf(1)
                ),
                WorkoutDay(
                    id = 2,
                    dayNumber = 3,
                    title = "Cardio & Core",
                    status = WorkoutStatus.IN_PROGRESS,
                    duration = 28,
                    exerciseCount = 5,
                    // Jogging, intervals and three floor exercises: a mat, nothing else.
                    equipment = listOf("Yoga Mat"),
                    exercises = listOf(
                        WorkoutExercise(
                            name = "Warm-up jog",
                            durationMinutes = 5,
                            prescription = "5 minutes",
                            instructions = "Light jogging in place to get your heart rate up and muscles warm.",
                            videoTutorialUrl = "https://www.youtube.com/watch?v=xmkYBO85leM",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            name = "High-Intensity Intervals",
                            durationMinutes = 10,
                            prescription = "5 sets of 1 minute",
                            instructions = "Quick bursts of intense effort with short rest to boost " +
                                "cardio, burn fat, and build endurance.",
                            videoTutorialUrl = "https://www.youtube.com/watch?v=WofWmk-4qU4"
                        ),
                        WorkoutExercise(
                            name = "Bicycle Crunches",
                            durationMinutes = 5,
                            prescription = "3 sets of 20 reps",
                            instructions = "Core exercise with alternating elbow-to-knee twists to target abs and obliques.",
                            videoTutorialUrl = "https://www.youtube.com/watch?v=kDPxFoCmb-w"
                        ),
                        WorkoutExercise(
                            name = "Russian Twists",
                            durationMinutes = 4,
                            prescription = "3 sets of 15 reps",
                            instructions = "Seated core exercise involving torso rotation to engage abs and obliques.",
                            videoTutorialUrl = "https://www.youtube.com/watch?v=IJDOoVyVjhc"
                        ),
                        WorkoutExercise(
                            name = "Leg Raises",
                            durationMinutes = 4,
                            prescription = "3 sets of 12 reps",
                            instructions = "Lying core exercise that lifts legs to strengthen lower abs and hip flexors.",
                            videoTutorialUrl = "https://www.youtube.com/watch?v=0tzBVqiDwSs"
                        )
                    )
                ),
                WorkoutDay(
                    id = 3,
                    dayNumber = 5,
                    title = "Lower Body Power",
                    status = WorkoutStatus.NOT_STARTED,
                    duration = 40,
                    exerciseCount = 4,
                    equipment = listOf("Dumbbells", "Yoga Mat"),
                    exercises = listOf(
                        WorkoutExercise(
                            name = "Jump Squats",
                            durationMinutes = 10,
                            prescription = "4 sets of 12 reps",
                            instructions = "Explode upward out of a squat to build lower-body power."
                        ),
                        WorkoutExercise(
                            name = "Walking Lunges",
                            durationMinutes = 10,
                            prescription = "3 sets of 20 steps",
                            instructions = "Step forward into deep lunges to work quads, glutes and balance."
                        ),
                        WorkoutExercise(
                            name = "Dumbbell Step-Ups",
                            durationMinutes = 10,
                            prescription = "3 sets of 10 reps",
                            instructions = "Drive through the leading leg onto a step to build single-leg strength."
                        ),
                        WorkoutExercise(
                            name = "Glute Bridges",
                            durationMinutes = 10,
                            prescription = "3 sets of 15 reps",
                            instructions = "Lift the hips from the floor to switch on the glutes and hamstrings."
                        )
                    )
                )
            ),
            createdAt = weekStartMillis,
            updatedAt = weekStartMillis
        )
}
