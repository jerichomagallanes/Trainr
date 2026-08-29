package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
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
            startDateMillis = weekStartMillis,
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
                            exerciseKey = "goblet_squat",
                            name = "Goblet Squats",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 12, weightKg = 20f, done = true),
                            durationMinutes = 8,
                            prescription = "3 sets of 12 reps",
                            instructions = "Squat holding a dumbbell at your chest to build the legs and brace the core.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "dumbbell_floor_press",
                            name = "Dumbbell Floor Press",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 10, weightKg = 16f, done = true),
                            durationMinutes = 8,
                            prescription = "3 sets of 10 reps",
                            instructions = "Press dumbbells from the floor to work the chest, shoulders and triceps.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "bent_over_row",
                            name = "Bent-Over Rows",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 12, weightKg = 18f, done = true),
                            durationMinutes = 8,
                            prescription = "3 sets of 12 reps",
                            instructions = "Hinge at the hips and row dumbbells to your ribs for a stronger back.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "overhead_press",
                            name = "Overhead Press",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 10, weightKg = 12f, done = true),
                            durationMinutes = 7,
                            prescription = "3 sets of 10 reps",
                            instructions = "Press dumbbells overhead to build shoulder strength and stability.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "romanian_deadlift",
                            name = "Romanian Deadlifts",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 12, weightKg = 24f, done = true),
                            durationMinutes = 8,
                            prescription = "3 sets of 12 reps",
                            instructions = "Hinge with soft knees to load the hamstrings and glutes.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "plank",
                            name = "Plank",
                            measure = ExerciseMeasure.DURATION,
                            sets = timedSets(3, seconds = 45, done = true),
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
                            exerciseKey = "warm_up_jog",
                            name = "Warm-up jog",
                            measure = ExerciseMeasure.DURATION,
                            sets = timedSets(1, seconds = 300, done = true),
                            durationMinutes = 5,
                            prescription = "5 minutes",
                            instructions = "Light jogging in place to get your heart rate up and muscles warm.",
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "high_intensity_intervals",
                            name = "High-Intensity Intervals",
                            measure = ExerciseMeasure.DURATION,
                            sets = timedSets(5, seconds = 60),
                            durationMinutes = 10,
                            prescription = "5 sets of 1 minute",
                            instructions = "Quick bursts of intense effort with short rest to boost " +
                                "cardio, burn fat, and build endurance."
                        ),
                        WorkoutExercise(
                            exerciseKey = "bicycle_crunch",
                            name = "Bicycle Crunches",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 20),
                            durationMinutes = 5,
                            prescription = "3 sets of 20 reps",
                            instructions = "Core exercise with alternating elbow-to-knee twists to target abs and obliques."
                        ),
                        WorkoutExercise(
                            exerciseKey = "russian_twist",
                            name = "Russian Twists",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 15),
                            durationMinutes = 4,
                            prescription = "3 sets of 15 reps",
                            instructions = "Seated core exercise involving torso rotation to engage abs and obliques."
                        ),
                        WorkoutExercise(
                            exerciseKey = "leg_raise",
                            name = "Leg Raises",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 12),
                            durationMinutes = 4,
                            prescription = "3 sets of 12 reps",
                            instructions = "Lying core exercise that lifts legs to strengthen lower abs and hip flexors."
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
                            exerciseKey = "jump_squat",
                            name = "Jump Squats",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(4, reps = 12),
                            durationMinutes = 10,
                            prescription = "4 sets of 12 reps",
                            instructions = "Explode upward out of a squat to build lower-body power."
                        ),
                        WorkoutExercise(
                            exerciseKey = "walking_lunge",
                            name = "Walking Lunges",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 20),
                            durationMinutes = 10,
                            prescription = "3 sets of 20 steps",
                            instructions = "Step forward into deep lunges to work quads, glutes and balance."
                        ),
                        WorkoutExercise(
                            exerciseKey = "dumbbell_step_up",
                            name = "Dumbbell Step-Ups",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 10, weightKg = 12f),
                            durationMinutes = 10,
                            prescription = "3 sets of 10 reps",
                            instructions = "Drive through the leading leg onto a step to build single-leg strength."
                        ),
                        WorkoutExercise(
                            exerciseKey = "glute_bridge",
                            name = "Glute Bridges",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 15),
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

    private fun repSets(count: Int, reps: Int, weightKg: Float? = null, done: Boolean = false) =
        (1..count).map {
            ExerciseSet(
                setNumber = it,
                targetReps = reps,
                targetWeightKg = weightKg,
                actualReps = if (done) reps else null,
                actualWeightKg = if (done) weightKg else null,
                isCompleted = done
            )
        }

    private fun timedSets(count: Int, seconds: Int, done: Boolean = false) =
        (1..count).map {
            ExerciseSet(
                setNumber = it,
                targetSeconds = seconds,
                actualSeconds = if (done) seconds else null,
                isCompleted = done
            )
        }
}
