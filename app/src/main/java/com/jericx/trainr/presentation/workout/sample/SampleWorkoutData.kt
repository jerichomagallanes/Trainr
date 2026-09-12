package com.jericx.trainr.presentation.workout.sample

import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import java.util.Calendar
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment

// Written in the shape a generated plan arrives in, so the screens map either
// source the same way. dayNumber is the ISO day of week.
object SampleWorkoutData {

    const val DEFAULT_DAY_NUMBER = 3

    val weekStartMillis: Long get() = dateOf(1)

    val weekEndMillis: Long get() = dateOf(7)

    // Read per call: a cached date would freeze if the default time zone changes.
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
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "dumbbell_floor_press",
                            name = "Dumbbell Floor Press",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 10, weightKg = 16f, done = true),
                            durationMinutes = 8,
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "barbell_bent_over_row",
                            name = "Bent-Over Rows",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 12, weightKg = 18f, done = true),
                            durationMinutes = 8,
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "barbell_overhead_press",
                            name = "Overhead Press",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 10, weightKg = 12f, done = true),
                            durationMinutes = 7,
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "barbell_romanian_deadlift",
                            name = "Romanian Deadlifts",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 12, weightKg = 24f, done = true),
                            durationMinutes = 8,
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "plank",
                            name = "Plank",
                            measure = ExerciseMeasure.DURATION,
                            sets = timedSets(3, seconds = 45, done = true),
                            durationMinutes = 6,
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
                    equipment = listOf("Yoga Mat"),
                    exercises = listOf(
                        WorkoutExercise(
                            exerciseKey = "warm_up",
                            name = "Warm-up jog",
                            measure = ExerciseMeasure.DURATION,
                            sets = timedSets(1, seconds = 300, done = true),
                            durationMinutes = 5,
                            isCompleted = true
                        ),
                        WorkoutExercise(
                            exerciseKey = "hiit",
                            name = "High-Intensity Intervals",
                            measure = ExerciseMeasure.DURATION,
                            sets = timedSets(5, seconds = 60),
                            durationMinutes = 10
                        ),
                        WorkoutExercise(
                            exerciseKey = "bicycle_crunch",
                            name = "Bicycle Crunches",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 20),
                            durationMinutes = 5
                        ),
                        WorkoutExercise(
                            exerciseKey = "bodyweight_russian_twist",
                            name = "Russian Twists",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 15),
                            durationMinutes = 4
                        ),
                        WorkoutExercise(
                            exerciseKey = "lying_leg_raise",
                            name = "Leg Raises",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 12),
                            durationMinutes = 4
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
                            durationMinutes = 10
                        ),
                        WorkoutExercise(
                            exerciseKey = "walking_lunge",
                            name = "Walking Lunges",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 20),
                            durationMinutes = 10
                        ),
                        WorkoutExercise(
                            exerciseKey = "dumbbell_step_up",
                            name = "Dumbbell Step-Ups",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = repSets(3, reps = 10, weightKg = 12f),
                            durationMinutes = 10
                        ),
                        WorkoutExercise(
                            exerciseKey = "glute_bridge",
                            name = "Glute Bridges",
                            measure = ExerciseMeasure.REPS,
                            sets = repSets(3, reps = 15),
                            durationMinutes = 10
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

    // Real rows, copied from the shipped catalog, so a preview shows the same
    // muscles and steps a client sees rather than placeholder prose.
    val catalog: ExerciseCatalog = InMemoryExerciseCatalog(
        listOf(
            CatalogExercise(
                key = "goblet_squat",
                name = "Goblet Squat",
                primary = MuscleGroup.QUADRICEPS,
                secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.ABDOMINALS),
                equipment = Equipment.DUMBBELL,
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                pattern = MovementPattern.SQUAT,
                staple = true,
                summary = "A squat holding one weight at your chest, which keeps you upright.",
                steps = listOf(
                    "Hold one dumbbell vertically against your chest with both hands.",
                    "Stand with your feet shoulder-width, toes slightly out.",
                    "Breathe in and squat until your elbows pass the inside of your knees."
                )
            ),
            CatalogExercise(
                key = "dumbbell_floor_press",
                name = "Floor Press (Dumbbell)",
                primary = MuscleGroup.CHEST,
                secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                equipment = Equipment.DUMBBELL,
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                pattern = MovementPattern.HORIZONTAL_PUSH,
                staple = false,
                summary = "A press from the floor. The elbows stop before the shoulder strains.",
                steps = listOf(
                    "Lie on the floor with a dumbbell in each hand and knees bent.",
                    "Hold the weights over your chest with your elbows tucked.",
                    "Breathe in and lower until your upper arms touch the floor."
                )
            ),
            CatalogExercise(
                key = "barbell_bent_over_row",
                name = "Bent Over Row (Barbell)",
                primary = MuscleGroup.UPPER_BACK,
                secondary = listOf(MuscleGroup.LATS, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                equipment = Equipment.BARBELL,
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                pattern = MovementPattern.HORIZONTAL_PULL,
                staple = true,
                summary = "The main horizontal pull. Flat back, bar to the stomach.",
                steps = listOf(
                    "Stand over the bar with feet hip-width, toes slightly out.",
                    "Hinge forward with a flat back and grip the bar overhand.",
                    "Lift the bar to arm's length and brace your abs."
                )
            ),
            CatalogExercise(
                key = "barbell_overhead_press",
                name = "Overhead Press (Barbell)",
                primary = MuscleGroup.SHOULDERS,
                secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.ABDOMINALS),
                equipment = Equipment.BARBELL,
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                pattern = MovementPattern.VERTICAL_PUSH,
                staple = true,
                summary = "The main overhead lift. Squeeze your glutes so you do not lean back.",
                steps = listOf(
                    "Set the bar on your front shoulders with hands just outside them.",
                    "Squeeze your glutes and brace your abs.",
                    "Breathe in and press the bar straight up past your face."
                )
            ),
            CatalogExercise(
                key = "barbell_romanian_deadlift",
                name = "Romanian Deadlift (Barbell)",
                primary = MuscleGroup.HAMSTRINGS,
                secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
                equipment = Equipment.BARBELL,
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                pattern = MovementPattern.HINGE,
                staple = true,
                summary = "A hinge with soft knees. Feel it in the hamstrings, not the back.",
                steps = listOf(
                    "Stand holding the bar at your thighs, feet hip-width.",
                    "Soften your knees and brace your abs.",
                    "Breathe in and push your hips back, lowering the bar down your legs."
                )
            ),
            CatalogExercise(
                key = "plank",
                name = "Plank",
                primary = MuscleGroup.ABDOMINALS,
                secondary = listOf(MuscleGroup.SHOULDERS),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.DURATION,
                pattern = MovementPattern.CORE,
                staple = true,
                summary = "A whole-body brace. Squeeze your glutes to stop your hips sagging.",
                steps = listOf(
                    "Rest on your forearms with your elbows under your shoulders.",
                    "Straighten your body from head to heels.",
                    "Squeeze your glutes and pull your belly button in."
                )
            ),
            CatalogExercise(
                key = "warm_up",
                name = "Warm Up",
                primary = MuscleGroup.FULL_BODY,
                secondary = listOf(),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.DURATION,
                pattern = MovementPattern.MOBILITY,
                staple = true,
                summary = "A whole-session activity; log the time you spend on it.",
                steps = emptyList()
            ),
            CatalogExercise(
                key = "hiit",
                name = "HIIT",
                primary = MuscleGroup.CARDIO,
                secondary = listOf(),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.DURATION,
                pattern = MovementPattern.CONDITIONING,
                staple = false,
                summary = "A whole-session activity; log the time you spend on it.",
                steps = emptyList()
            ),
            CatalogExercise(
                key = "bicycle_crunch",
                name = "Bicycle Crunch",
                primary = MuscleGroup.ABDOMINALS,
                secondary = listOf(),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.CORE,
                staple = false,
                summary = "An alternating twist crunch. Turn from the ribs, not the neck.",
                steps = listOf(
                    "Lie on your back with knees bent and feet off the floor.",
                    "Rest your fingertips behind your head.",
                    "Breathe in, then crunch your right elbow toward your left knee."
                )
            ),
            CatalogExercise(
                key = "bodyweight_russian_twist",
                name = "Russian Twist (Bodyweight)",
                primary = MuscleGroup.ABDOMINALS,
                secondary = listOf(),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.CORE,
                staple = false,
                summary = "A seated rotation. Turn from the ribs, not by swinging your arms.",
                steps = listOf(
                    "Sit with your knees bent and lean your torso back to about forty-five degrees.",
                    "Lift your feet a few inches off the floor and brace your abs.",
                    "Rotate your ribs to bring your hands beside one hip."
                )
            ),
            CatalogExercise(
                key = "lying_leg_raise",
                name = "Lying Leg Raise",
                primary = MuscleGroup.ABDOMINALS,
                secondary = listOf(),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.CORE,
                staple = false,
                summary = "A straight-leg raise. Stop the moment your lower back lifts.",
                steps = listOf(
                    "Lie on your back with your hands flat beside your hips.",
                    "Keep your legs straight and press your lower back into the floor.",
                    "Breathe out and raise your legs until they point at the ceiling."
                )
            ),
            CatalogExercise(
                key = "jump_squat",
                name = "Jump Squat",
                primary = MuscleGroup.QUADRICEPS,
                secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.SQUAT,
                staple = false,
                summary = "An explosive squat. Absorb the landing with bent knees.",
                steps = listOf(
                    "Stand with your feet shoulder-width and brace your abs.",
                    "Breathe in and squat to about halfway down.",
                    "Jump straight up, driving through your heels."
                )
            ),
            CatalogExercise(
                key = "walking_lunge",
                name = "Walking Lunge",
                primary = MuscleGroup.QUADRICEPS,
                secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.CONDITIONING,
                staple = false,
                summary = "Lunges travelling forward. Keep your torso upright throughout.",
                steps = listOf(
                    "Stand tall and step one foot forward into a long stride.",
                    "Bend both knees until your back knee nears the floor.",
                    "Drive through the front heel and step the back foot through."
                )
            ),
            CatalogExercise(
                key = "dumbbell_step_up",
                name = "Dumbbell Step Up",
                primary = MuscleGroup.QUADRICEPS,
                secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
                equipment = Equipment.DUMBBELL,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.LUNGE,
                staple = false,
                summary = "Stepping onto a box. Drive with the top leg, do not push off the floor.",
                steps = listOf(
                    "Hold a dumbbell in each hand and stand facing a box or bench.",
                    "Place one whole foot on top of it.",
                    "Drive through that heel to stand up on the box."
                )
            ),
            CatalogExercise(
                key = "glute_bridge",
                name = "Glute Bridge",
                primary = MuscleGroup.GLUTES,
                secondary = listOf(MuscleGroup.HAMSTRINGS),
                equipment = Equipment.NONE,
                measure = ExerciseMeasure.REPS,
                pattern = MovementPattern.HINGE,
                staple = true,
                summary = "The base glute movement. Finish with the hips, not the lower back.",
                steps = listOf(
                    "Lie on your back with knees bent and feet flat, close to your hips.",
                    "Squeeze your glutes and breathe in.",
                    "Drive through your heels to lift your hips into a straight line."
                )
            )
        )
    )
}
