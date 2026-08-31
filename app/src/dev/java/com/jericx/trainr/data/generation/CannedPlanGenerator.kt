package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise

// Development builds never call the model. The free allowance is counted per
// day and a day of building an app exhausts it long before a user would, so the
// dev flavour answers from here instead: instantly, offline, and with the same
// week every time, which is what makes a UI change legible.
//
// It is not a fixture. It reads the request the way the model is asked to, so
// what comes back has the shape the parser and the screens expect: exactly the
// number of days asked for, canonical keys the video catalog knows, and loads
// that move on from the previous week when there is one.
class CannedPlanGenerator : PlanGenerator {

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        val days = request.user.workoutDaysPerWeek.coerceIn(1, DAY_SLOTS.size)
        val previous = request.previousWeek

        return PlanGenerationResult.Generated(
            WeeklyWorkoutPlan(
                userId = request.user.id,
                weekNumber = request.weekNumber,
                title = "Sample Build",
                startDateMillis = request.startDateMillis,
                workoutDays = (0 until days).map { index ->
                    day(index, slots(days)[index], previous)
                }
            )
        )
    }

    private fun slots(days: Int): List<Int> = DAY_SLOTS[days - 1]

    private fun day(index: Int, dayNumber: Int, previous: WeeklyWorkoutPlan?) = WorkoutDay(
        dayNumber = dayNumber,
        title = DAY_TITLES[index % DAY_TITLES.size],
        duration = 45,
        exerciseCount = TEMPLATE.size,
        equipment = listOf("Dumbbells", "Yoga Mat"),
        exercises = TEMPLATE.mapIndexed { position, exercise ->
            exercise.progressedFrom(previous, dayNumber, position)
        }
    )

    // A canned week that never moved would make progression impossible to look
    // at, so loads step up the way the prompt asks the model to step them up.
    private fun WorkoutExercise.progressedFrom(
        previous: WeeklyWorkoutPlan?,
        dayNumber: Int,
        position: Int
    ): WorkoutExercise {
        val before = previous
            ?.workoutDays?.firstOrNull { it.dayNumber == dayNumber }
            ?.exercises?.getOrNull(position)
            ?: return this

        return copy(
            sets = sets.map { set ->
                val last = before.sets.firstOrNull { it.setNumber == set.setNumber }
                set.copy(
                    targetWeightKg = last?.targetWeightKg?.plus(WEIGHT_STEP_KG)
                        ?: set.targetWeightKg,
                    targetSeconds = last?.targetSeconds?.plus(SECONDS_STEP)
                        ?: set.targetSeconds
                )
            }
        )
    }

    private companion object {
        const val WEIGHT_STEP_KG = 2.5f
        const val SECONDS_STEP = 5

        // Which weekdays each plan length lands on, spacing the sessions the way
        // the prompt asks for: never two hard days back to back where it fits.
        val DAY_SLOTS = listOf(
            listOf(1),
            listOf(1, 4),
            listOf(1, 3, 5),
            listOf(1, 2, 4, 5),
            listOf(1, 2, 3, 5, 6),
            listOf(1, 2, 3, 4, 5, 6),
            listOf(1, 2, 3, 4, 5, 6, 7)
        )

        val DAY_TITLES = listOf(
            "Full Body Strength",
            "Full Body Hypertrophy",
            "Full Body Conditioning",
            "Upper Body Focus",
            "Lower Body Focus",
            "Core and Mobility",
            "Full Body Finisher"
        )

        // Keys the video catalog knows, so tutorials render in dev too.
        val TEMPLATE = listOf(
            WorkoutExercise(
                exerciseKey = "warm_up_jog",
                name = "Warm-up Jog in Place",
                measure = ExerciseMeasure.DURATION,
                durationMinutes = 5,
                prescription = "1 set of 5 minutes",
                instructions = "Jog lightly in place to raise your heart rate.",
                sets = listOf(ExerciseSet(setNumber = 1, targetSeconds = 300))
            ),
            WorkoutExercise(
                exerciseKey = "goblet_squat",
                name = "Goblet Squat",
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                durationMinutes = 10,
                prescription = "3 sets of 10 reps",
                instructions = "Hold the dumbbell at your chest and keep your torso upright.",
                sets = (1..3).map {
                    ExerciseSet(setNumber = it, targetReps = 10, targetWeightKg = 10f)
                }
            ),
            WorkoutExercise(
                exerciseKey = "dumbbell_floor_press",
                name = "Dumbbell Floor Press",
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                durationMinutes = 10,
                prescription = "3 sets of 10 reps",
                instructions = "Lie flat, knees bent, and press the dumbbells up.",
                sets = (1..3).map {
                    ExerciseSet(setNumber = it, targetReps = 10, targetWeightKg = 12f)
                }
            ),
            WorkoutExercise(
                exerciseKey = "bent_over_row",
                name = "Bent Over Row",
                measure = ExerciseMeasure.WEIGHT_AND_REPS,
                durationMinutes = 10,
                prescription = "3 sets of 12 reps",
                instructions = "Hinge at the hips and row the dumbbells to your waist.",
                sets = (1..3).map {
                    ExerciseSet(setNumber = it, targetReps = 12, targetWeightKg = 10f)
                }
            ),
            WorkoutExercise(
                exerciseKey = "plank",
                name = "Plank",
                measure = ExerciseMeasure.DURATION,
                durationMinutes = 10,
                prescription = "3 sets of 45 seconds",
                instructions = "Hold a straight line from head to heels.",
                sets = (1..3).map { ExerciseSet(setNumber = it, targetSeconds = 45) }
            )
        )
    }
}
