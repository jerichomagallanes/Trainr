package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import kotlin.math.max
import kotlin.math.min

data class ProgressionRequest(
    val user: UserProfile,
    val exercise: CatalogExercise,
    val history: ExerciseHistory = ExerciseHistory.None,
    // From the skeleton. The engine may return fewer, never more.
    val sets: Int,
    // Passed in, never read from the clock: a domain object that reads the
    // clock cannot be tested and cannot be replayed.
    val nowMillis: Long = 0L,
    val deload: Boolean = false,
    // The movement touches an injury the client declared.
    val cautioned: Boolean = false
)

data class ProgressionTarget(
    val sets: List<ExerciseSet>,
    val repRange: IntRange? = null,
    // A guess the client's first session will correct, and the card says so.
    val isEstimate: Boolean = false,
    val outcome: ProgressionOutcome,
    val notes: Set<ProgressionNote> = emptySet(),
    val stallCount: Int = 0
)

enum class ProgressionOutcome {
    CALIBRATED, RESEEDED, REPEATED, HELD, LOAD_ADDED, REPS_ADDED,
    SECONDS_ADDED, REDUCED, RE_ANCHORED, RAMPED_BACK, DELOADED
}

// Asks for a different movement rather than lying about this one: the
// lightest barbell is still 20 kg, and a hold past ninety seconds is a
// different exercise, not a harder one.
enum class ProgressionNote { NEEDS_HARDER_VARIATION, LIGHTER_THAN_THE_BAR }

// Next week's targets from what the client actually did. Every rule is here as
// arithmetic because a model asked to do it in prose invents kilograms, and
// because the default tick-off logs exactly the target: progression has to key
// off hitting it, not beating it. docs/stage1-reshape.md section 2 is the table.
object ProgressionEngine {

    fun next(request: ProgressionRequest): ProgressionTarget = Week(request).run {
        when (request.exercise.measure) {
            ExerciseMeasure.WEIGHT_AND_REPS -> loaded()
            ExerciseMeasure.REPS -> bodyweight()
            ExerciseMeasure.DURATION -> timed()
        }
    }

    private class Week(private val request: ProgressionRequest) {
        private val user = request.user
        private val exercise = request.exercise
        private val units = user.weightUnits
        private val window = RepWindow.forExercise(user, exercise)
        private val askedSets = request.sets.coerceAtLeast(1)
        private val sessions = request.history.sessions
        private val usable = sessions.filter { it.isUsableFor(exercise.measure) }
        private val last = usable.firstOrNull()
        private val step = RepWindow.loadStepFraction(user, exercise)
            .let { if (request.cautioned) min(it, CAUTIOUS_STEP) else it }
        private val gapDays: Long = last?.performedAtMillis
            ?.takeIf { request.nowMillis > 0L }
            ?.let { max(0L, (request.nowMillis - it) / DAY_MILLIS) }
            ?: 0L

        fun loaded(): ProgressionTarget {
            if (sessions.isEmpty()) return calibrateLoaded()
            val done = last ?: return repeatUnperformed(sessions.first())
            val anchor = done.targetLoadKg
                ?.let { LoadStep.snap(it, exercise, units, Snap.DOWN) }
                ?: return calibrateLoaded()
            val lastReps = done.targetAmount ?: window.first

            if (request.deload) {
                return loadedTarget(lastReps, anchor, askedSets / 2, ProgressionOutcome.DELOADED, anchor)
            }
            when {
                gapDays in LONG_LAYOFF -> {
                    val seed = SeedLoad.loadKg(user, exercise, window.first) ?: anchor
                    return loadedTarget(
                        window.first, snapDown(min(seed, anchor * AFTER_LONG_LAYOFF)),
                        askedSets, ProgressionOutcome.CALIBRATED, estimate = true
                    )
                }
                gapDays in MONTH_AWAY -> return loadedTarget(
                    lastReps, snapDown(anchor * AFTER_MONTH_AWAY), askedSets - 1,
                    ProgressionOutcome.REDUCED, anchor
                )
                gapDays in FORTNIGHT_AWAY ->
                    return loadedTarget(lastReps, anchor, askedSets, ProgressionOutcome.REPEATED, anchor)
            }
            if (isRampingBack()) {
                val ramped = max(
                    LoadStep.snap(anchor * RAMP_BACK, exercise, units),
                    LoadStep.nextUp(anchor, exercise, units)
                )
                return loadedTarget(lastReps, ramped, askedSets, ProgressionOutcome.RAMPED_BACK, anchor)
            }
            val lastRange = done.targetAmountRange
            if (lastRange != null && (lastRange.last < window.first || lastRange.first > window.last)) {
                return loadedTarget(
                    window.first, reAnchor(anchor, done.minDone), askedSets,
                    ProgressionOutcome.RE_ANCHORED, estimate = true
                )
            }
            if (!done.hasQuorum) {
                return loadedTarget(lastReps, anchor, askedSets, ProgressionOutcome.REPEATED, anchor)
            }
            if (usable.size == 1 && done.minDone !in window) {
                return loadedTarget(
                    window.first, reseed(anchor, done.minDone, lastReps), askedSets,
                    ProgressionOutcome.RESEEDED, estimate = true
                )
            }
            if (done.metInFull) {
                return if (ladder(anchor) >= ladderTop(anchor)) {
                    val added = max(
                        LoadStep.snap(anchor * (1 + step), exercise, units),
                        LoadStep.nextUp(anchor, exercise, units)
                    )
                    loadedTarget(window.first, added, askedSets, ProgressionOutcome.LOAD_ADDED, anchor)
                } else {
                    loadedTarget(
                        min(lastReps + 1, window.last), anchor, askedSets, ProgressionOutcome.HELD, anchor
                    )
                }
            }
            if (done.isShortByALittle) {
                return loadedTarget(lastReps, anchor, askedSets, ProgressionOutcome.REPEATED, anchor)
            }
            return loadedTarget(
                window.first, snapDown(anchor * AFTER_STALL), askedSets,
                ProgressionOutcome.REDUCED, anchor, stall = request.history.stallCount
            )
        }

        fun bodyweight(): ProgressionTarget {
            val first = if (user.experienceLevel == ExperienceLevel.BEGINNER &&
                exercise.role == ExerciseRole.COMPOUND
            ) min(window.first, BEGINNER_BODYWEIGHT_REPS) else window.first

            if (sessions.isEmpty()) {
                return repsTarget(first, askedSets, ProgressionOutcome.CALIBRATED, estimate = true)
            }
            val done = last ?: return repsTarget(
                sessions.first().targetAmount ?: first, askedSets,
                ProgressionOutcome.REPEATED, estimate = true
            )
            val lastReps = done.targetAmount ?: first
            return when {
                request.deload -> repsTarget(lastReps, askedSets / 2, ProgressionOutcome.DELOADED)
                gapDays in LONG_LAYOFF ->
                    repsTarget(first, askedSets, ProgressionOutcome.CALIBRATED, estimate = true)
                gapDays in MONTH_AWAY -> repsTarget(lastReps, askedSets - 1, ProgressionOutcome.REDUCED)
                gapDays in FORTNIGHT_AWAY || !done.hasQuorum ->
                    repsTarget(lastReps, askedSets, ProgressionOutcome.REPEATED)
                done.metInFull && lastReps >= window.last -> repsTarget(
                    lastReps, askedSets, ProgressionOutcome.HELD,
                    notes = setOf(ProgressionNote.NEEDS_HARDER_VARIATION)
                )
                done.metInFull -> repsTarget(lastReps + 1, askedSets, ProgressionOutcome.REPS_ADDED)
                else -> repsTarget(max(first, done.minDone), askedSets, ProgressionOutcome.REDUCED)
            }
        }

        fun timed(): ProgressionTarget {
            // A warm-up that grows five seconds a week is thirteen minutes of
            // warm-up in a year.
            if (exercise.pattern == MovementPattern.MOBILITY) {
                val seconds = if (exercise.key == WARM_UP_KEY) SeedLoad.WARM_UP_SECONDS
                else SeedLoad.MOBILITY_SECONDS
                val outcome = if (sessions.isEmpty()) ProgressionOutcome.CALIBRATED
                else ProgressionOutcome.HELD
                return secondsTarget(seconds, askedSets, outcome)
            }
            val conditioning = exercise.primary == MuscleGroup.CARDIO
            val seed = if (conditioning) SeedLoad.conditioningSeconds(user) else SeedLoad.holdSeconds(user)

            if (sessions.isEmpty()) {
                return secondsTarget(seed, askedSets, ProgressionOutcome.CALIBRATED, estimate = true)
            }
            val done = last ?: return secondsTarget(
                sessions.first().targetAmount ?: seed, askedSets,
                ProgressionOutcome.REPEATED, estimate = true
            )
            val lastSeconds = done.targetAmount ?: seed
            return when {
                request.deload -> secondsTarget(lastSeconds, askedSets / 2, ProgressionOutcome.DELOADED)
                gapDays in LONG_LAYOFF ->
                    secondsTarget(seed, askedSets, ProgressionOutcome.CALIBRATED, estimate = true)
                gapDays in MONTH_AWAY ->
                    secondsTarget(lastSeconds, askedSets - 1, ProgressionOutcome.REDUCED)
                gapDays in FORTNIGHT_AWAY || !done.hasQuorum ->
                    secondsTarget(lastSeconds, askedSets, ProgressionOutcome.REPEATED)
                conditioning -> conditioning(done, lastSeconds)
                else -> hold(done, lastSeconds)
            }
        }

        // Conditioning is missed for reasons that are rarely about capacity,
        // so a short week is repeated rather than cut.
        private fun conditioning(done: LoggedSession, lastSeconds: Int): ProgressionTarget {
            if (!done.metInFull) return secondsTarget(lastSeconds, askedSets, ProgressionOutcome.REPEATED)
            val grown = roundTo(lastSeconds * CONDITIONING_GROWTH, CONDITIONING_GRANULARITY)
            val next = max(grown, lastSeconds + MIN_CONDITIONING_GAIN)
                .coerceIn(CONDITIONING_FLOOR, conditioningCeiling())
            return secondsTarget(next, askedSets, ProgressionOutcome.SECONDS_ADDED)
        }

        private fun hold(done: LoggedSession, lastSeconds: Int): ProgressionTarget {
            if (done.metInFull) {
                if (lastSeconds >= HOLD_CEILING_SECONDS) {
                    return secondsTarget(
                        lastSeconds, askedSets, ProgressionOutcome.HELD,
                        notes = setOf(ProgressionNote.NEEDS_HARDER_VARIATION)
                    )
                }
                return secondsTarget(
                    min(lastSeconds + HOLD_STEP_SECONDS, HOLD_CEILING_SECONDS), askedSets,
                    ProgressionOutcome.SECONDS_ADDED
                )
            }
            val shortTwice = usable.size >= 2 && usable.take(2).none { it.metInFull }
            return if (shortTwice) {
                secondsTarget(
                    max(lastSeconds - HOLD_CUT_SECONDS, HOLD_FLOOR_SECONDS), askedSets,
                    ProgressionOutcome.REDUCED
                )
            } else {
                secondsTarget(lastSeconds, askedSets, ProgressionOutcome.REPEATED)
            }
        }

        private fun calibrateLoaded(): ProgressionTarget {
            val raw = SeedLoad.loadKg(user, exercise, window.first)
            val lightest = LoadStep.lightest(exercise, units)
            val notes = if (exercise.equipment == Equipment.BARBELL && raw != null && raw < lightest) {
                setOf(ProgressionNote.LIGHTER_THAN_THE_BAR)
            } else {
                emptySet()
            }
            return loadedTarget(
                window.first, raw?.let { snapDown(it) }, askedSets,
                ProgressionOutcome.CALIBRATED, estimate = true, notes = notes
            )
        }

        // Prescribed and never done: there is nothing to judge, so the same
        // targets come back and stay marked as a guess.
        private fun repeatUnperformed(latest: LoggedSession): ProgressionTarget {
            val reps = latest.targetAmount ?: window.first
            val load = latest.targetLoadKg?.let { snapDown(it) }
            return loadedTarget(reps, load, askedSets, ProgressionOutcome.REPEATED, estimate = true)
        }

        // Consecutive most recent sessions at this same load that were met in
        // full: the rungs already climbed before the load moves.
        private fun ladder(anchor: Float): Int = usable.takeWhile { session ->
            session.metInFull &&
                session.targetLoadKg?.let { LoadStep.snap(it, exercise, units, Snap.DOWN) } == anchor
        }.size

        // Three rungs gives every goal a load change inside its own window. A
        // rule that waited for the top of an endurance window would leave the
        // load still for nine weeks. Where the smallest increment is a big
        // share of the load, more reps are banked before taking it.
        private fun ladderTop(anchor: Float): Int {
            val span = window.last - window.first
            return if (LoadStep.stepFractionOf(anchor, exercise, units) > COARSE_STEP) {
                min(COARSE_LADDER, span)
            } else {
                min(LADDER, span)
            }
        }

        private fun isRampingBack(): Boolean = (0..1).any { index ->
            val returned = usable.getOrNull(index)?.performedAtMillis
            val before = usable.getOrNull(index + 1)?.performedAtMillis
            returned != null && before != null && (returned - before) / DAY_MILLIS in MONTH_AWAY
        }

        private fun reAnchor(anchor: Float, minDone: Int): Float {
            val oneRepMax = anchor * (1 + min(minDone, EPLEY_CEILING) / EPLEY_DIVISOR)
            val atFirst = oneRepMax / (1 + min(window.first, EPLEY_CEILING) / EPLEY_DIVISOR)
            return snapDown(atFirst.coerceIn(anchor * 0.75f, anchor * 1.15f))
        }

        private fun reseed(anchor: Float, minDone: Int, targetReps: Int): Float {
            val scaled = anchor * (minDone + RESEED_OFFSET) / (targetReps + RESEED_OFFSET)
            return snapDown(scaled.coerceIn(anchor * 0.6f, anchor * 1.5f))
        }

        private fun snapDown(kg: Float): Float = LoadStep.snap(kg, exercise, units, Snap.DOWN)

        // No single week moves the load more than a fifth, so a garbage number
        // in history cannot become a garbage prescription. It never blocks a
        // single increment, though: on a light dumbbell one step is a quarter.
        private fun bounded(kg: Float, anchor: Float): Float {
            val high = max(anchor * (1 + MAX_WEEKLY_CHANGE), LoadStep.nextUp(anchor, exercise, units))
            val low = min(anchor * (1 - MAX_WEEKLY_CHANGE), LoadStep.nextDown(anchor, exercise, units))
            return when {
                kg > high -> LoadStep.snap(high, exercise, units, Snap.DOWN)
                kg < low -> LoadStep.snap(low, exercise, units, Snap.UP)
                else -> kg
            }
        }

        private fun loadedTarget(
            reps: Int,
            loadKg: Float?,
            sets: Int,
            outcome: ProgressionOutcome,
            anchor: Float? = null,
            estimate: Boolean = false,
            notes: Set<ProgressionNote> = emptySet(),
            stall: Int = 0
        ): ProgressionTarget {
            val weight = loadKg?.let { raw ->
                val kept = anchor?.let { bounded(raw, it) } ?: raw
                kept.coerceIn(LoadStep.lightest(exercise, units), LoadStep.ceilingKg(exercise.equipment))
                    .coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG)
            }
            return ProgressionTarget(
                sets = setNumbers(sets).map {
                    ExerciseSet(setNumber = it, targetReps = reps.coerceIn(MIN_REPS, MAX_REPS), targetWeightKg = weight)
                },
                repRange = window,
                isEstimate = estimate,
                outcome = outcome,
                notes = notes,
                stallCount = stall
            )
        }

        private fun repsTarget(
            reps: Int,
            sets: Int,
            outcome: ProgressionOutcome,
            estimate: Boolean = false,
            notes: Set<ProgressionNote> = emptySet()
        ) = ProgressionTarget(
            sets = setNumbers(sets).map {
                ExerciseSet(setNumber = it, targetReps = reps.coerceIn(MIN_REPS, MAX_REPS))
            },
            repRange = window,
            isEstimate = estimate,
            outcome = outcome,
            notes = notes
        )

        private fun secondsTarget(
            seconds: Int,
            sets: Int,
            outcome: ProgressionOutcome,
            estimate: Boolean = false,
            notes: Set<ProgressionNote> = emptySet()
        ) = ProgressionTarget(
            sets = setNumbers(sets).map {
                ExerciseSet(setNumber = it, targetSeconds = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
            },
            isEstimate = estimate,
            outcome = outcome,
            notes = notes
        )

        // Never more sets than the skeleton paid for, so nothing downstream
        // ever has to trim.
        private fun setNumbers(sets: Int): IntRange = 1..sets.coerceIn(1, askedSets)

        private fun conditioningCeiling(): Int = when (user.fitnessGoal) {
            FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> 3600
            FitnessGoal.GENERAL_FITNESS -> 2400
            else -> 1800
        }

        private fun roundTo(value: Float, granularity: Int): Int =
            (Math.round(value / granularity) * granularity)
    }

    private val LONG_LAYOFF = 43L..Long.MAX_VALUE
    private val MONTH_AWAY = 22L..42L
    private val FORTNIGHT_AWAY = 11L..21L

    private const val DAY_MILLIS = 86_400_000L
    private const val WARM_UP_KEY = "warm_up"
    private const val CAUTIOUS_STEP = 0.025f
    private const val AFTER_LONG_LAYOFF = 0.70f
    private const val AFTER_MONTH_AWAY = 0.90f
    private const val AFTER_STALL = 0.90f
    private const val RAMP_BACK = 1.05f
    private const val MAX_WEEKLY_CHANGE = 0.20f
    private const val COARSE_STEP = 0.25f
    private const val LADDER = 2
    private const val COARSE_LADDER = 5
    private const val RESEED_OFFSET = 5
    private const val EPLEY_CEILING = 12
    private const val EPLEY_DIVISOR = 30f
    private const val BEGINNER_BODYWEIGHT_REPS = 8
    private const val HOLD_STEP_SECONDS = 5
    private const val HOLD_CEILING_SECONDS = 90
    private const val HOLD_CUT_SECONDS = 10
    private const val HOLD_FLOOR_SECONDS = 20
    private const val CONDITIONING_GROWTH = 1.10f
    private const val CONDITIONING_GRANULARITY = 30
    private const val MIN_CONDITIONING_GAIN = 60
    private const val CONDITIONING_FLOOR = 300
    private const val MIN_REPS = 1
    private const val MAX_REPS = 100
    private const val MIN_SECONDS = 5
    private const val MAX_SECONDS = 5400
    private const val MIN_WEIGHT_KG = 0.5f
    private const val MAX_WEIGHT_KG = 500f
}
