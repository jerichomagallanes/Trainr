package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.withoutWeekNumber
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import kotlinx.serialization.json.Json

sealed interface PlanParseResult {
    data class Parsed(val plan: WeeklyWorkoutPlan) : PlanParseResult
    data class Invalid(val errors: List<String>) : PlanParseResult
}

// Turns generator output into a WeeklyWorkoutPlan, or a list of everything
// wrong with it. The generator never writes ids, dates, week numbers or
// completion state: the app knows those, the model has no clock, so they
// arrive as parameters instead of JSON.
class GeneratedPlanParser {

    private val decoder = Json { ignoreUnknownKeys = true }

    fun parse(
        json: String,
        userId: Long,
        weekNumber: Int,
        startDateMillis: Long
    ): PlanParseResult {
        val generated = try {
            decoder.decodeFromString<GeneratedPlan>(json)
        } catch (e: IllegalArgumentException) {
            return PlanParseResult.Invalid(listOf("not a generated plan: ${e.message}"))
        }

        val errors = buildList { check(generated) }
        if (errors.isNotEmpty()) return PlanParseResult.Invalid(errors)

        return PlanParseResult.Parsed(
            WeeklyWorkoutPlan(
                userId = userId,
                weekNumber = weekNumber,
                title = generated.title.withoutWeekNumber(),
                startDateMillis = startDateMillis,
                workoutDays = generated.days.sortedBy { it.dayNumber }.map { it.toDomain() }
            )
        )
    }

    private val keyShape = Regex("[a-z][a-z0-9_]*")

    private fun MutableList<String>.check(plan: GeneratedPlan) {
        if (plan.title.isBlank()) add("plan: title is blank")
        if (plan.days.isEmpty()) add("plan: has no days")
        plan.days.groupingBy { it.dayNumber }.eachCount()
            .filterValues { it > 1 }
            .keys.forEach { add("plan: day $it appears more than once") }
        plan.days.forEach { check(it) }
    }

    private fun MutableList<String>.check(day: GeneratedDay) {
        val where = "day ${day.dayNumber}"
        if (day.dayNumber !in 1..7) add("$where: dayNumber must be 1..7, Monday to Sunday")
        if (day.title.isBlank()) add("$where: title is blank")
        if (day.exercises.isEmpty()) add("$where: has no exercises")
        day.exercises.groupingBy { it.exerciseKey }.eachCount()
            .filterValues { it > 1 }
            .keys.forEach { add("$where: exerciseKey '$it' appears more than once") }
        day.exercises.forEach { check(where, it) }
    }

    private fun MutableList<String>.check(dayWhere: String, exercise: GeneratedExercise) {
        val where = "$dayWhere, ${exercise.exerciseKey.ifBlank { "exercise" }}"
        if (!keyShape.matches(exercise.exerciseKey)) {
            add("$where: exerciseKey '${exercise.exerciseKey}' is not a lower_snake_case slug")
        }
        if (exercise.name.isBlank()) add("$where: name is blank")
        if (exercise.prescription.isBlank()) add("$where: prescription is blank")
        if (exercise.instructions.isBlank()) add("$where: instructions are blank")
        if (exercise.durationMinutes <= 0) add("$where: durationMinutes must be above zero")
        if (exercise.restSeconds != null && exercise.restSeconds <= 0) {
            add("$where: restSeconds must be above zero")
        }
        if (exercise.sets.isEmpty()) add("$where: has no sets")
        exercise.sets.forEachIndexed { index, set ->
            check("$where, set ${index + 1}", set, exercise.resolvedMeasure)
        }
    }

    private fun MutableList<String>.check(where: String, set: GeneratedSet, measure: ExerciseMeasure) {
        when (measure) {
            ExerciseMeasure.WEIGHT_AND_REPS, ExerciseMeasure.REPS ->
                if ((set.reps ?: 0) <= 0) add("$where: needs reps above zero")
            ExerciseMeasure.DURATION ->
                if ((set.seconds ?: 0) <= 0) add("$where: needs seconds above zero")
        }
        if (set.weightKg != null && set.weightKg <= 0f) add("$where: weightKg must be above zero")
    }

    // Unknown measures degrade to REPS, the same fallback the database mapper uses.
    private val GeneratedExercise.resolvedMeasure: ExerciseMeasure
        get() = runCatching { ExerciseMeasure.valueOf(measure) }
            .getOrDefault(ExerciseMeasure.REPS)

    private fun GeneratedDay.toDomain() = WorkoutDay(
        dayNumber = dayNumber,
        title = title,
        duration = exercises.sumOf { it.durationMinutes },
        exerciseCount = exercises.size,
        equipment = equipment,
        exercises = exercises.map { it.toDomain() }
    )

    private fun GeneratedExercise.toDomain(): WorkoutExercise {
        val resolved = resolvedMeasure
        return WorkoutExercise(
            exerciseKey = exerciseKey,
            name = name,
            measure = resolved,
            sets = sets.mapIndexed { index, set -> set.toDomain(index + 1, resolved) },
            setCount = sets.size,
            durationMinutes = durationMinutes,
            prescription = prescription,
            restTime = restSeconds,
            instructions = instructions
        )
    }

    // A set keeps only the targets its measure renders, so a stray weight on a
    // bodyweight exercise cannot linger invisibly in the log.
    private fun GeneratedSet.toDomain(setNumber: Int, measure: ExerciseMeasure) = when (measure) {
        ExerciseMeasure.WEIGHT_AND_REPS ->
            ExerciseSet(setNumber = setNumber, targetReps = reps, targetWeightKg = weightKg)
        ExerciseMeasure.REPS -> ExerciseSet(setNumber = setNumber, targetReps = reps)
        ExerciseMeasure.DURATION -> ExerciseSet(setNumber = setNumber, targetSeconds = seconds)
    }
}
