package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise

// Development builds never call the model. The free allowance is counted per
// day and a day of building an app exhausts it long before a user would, so the
// dev flavour answers from here instead: instantly, offline, and predictably,
// which is what makes a UI change legible.
//
// It is not a fixture. It reads the request the way the model is asked to, so
// what comes back has the shape the parser and the screens expect: the number
// of days asked for, a session as long as the one requested, movements the
// client owns the equipment for, canonical keys the video catalog knows, and
// loads that move on from the previous week when there is one.
//
// What it deliberately does not read is goal, workout style and injuries. Those
// change which movements a coach would pick, which is a judgement, and a canned
// answer that pretended to make it would be a worse lie than an obvious one.
class CannedPlanGenerator : PlanGenerator {

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        val user = request.user
        val days = user.workoutDaysPerWeek.coerceIn(1, DAY_SLOTS.size)
        val exercises = exercisesFor(user)

        return PlanGenerationResult.Generated(
            WeeklyWorkoutPlan(
                userId = user.id,
                weekNumber = request.weekNumber,
                title = "Sample Build",
                startDateMillis = request.startDateMillis,
                workoutDays = (0 until days).map { index ->
                    day(index, slots(days)[index], exercises, user, request.previousWeek)
                }
            )
        )
    }

    private fun slots(days: Int): List<Int> = DAY_SLOTS[days - 1]

    // Only movements the client can actually perform. A bodyweight-only profile
    // being handed goblet squats is exactly the kind of thing a dev build is
    // supposed to let you notice, so it must not be the dev build inventing it.
    private fun exercisesFor(user: UserProfile): List<WorkoutExercise> {
        val usable = POOL.filter { it.isPossibleWith(user.availableEquipment) }
        val minutes = minutesFor(user.workoutDuration, usable.size)

        return usable.take(minutes.size).mapIndexed { index, candidate ->
            candidate.exercise.copy(durationMinutes = minutes[index])
        }
    }

    // The session is as long as the one that was asked for, to the minute: the
    // day header states the requested length and the routine adds its own
    // exercises up, so the two disagreeing reads as a bug on every screen.
    private fun minutesFor(requested: Int, available: Int): List<Int> {
        val warmUp = WARM_UP_MINUTES.coerceAtMost(requested)
        val rest = requested - warmUp
        if (rest <= 0 || available <= 1) return listOf(requested)

        val count = (rest / TARGET_MINUTES_EACH).coerceIn(1, available - 1)
        val each = rest / count
        val leftOver = rest % count

        return listOf(warmUp) + (0 until count).map { each + if (it < leftOver) 1 else 0 }
    }

    private fun day(
        index: Int,
        dayNumber: Int,
        exercises: List<WorkoutExercise>,
        user: UserProfile,
        previous: WeeklyWorkoutPlan?
    ) = WorkoutDay(
        dayNumber = dayNumber,
        title = DAY_TITLES[index % DAY_TITLES.size],
        duration = exercises.sumOf { it.durationMinutes },
        exerciseCount = exercises.size,
        equipment = equipmentFor(exercises, user),
        exercises = exercises.mapIndexed { position, exercise ->
            exercise.progressedFrom(previous, dayNumber, position)
        }
    )

    // What this day needs, not everything the client owns: the card names the
    // kit to bring, and listing a squat rack for a session of planks is noise.
    private fun equipmentFor(
        exercises: List<WorkoutExercise>,
        user: UserProfile
    ): List<String> {
        val used = exercises
            .mapNotNull { exercise ->
                POOL.firstOrNull { it.exercise.exerciseKey == exercise.exerciseKey }
                    ?.needs
                    ?.firstOrNull { it in user.availableEquipment }
            }
            .distinct()
            .map { it.asText() }

        return used.ifEmpty { listOf(BODYWEIGHT) }
    }

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

    // A movement and the kit that would let you do it. An empty set is
    // bodyweight, which everybody has.
    private data class Candidate(
        val needs: Set<Equipment>,
        val exercise: WorkoutExercise
    ) {
        fun isPossibleWith(owned: List<Equipment>): Boolean =
            needs.isEmpty() || needs.any { it in owned }
    }

    private companion object {
        const val WEIGHT_STEP_KG = 2.5f
        const val SECONDS_STEP = 5
        const val WARM_UP_MINUTES = 5
        const val TARGET_MINUTES_EACH = 10
        const val BODYWEIGHT = "Bodyweight"

        fun Equipment.asText(): String = when (this) {
            Equipment.DUMBBELLS -> "Dumbbells"
            Equipment.BARBELL -> "Barbell"
            Equipment.KETTLEBELLS -> "Kettlebells"
            Equipment.BENCH -> "Bench"
            Equipment.RESISTANCE_BANDS -> "Resistance bands"
            Equipment.PULL_UP_BAR -> "Pull-up bar"
            Equipment.SQUAT_RACK -> "Squat rack"
            Equipment.CABLE_MACHINE -> "Cable machine"
            Equipment.CARDIO_MACHINES -> "Cardio equipment"
            Equipment.NONE, Equipment.OTHERS -> BODYWEIGHT
        }

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

        // Keys the video catalog knows, so tutorials render in dev too. The warm
        // up leads and the core work trails, so a session that fills only part
        // of the pool still reads like a session.
        val POOL = listOf(
            Candidate(
                needs = emptySet(),
                exercise = WorkoutExercise(
                    exerciseKey = "warm_up_jog",
                    name = "Warm-up Jog in Place",
                    measure = ExerciseMeasure.DURATION,
                    durationMinutes = WARM_UP_MINUTES,
                    prescription = "1 set of 5 minutes",
                    instructions = "Jog lightly in place to raise your heart rate.",
                    sets = listOf(ExerciseSet(setNumber = 1, targetSeconds = 300))
                )
            ),
            Candidate(
                needs = setOf(Equipment.DUMBBELLS, Equipment.KETTLEBELLS),
                exercise = WorkoutExercise(
                    exerciseKey = "goblet_squat",
                    name = "Goblet Squat",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 10 reps",
                    instructions = "Hold the weight at your chest and keep your torso upright.",
                    sets = (1..3).map {
                        ExerciseSet(setNumber = it, targetReps = 10, targetWeightKg = 10f)
                    }
                )
            ),
            Candidate(
                needs = setOf(Equipment.DUMBBELLS),
                exercise = WorkoutExercise(
                    exerciseKey = "dumbbell_floor_press",
                    name = "Dumbbell Floor Press",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 10 reps",
                    instructions = "Lie flat, knees bent, and press the dumbbells up.",
                    sets = (1..3).map {
                        ExerciseSet(setNumber = it, targetReps = 10, targetWeightKg = 12f)
                    }
                )
            ),
            Candidate(
                needs = setOf(Equipment.DUMBBELLS, Equipment.BARBELL),
                exercise = WorkoutExercise(
                    exerciseKey = "bent_over_row",
                    name = "Bent Over Row",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 12 reps",
                    instructions = "Hinge at the hips and row the weight to your waist.",
                    sets = (1..3).map {
                        ExerciseSet(setNumber = it, targetReps = 12, targetWeightKg = 10f)
                    }
                )
            ),
            Candidate(
                needs = setOf(Equipment.DUMBBELLS, Equipment.BARBELL),
                exercise = WorkoutExercise(
                    exerciseKey = "romanian_deadlift",
                    name = "Romanian Deadlift",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 10 reps",
                    instructions = "Hinge from the hips with a long spine and soft knees.",
                    sets = (1..3).map {
                        ExerciseSet(setNumber = it, targetReps = 10, targetWeightKg = 15f)
                    }
                )
            ),
            Candidate(
                needs = setOf(Equipment.DUMBBELLS, Equipment.BARBELL),
                exercise = WorkoutExercise(
                    exerciseKey = "overhead_press",
                    name = "Overhead Press",
                    measure = ExerciseMeasure.WEIGHT_AND_REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 8 reps",
                    instructions = "Press overhead without leaning back through your lower spine.",
                    sets = (1..3).map {
                        ExerciseSet(setNumber = it, targetReps = 8, targetWeightKg = 8f)
                    }
                )
            ),
            Candidate(
                needs = emptySet(),
                exercise = WorkoutExercise(
                    exerciseKey = "walking_lunge",
                    name = "Walking Lunge",
                    measure = ExerciseMeasure.REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 20 steps",
                    instructions = "Step forward and lower until both knees bend to about a right angle.",
                    sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 20) }
                )
            ),
            Candidate(
                needs = emptySet(),
                exercise = WorkoutExercise(
                    exerciseKey = "glute_bridge",
                    name = "Glute Bridge",
                    measure = ExerciseMeasure.REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 15 reps",
                    instructions = "Drive through your heels and squeeze at the top.",
                    sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 15) }
                )
            ),
            Candidate(
                needs = emptySet(),
                exercise = WorkoutExercise(
                    exerciseKey = "bicycle_crunch",
                    name = "Bicycle Crunches",
                    measure = ExerciseMeasure.REPS,
                    durationMinutes = 10,
                    prescription = "3 sets of 20 reps",
                    instructions = "Alternate elbow to opposite knee without pulling on your neck.",
                    sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 20) }
                )
            ),
            Candidate(
                needs = emptySet(),
                exercise = WorkoutExercise(
                    exerciseKey = "plank",
                    name = "Plank",
                    measure = ExerciseMeasure.DURATION,
                    durationMinutes = 10,
                    prescription = "3 sets of 45 seconds",
                    instructions = "Hold a straight line from head to heels.",
                    sets = (1..3).map { ExerciseSet(setNumber = it, targetSeconds = 45) }
                )
            )
        )
    }
}
