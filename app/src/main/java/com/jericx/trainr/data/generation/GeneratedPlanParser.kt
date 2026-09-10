package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.PatternRequirement
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.withoutWeekNumber
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import kotlin.math.ceil
import kotlinx.serialization.json.Json

// What the client's own answers make possible, so a plan that cannot be
// performed is rejected while the model still has an attempt left to fix it.
data class PlanLimits(
    val maxSetsPerSession: Int,
    // Empty means the vocabulary is not being enforced, which is only true in
    // tests: a real request always has a shortlist.
    val allowedKeys: Set<String> = emptySet(),
    val requiredPatterns: Set<PatternRequirement> = emptySet(),
    val languageCode: String = "en"
) {
    companion object {
        val Unbounded = PlanLimits(maxSetsPerSession = Int.MAX_VALUE)
    }
}

sealed interface PlanParseResult {
    data class Parsed(val plan: WeeklyWorkoutPlan) : PlanParseResult
    data class Invalid(val errors: List<String>) : PlanParseResult
}

// The generator never writes ids, dates, week numbers or completion state, so
// those arrive as parameters rather than JSON.
class GeneratedPlanParser(private val catalog: ExerciseCatalog = InMemoryExerciseCatalog(emptyList())) {

    private val decoder = Json { ignoreUnknownKeys = true }

    fun parse(
        json: String,
        userId: Long,
        weekNumber: Int,
        startDateMillis: Long,
        limits: PlanLimits = PlanLimits.Unbounded
    ): PlanParseResult {
        val generated = try {
            decoder.decodeFromString<GeneratedPlan>(json)
        } catch (e: IllegalArgumentException) {
            return PlanParseResult.Invalid(listOf("not a generated plan: ${e.message}"))
        }

        val errors = buildList { check(generated, limits) }
        if (errors.isNotEmpty()) return PlanParseResult.Invalid(errors)

        return PlanParseResult.Parsed(
            WeeklyWorkoutPlan(
                userId = userId,
                weekNumber = weekNumber,
                title = generated.title.withoutWeekNumber(),
                startDateMillis = startDateMillis,
                workoutDays = generated.days
                    .sortedBy { it.dayNumber }
                    .map { it.toDomain(limits.languageCode) }
            )
        )
    }

    private val keyShape = Regex("[a-z][a-z0-9_]*")

    private fun MutableList<String>.check(plan: GeneratedPlan, limits: PlanLimits) {
        if (plan.title.isBlank()) add("plan: title is blank")
        if (plan.days.isEmpty()) add("plan: has no days")
        plan.days.groupingBy { it.dayNumber }.eachCount()
            .filterValues { it > 1 }
            .keys.forEach { add("plan: day $it appears more than once") }
        plan.days.forEach { check(it, limits) }
        checkPatterns(plan, limits)
    }

    // The three patterns that earn the most for the time they take. Checked
    // across the week rather than the day, because a split spreads them.
    private fun MutableList<String>.checkPatterns(plan: GeneratedPlan, limits: PlanLimits) {
        if (limits.requiredPatterns.isEmpty()) return
        val patterns = plan.days
            .flatMap { it.exercises }
            .mapNotNull { catalog[it.exerciseKey]?.pattern }
        limits.requiredPatterns
            .filterNot { requirement -> patterns.any(requirement::isMetBy) }
            .forEach { add("plan: the week has no ${it.label}, and needs one") }
    }

    private fun MutableList<String>.check(day: GeneratedDay, limits: PlanLimits) {
        val where = "day ${day.dayNumber}"
        if (day.dayNumber !in 1..7) add("$where: dayNumber must be 1..7, Monday to Sunday")
        if (day.title.isBlank()) add("$where: title is blank")
        if (day.exercises.isEmpty()) add("$where: has no exercises")
        if (day.exercises.size > MAX_EXERCISES_PER_DAY) {
            add("$where: has ${day.exercises.size} exercises, more than $MAX_EXERCISES_PER_DAY")
        }
        val sets = day.exercises.sumOf { it.sets.size }
        if (sets > limits.maxSetsPerSession) {
            add(
                "$where: has $sets sets but the client's session length allows at most " +
                    "${limits.maxSetsPerSession}, warm-up included"
            )
        }
        day.exercises.groupingBy { it.exerciseKey }.eachCount()
            .filterValues { it > 1 }
            .keys.forEach { add("$where: exerciseKey '$it' appears more than once") }
        day.exercises.forEach { check(where, it, limits) }
    }

    private fun MutableList<String>.check(
        dayWhere: String,
        exercise: GeneratedExercise,
        limits: PlanLimits
    ) {
        val where = "$dayWhere, ${exercise.exerciseKey.ifBlank { "exercise" }}"
        if (!keyShape.matches(exercise.exerciseKey)) {
            add("$where: exerciseKey '${exercise.exerciseKey}' is not a lower_snake_case slug")
        }
        if (limits.allowedKeys.isNotEmpty() && exercise.exerciseKey !in limits.allowedKeys) {
            add(
                "$where: '${exercise.exerciseKey}' is not one of the movements offered; " +
                    "choose only from that list"
            )
        }
        if (catalog[exercise.exerciseKey] == null && limits.allowedKeys.isNotEmpty()) {
            add("$where: '${exercise.exerciseKey}' is not a movement the app knows")
        }
        if (exercise.prescription.isBlank()) add("$where: prescription is blank")
        if (exercise.instructions.isBlank()) add("$where: instructions are blank")
        if (exercise.restSeconds != null && exercise.restSeconds !in MIN_REST..MAX_REST) {
            add("$where: restSeconds must be $MIN_REST..$MAX_REST")
        }
        if (exercise.sets.isEmpty()) add("$where: has no sets")
        if (exercise.sets.size > MAX_SETS_PER_EXERCISE) {
            add("$where: has ${exercise.sets.size} sets, more than $MAX_SETS_PER_EXERCISE")
        }
        exercise.sets.forEachIndexed { index, set ->
            check("$where, set ${index + 1}", set, exercise.resolvedMeasure)
        }
    }

    // Bounds, not tastes: a number outside these is one no client could
    // perform, and it costs less to ask again than to show it to them.
    private fun MutableList<String>.check(where: String, set: GeneratedSet, measure: ExerciseMeasure) {
        when (measure) {
            ExerciseMeasure.WEIGHT_AND_REPS, ExerciseMeasure.REPS ->
                if ((set.reps ?: 0) !in MIN_REPS..MAX_REPS) {
                    add("$where: needs reps between $MIN_REPS and $MAX_REPS")
                }
            ExerciseMeasure.DURATION ->
                if ((set.seconds ?: 0) !in MIN_SECONDS..MAX_SECONDS) {
                    add("$where: needs seconds between $MIN_SECONDS and $MAX_SECONDS")
                }
        }
        if (set.weightKg != null && set.weightKg !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            add("$where: weightKg must be between $MIN_WEIGHT_KG and $MAX_WEIGHT_KG")
        }
    }

    // How long the exercise takes is arithmetic on what was prescribed, not a
    // fourth number for the model to keep in agreement with the other three.
    // A rep is about three seconds at the moderate velocity ACSM asks for.
    private val GeneratedExercise.minutes: Int
        get() {
            val perSet = when (resolvedMeasure) {
                ExerciseMeasure.DURATION -> sets.map { it.seconds ?: 0 }
                else -> sets.map { (it.reps ?: 0) * SECONDS_PER_REP }
            }
            val rest = (restSeconds ?: 0) * (sets.size - 1).coerceAtLeast(0)
            return ceil((perSet.sum() + rest) / 60.0).toInt().coerceAtLeast(1)
        }

    // How a movement is measured is a property of the movement, so the catalog
    // answers it. A key the catalog does not know degrades to REPS, the same
    // fallback the database mapper uses.
    private val GeneratedExercise.resolvedMeasure: ExerciseMeasure
        get() = catalog[exerciseKey]?.measure ?: ExerciseMeasure.REPS

    private fun Equipment.asDisplayText() = name.lowercase()
        .split('_')
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

    // The day's kit is the union of what its movements need, which the
    // catalog already knows; asking a model to restate it only gave it a way
    // to name equipment the client does not own.
    private fun GeneratedDay.toDomain(languageCode: String) = WorkoutDay(
        dayNumber = dayNumber,
        title = title,
        duration = exercises.sumOf { it.minutes },
        exerciseCount = exercises.size,
        equipment = exercises
            .mapNotNull { catalog[it.exerciseKey] }
            .flatMap { it.requires }
            .filterNot { it == Equipment.NONE }
            .distinct()
            .map { it.asDisplayText() },
        exercises = exercises.map { it.toDomain(languageCode) }
    )

    private fun GeneratedExercise.toDomain(languageCode: String): WorkoutExercise {
        val resolved = resolvedMeasure
        return WorkoutExercise(
            exerciseKey = exerciseKey,
            name = catalog[exerciseKey]?.displayName(languageCode) ?: exerciseKey,
            measure = resolved,
            sets = sets.mapIndexed { index, set -> set.toDomain(index + 1, resolved) },
            setCount = sets.size,
            durationMinutes = minutes,
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

    private companion object {
        const val SECONDS_PER_REP = 3
        const val MAX_EXERCISES_PER_DAY = 12
        const val MAX_SETS_PER_EXERCISE = 10
        const val MIN_REPS = 1
        const val MAX_REPS = 100
        const val MIN_SECONDS = 5
        const val MAX_SECONDS = 5_400
        const val MIN_REST = 5
        const val MAX_REST = 600
        const val MIN_WEIGHT_KG = 0.5f
        const val MAX_WEIGHT_KG = 500f
    }
}
