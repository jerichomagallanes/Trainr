package com.jericx.trainr.domain.generation

import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.ExerciseShortlist
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.catalog.PatternRequirement
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile

// The shape of a week before anything picks a movement: which days, what each
// session is for, how many movements, in what order, with how many sets and
// what rest, and which movements could fill each place. A model then only
// chooses among a handful of keys per slot, and with no model at all the top
// of each list is already a week a coach would sign.
class PlanSkeletonBuilder(private val catalog: ExerciseCatalog) {

    fun build(request: PlanRequest): PlanSkeleton {
        val user = request.user
        val owned = user.availableEquipment.toSet().ifEmpty { setOf(Equipment.NONE) }
        val pool = catalog.availableWith(owned)
            .filterNot { InjuryGuard.excludes(it, user.injuries) }
            .sortedBy { it.key }
        val required = ExerciseShortlist.requiredPatterns(pool, user.fitnessGoal)
        val lastWeek = request.previousWeek?.workoutDays.orEmpty()
            .flatMap { day -> day.exercises.map { it.exerciseKey } }
            .toSet()

        val week = Week(user, pool, lastWeek)
        val days = split(user).mapIndexed { index, (dayNumber, focus) ->
            week.wishList(dayNumber, focus, index)
        }
        val uncovered = week.deal(required, days).toMutableSet()
        val built = days.map { week.fit(it, uncovered) }

        return PlanSkeleton(
            title = titleFor(user),
            days = built,
            units = user.weightUnits,
            maxSetsPerSession = SessionBudget.maxSetsPerSession(user),
            sessionCeilingMinutes = SessionBudget.sessionCeilingMinutes(user),
            weeklySetsByRegion = week.setsByRegion(built),
            uncoveredPatterns = uncovered
        )
    }

    private fun split(user: UserProfile): List<Pair<Int, SessionFocus>> {
        val days = user.workoutDaysPerWeek.coerceIn(1, 7)
        val dayNumbers = DayNumbers.getValue(days)
        if (user.fitnessGoal == FitnessGoal.FLEXIBILITY) {
            return dayNumbers.map { it to SessionFocus.MOBILITY_FLOW }
        }
        return dayNumbers.zip(Splits.getValue(days))
    }

    private fun titleFor(user: UserProfile): String {
        val level = when (user.experienceLevel) {
            ExperienceLevel.BEGINNER -> "Beginner"
            ExperienceLevel.INTERMEDIATE -> "Intermediate"
            ExperienceLevel.ADVANCED -> "Advanced"
        }
        val aim = when (user.fitnessGoal) {
            FitnessGoal.STRENGTH -> "Strength"
            FitnessGoal.MUSCLE_GAIN -> "Muscle Building"
            FitnessGoal.GENERAL_FITNESS -> "General Fitness"
            FitnessGoal.WEIGHT_LOSS -> "Weight Loss"
            FitnessGoal.ENDURANCE -> "Endurance"
            FitnessGoal.FLEXIBILITY -> "Mobility"
        }
        return "$level $aim"
    }

    private class Draft(
        val tier: SlotTier,
        val id: String,
        var patterns: List<MovementPattern>,
        val scope: Set<MuscleGroup>,
        val minSets: Int,
        val preferredSets: Int
    ) {
        var muscles: Set<MuscleGroup> = scope
        var required: PatternRequirement? = null
        var candidates: List<String> = emptyList()
        var sets: Int = minSets
        var conditioningSeconds: Int = 0
    }

    private class DayDraft(val dayNumber: Int, val focus: SessionFocus, val slots: MutableList<Draft>)

    private inner class Week(
        private val user: UserProfile,
        private val pool: List<CatalogExercise>,
        private val lastWeek: Set<String>
    ) {
        private val shape = shapeFor(user.fitnessGoal)
        private val usesThisWeek = mutableMapOf<String, Int>()
        private val directSets = mutableMapOf<MuscleRegion, Int>()
        private val maxSets = SessionBudget.maxSetsPerSession(user)
        private val ceiling = SessionBudget.sessionCeilingMinutes(user)
        private val conditioningDays = conditioningDayIndexes(
            user.fitnessGoal, user.workoutDaysPerWeek.coerceIn(1, 7)
        )

        fun wishList(dayNumber: Int, focus: SessionFocus, index: Int): DayDraft {
            val scope = scopeOf(focus)
            val counts = mutableMapOf<SlotTier, Int>()
            val drafts = WishLists.getValue(focus).mapNotNull { tier ->
                if (tier == SlotTier.CONDITIONING && focus.isHard && index !in conditioningDays) {
                    return@mapNotNull null
                }
                val sets = shape.sets[tier] ?: if (focus.isHard) return@mapNotNull null else 1 to 1
                val instance = (counts[tier] ?: 0) + 1
                counts[tier] = instance
                Draft(
                    tier = tier,
                    id = idOf(tier, instance),
                    patterns = familyOf(focus, tier),
                    scope = if (tier == SlotTier.CORE) AllMuscles else scope,
                    minSets = sets.first,
                    preferredSets = sets.second
                ).also {
                    it.conditioningSeconds = if (shape.conditioningFillsTheSession) CONDITIONING_FLOOR_SECONDS
                    else SeedLoad.conditioningSeconds(user)
                }
            }
            return DayDraft(dayNumber, focus, drafts.toMutableList())
        }

        // Each weekly requirement goes to the earliest compound slot that can
        // actually deliver it. Push and pull live in the secondary and
        // accessory slots of a full-body day, not the primary, so dealing only
        // to primaries would leave every full-body week without a press.
        fun deal(required: Set<PatternRequirement>, days: List<DayDraft>): Set<PatternRequirement> {
            val uncovered = mutableSetOf<PatternRequirement>()
            PatternRequirement.entries.filter { it in required }.forEach { requirement ->
                val home = days.asSequence().flatMap { day -> day.slots.asSequence().map { day to it } }
                    .firstOrNull { (day, slot) ->
                        slot.tier.isCompound && slot.required == null &&
                            slot.patterns.any { requirement.isMetBy(it) && hasAny(it, scopeOf(day.focus)) }
                    }
                if (home == null) {
                    uncovered += requirement
                } else {
                    val slot = home.second
                    slot.required = requirement
                    slot.patterns = slot.patterns.filter { requirement.isMetBy(it) }
                }
            }
            return uncovered
        }

        fun fit(day: DayDraft, uncovered: MutableSet<PatternRequirement>): SkeletonDay {
            val drop = dropOrderFor(day.focus)
            trimToCount(day, drop)
            fillCandidates(day, uncovered)
            fitMinimums(day, drop)
            topUp(day)
            day.slots.forEach { slot -> slot.candidates.firstOrNull()?.let { countDirect(it, slot.sets) } }
            return SkeletonDay(
                dayNumber = day.dayNumber,
                focus = day.focus,
                slots = day.slots.sortedBy { it.tier.ordinal }.map { it.toSlot() }
            )
        }

        fun setsByRegion(days: List<SkeletonDay>): Map<MuscleRegion, Float> {
            val totals = mutableMapOf<MuscleRegion, Float>()
            days.flatMap { it.slots }.forEach { slot ->
                val top = slot.candidates.firstOrNull()?.let { catalog[it] } ?: return@forEach
                top.primary.region.takeIf { it.isTrainable }?.let { totals.merge(it, slot.sets.toFloat(), Float::plus) }
                top.secondary.map { it.region }.filter { it.isTrainable && it != top.primary.region }
                    .distinct()
                    .forEach { totals.merge(it, slot.sets * ASSIST_SHARE, Float::plus) }
            }
            return totals
        }

        private fun trimToCount(day: DayDraft, drop: List<String>) {
            val removable = drop.iterator()
            while (day.slots.size > shape.count && removable.hasNext()) {
                val id = removable.next()
                day.slots.removeAll { it.id == id && it.isDroppable }
            }
        }

        private fun fillCandidates(day: DayDraft, uncovered: MutableSet<PatternRequirement>) {
            val takenToday = mutableSetOf<String>()
            val patternsToday = mutableSetOf<MovementPattern>()
            val order = day.slots.sortedWith(compareBy({ it.fillOrder }, { it.tier.ordinal }))
            order.forEach { slot ->
                if (slot.tier == SlotTier.ISOLATION) slot.muscles = isolationMusclesFor(day, takenToday)
                // Variety is for lifting. There are five mobility movements in
                // the catalog and a flexibility day uses four, so capping their
                // weekly uses would leave the sixth day with nothing, and
                // stretching every day is simply normal.
                val eligible = pool.filter {
                    it.key !in takenToday &&
                        (slot.tier.isTimed || (usesThisWeek[it.key] ?: 0) < MAX_WEEKLY_USES)
                }
                val matched = if (slot.tier.isCompound) {
                    slot.patterns.filter { it !in patternsToday }.firstNotNullOfOrNull { pattern ->
                        eligible.filter { it.fits(slot) && it.pattern == pattern }.takeIf { it.isNotEmpty() }
                            ?.also { slot.patterns = listOf(pattern) }
                    }.orEmpty()
                } else {
                    eligible.filter { it.fits(slot) }
                }
                val sharing = order.count { it.tier == slot.tier && it.candidates.isEmpty() }
                val share = if (slot.tier == SlotTier.WARM_UP) 1
                else maxOf(1, minOf(MAX_CANDIDATES, matched.size / maxOf(1, sharing)))
                slot.candidates = matched.sortedWith(rankFor(slot)).take(share).map { it.key }
                if (slot.candidates.isEmpty()) {
                    slot.required?.let { uncovered += it }
                    return@forEach
                }
                takenToday += slot.candidates
                if (slot.tier.isCompound) patternsToday += slot.patterns
                slot.candidates.first().let { usesThisWeek.merge(it, 1, Int::plus) }
            }
            day.slots.removeAll { it.candidates.isEmpty() }
        }

        // Pass B: the minimums have to fit before anything is topped up.
        private fun fitMinimums(day: DayDraft, drop: List<String>) {
            val removable = drop.iterator()
            while (!fits(day) && removable.hasNext()) {
                val id = removable.next()
                day.slots.removeAll { it.id == id && it.isDroppable }
            }
            while (!fits(day)) {
                val shrink = day.slots.filter { it.sets > 1 }
                    .sortedWith(compareBy({ !it.tier.isCompound }, { -it.tier.ordinal }))
                    .firstOrNull() ?: break
                shrink.sets -= 1
            }
        }

        // Pass C, aimed at the session the client asked for rather than the
        // ceiling above it: every slot to its preferred sets first, then a
        // little more where a long session has room, then the rest of a
        // weight-loss session to conditioning. Extra sets go to the first
        // movements of the day, the same fatigue rule as the order itself.
        private fun topUp(day: DayDraft) {
            grow(day) { it.preferredSets }
            grow(day) { if (it.tier.isTimed && it.tier != SlotTier.MOBILITY) it.preferredSets else it.preferredSets + STRETCH_SETS }
            if (shape.conditioningFillsTheSession) fillWithConditioning(day)
        }

        private fun grow(day: DayDraft, limitFor: (Draft) -> Int) {
            var grew = true
            while (grew) {
                grew = false
                day.slots.sortedBy { it.tier.ordinal }.forEach { slot ->
                    if (slot.sets >= minOf(limitFor(slot), MAX_SETS_PER_SLOT)) return@forEach
                    slot.sets += 1
                    if (fits(day) && isWithinTheAnswer(day)) grew = true else slot.sets -= 1
                }
            }
        }

        private fun fillWithConditioning(day: DayDraft) {
            val block = day.slots.firstOrNull { it.tier == SlotTier.CONDITIONING } ?: return
            while (block.conditioningSeconds + CONDITIONING_STEP_SECONDS <= CONDITIONING_CEILING_SECONDS) {
                block.conditioningSeconds += CONDITIONING_STEP_SECONDS
                if (!fits(day) || !isWithinTheAnswer(day)) {
                    block.conditioningSeconds -= CONDITIONING_STEP_SECONDS
                    return
                }
            }
        }

        // The hard limit: never more sets than the session pays for, and
        // never past its ceiling even at the top of every rep window, so no
        // week of the ladder can break it.
        private fun fits(day: DayDraft): Boolean =
            day.slots.sumOf { it.sets } <= maxSets &&
                SessionMinutes.forDay(day.slots.map { minutesOf(it, atTop = true) }) <= ceiling

        // The aim: about as long as the answer, priced at the reps a set is
        // typically done at rather than the most it could ever ask.
        private fun isWithinTheAnswer(day: DayDraft): Boolean =
            SessionMinutes.forDay(day.slots.map { minutesOf(it, atTop = false) }) <= user.workoutDuration

        private fun minutesOf(slot: Draft, atTop: Boolean): Int {
            val top = slot.candidates.firstOrNull()?.let { catalog[it] } ?: return 0
            return if (top.measure == ExerciseMeasure.DURATION) {
                SessionMinutes.forExercise(
                    ExerciseMeasure.DURATION, List(slot.sets) { secondsFor(slot, top) }, restFor(slot)
                )
            } else {
                val window = RepWindow.forExercise(user, top)
                val reps = if (atTop) window.last else minOf(window.first + TYPICAL_REP_CLIMB, window.last)
                SessionMinutes.forExercise(top.measure, List(slot.sets) { reps }, restFor(slot), top.unilateral)
            }
        }

        private fun secondsFor(slot: Draft, top: CatalogExercise): Int = when (slot.tier) {
            SlotTier.WARM_UP ->
                if (top.key == WARM_UP_KEY) SeedLoad.WARM_UP_SECONDS else SeedLoad.MOBILITY_SECONDS
            SlotTier.MOBILITY -> SeedLoad.MOBILITY_SECONDS
            SlotTier.CONDITIONING -> slot.conditioningSeconds
            else -> HOLD_BUDGET_SECONDS
        }

        private fun restFor(slot: Draft): Int = when {
            slot.tier.isCompound -> SessionBudget.restSeconds(user.fitnessGoal, ExerciseRole.COMPOUND)
            slot.tier.isTimed -> SessionBudget.restSeconds(user.fitnessGoal, ExerciseRole.TIMED)
            else -> SessionBudget.restSeconds(user.fitnessGoal, ExerciseRole.ISOLATION)
        }

        // Isolation goes where the week has had the least direct work. Counted
        // direct only: assists would let arms look trained from rows alone
        // while calves, which nothing assists, took every slot.
        private fun isolationMusclesFor(day: DayDraft, takenToday: Set<String>): Set<MuscleGroup> {
            val scope = scopeOf(day.focus)
            val regions = pool.filter {
                it.pattern == MovementPattern.ISOLATION && it.primary in scope && it.key !in takenToday &&
                    it.primary.region.isTrainable && it.primary.region != MuscleRegion.CORE
            }.map { it.primary.region }.distinct()
            val region = regions.minWithOrNull(compareBy({ directSets[it] ?: 0 }, { it.ordinal }))
                ?: return scope
            return scope.filter { it.region == region }.toSet()
        }

        private fun rankFor(slot: Draft): Comparator<CatalogExercise> = compareBy(
            { if (slot.tier == SlotTier.WARM_UP && it.key == WARM_UP_KEY) 0 else 1 },
            { if (it.key in lastWeek) 0 else 1 },
            { if (slot.required?.isMetBy(it.pattern) == true) 0 else 1 },
            { if (it.staple) 0 else 1 },
            {
                val used = (usesThisWeek[it.key] ?: 0) > 0
                if (slot.tier.isCompound == used) 0 else 1
            },
            { directSets[it.primary.region] ?: 0 },
            { it.key }
        )

        private fun CatalogExercise.fits(slot: Draft): Boolean = when (slot.tier) {
            SlotTier.WARM_UP, SlotTier.MOBILITY -> pattern == MovementPattern.MOBILITY
            SlotTier.CONDITIONING -> primary == MuscleGroup.CARDIO && measure == ExerciseMeasure.DURATION
            SlotTier.CORE -> pattern == MovementPattern.CORE
            SlotTier.ISOLATION -> pattern == MovementPattern.ISOLATION &&
                primary in slot.muscles && measure != ExerciseMeasure.DURATION
            else -> pattern in slot.patterns && primary in slot.scope && measure != ExerciseMeasure.DURATION
        }

        private fun hasAny(pattern: MovementPattern, scope: Set<MuscleGroup>): Boolean =
            pool.any { it.pattern == pattern && it.primary in scope && it.measure != ExerciseMeasure.DURATION }

        private fun countDirect(key: String, sets: Int) {
            val region = catalog[key]?.primary?.region?.takeIf { it.isTrainable } ?: return
            directSets.merge(region, sets, Int::plus)
        }

        private fun Draft.toSlot(): SkeletonSlot {
            val top = catalog[candidates.first()]
            return SkeletonSlot(
                id = id,
                label = labelOf(tier),
                tier = tier,
                patterns = patterns,
                muscles = muscles,
                candidates = candidates,
                sets = sets,
                restSeconds = restFor(this),
                secondsPerSet = top?.takeIf { it.measure == ExerciseMeasure.DURATION }?.let { secondsFor(this, it) },
                required = required
            )
        }

        private val Draft.isDroppable: Boolean
            get() = tier != SlotTier.WARM_UP && tier != SlotTier.PRIMARY_COMPOUND && required == null

        // Compounds pick first so isolation can see what the day already
        // trains; the warm-up and cool-down draw last from what is left.
        private val Draft.fillOrder: Int
            get() = when (tier) {
                SlotTier.PRIMARY_COMPOUND, SlotTier.SECONDARY_COMPOUND, SlotTier.ACCESSORY -> 0
                SlotTier.WARM_UP -> 1
                SlotTier.ISOLATION, SlotTier.CORE, SlotTier.CONDITIONING -> 2
                SlotTier.MOBILITY -> 3
            }

        private fun dropOrderFor(focus: SessionFocus): List<String> =
            if (focus == SessionFocus.ACTIVE_RECOVERY) listOf("mobility_2", "core") else shape.drop
    }

    private data class Shape(
        val count: Int,
        val sets: Map<SlotTier, Pair<Int, Int>>,
        val drop: List<String>,
        // Weight loss and endurance take the rest of the session as
        // conditioning; every other goal takes a short fixed block.
        val conditioningFillsTheSession: Boolean = false
    )

    // A strength day sheds breadth to keep depth, a weight-loss day sheds the
    // lifting tail to keep its conditioning, and a flexibility day has no
    // compound slots at all.
    private fun shapeFor(goal: FitnessGoal): Shape = when (goal) {
        FitnessGoal.STRENGTH -> Shape(
            count = 6,
            sets = mapOf(
                SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (3 to 5),
                SlotTier.SECONDARY_COMPOUND to (2 to 4), SlotTier.ACCESSORY to (2 to 3),
                SlotTier.ISOLATION to (2 to 2), SlotTier.CORE to (1 to 2), SlotTier.CONDITIONING to (1 to 1)
            ),
            drop = listOf("isolation_2", "conditioning", "mobility_1", "accessory", "core", "isolation_1"),
        )
        FitnessGoal.MUSCLE_GAIN -> Shape(
            count = 8,
            sets = mapOf(
                SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (2 to 4),
                SlotTier.SECONDARY_COMPOUND to (2 to 3), SlotTier.ACCESSORY to (2 to 3),
                SlotTier.ISOLATION to (2 to 3), SlotTier.CORE to (1 to 3),
                SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (1 to 1)
            ),
            drop = listOf("mobility_1", "conditioning", "isolation_2", "core", "accessory", "isolation_1"),
        )
        FitnessGoal.GENERAL_FITNESS -> Shape(
            count = 8,
            sets = mapOf(
                SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (2 to 3),
                SlotTier.SECONDARY_COMPOUND to (2 to 3), SlotTier.ACCESSORY to (2 to 3),
                SlotTier.ISOLATION to (2 to 3), SlotTier.CORE to (1 to 3),
                SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (1 to 1)
            ),
            drop = listOf("mobility_1", "isolation_2", "isolation_1", "core", "conditioning", "accessory"),
        )
        FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> Shape(
            count = 7,
            sets = mapOf(
                SlotTier.WARM_UP to (1 to 1), SlotTier.PRIMARY_COMPOUND to (2 to 3),
                SlotTier.SECONDARY_COMPOUND to (2 to 3), SlotTier.ACCESSORY to (2 to 3),
                SlotTier.ISOLATION to (2 to 2), SlotTier.CORE to (2 to 3),
                SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (1 to 1)
            ),
            drop = listOf("isolation_2", "isolation_1", "accessory", "mobility_1", "secondary", "core"),
            conditioningFillsTheSession = true
        )
        FitnessGoal.FLEXIBILITY -> Shape(
            count = 6,
            sets = mapOf(
                SlotTier.WARM_UP to (1 to 1), SlotTier.CORE to (1 to 2),
                SlotTier.CONDITIONING to (1 to 1), SlotTier.MOBILITY to (3 to 4)
            ),
            drop = listOf("core", "conditioning", "mobility_4", "mobility_3"),
        )
    }

    // How many of the week's days carry a conditioning block, spread across
    // the week rather than bunched at its start. A weight-loss week needs its
    // minutes every day; a strength week needs them once and short, since
    // conditioning next to heavy legs blunts the lifting (Wilson 2012).
    private fun conditioningDayIndexes(goal: FitnessGoal, days: Int): Set<Int> {
        val wanted = when (goal) {
            FitnessGoal.STRENGTH, FitnessGoal.FLEXIBILITY -> 1
            FitnessGoal.MUSCLE_GAIN -> 2
            FitnessGoal.GENERAL_FITNESS -> 3
            FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE -> days
        }.coerceAtMost(days)
        return (0 until wanted).map { it * days / wanted }.toSet()
    }

    companion object {
        const val MAX_CANDIDATES = 8

        private const val MAX_WEEKLY_USES = 3
        private const val MAX_SETS_PER_SLOT = 10
        private const val HOLD_BUDGET_SECONDS = 90
        private const val STRETCH_SETS = 2
        private const val TYPICAL_REP_CLIMB = 2
        private const val CONDITIONING_FLOOR_SECONDS = 300
        private const val CONDITIONING_STEP_SECONDS = 60
        private const val CONDITIONING_CEILING_SECONDS = 3600
        private const val ASSIST_SHARE = 0.5f
        private const val WARM_UP_KEY = "warm_up"

        private val DayNumbers = mapOf(
            1 to listOf(1), 2 to listOf(1, 4), 3 to listOf(1, 3, 5), 4 to listOf(1, 2, 4, 5),
            // Five hard days never three in a row inside the week.
            5 to listOf(1, 2, 4, 5, 7), 6 to listOf(1, 2, 3, 4, 5, 6), 7 to (1..7).toList()
        )

        private val Splits = mapOf(
            1 to listOf(SessionFocus.FULL_BODY),
            2 to List(2) { SessionFocus.FULL_BODY },
            3 to List(3) { SessionFocus.FULL_BODY },
            4 to listOf(SessionFocus.UPPER, SessionFocus.LOWER, SessionFocus.UPPER, SessionFocus.LOWER),
            5 to listOf(
                SessionFocus.PUSH, SessionFocus.PULL, SessionFocus.LEGS, SessionFocus.UPPER, SessionFocus.LOWER
            ),
            6 to listOf(
                SessionFocus.PUSH, SessionFocus.PULL, SessionFocus.LEGS,
                SessionFocus.PUSH, SessionFocus.PULL, SessionFocus.LEGS
            ),
            7 to listOf(
                SessionFocus.PUSH, SessionFocus.PULL, SessionFocus.LEGS, SessionFocus.ACTIVE_RECOVERY,
                SessionFocus.UPPER, SessionFocus.LOWER, SessionFocus.ACTIVE_RECOVERY
            )
        )

        private val Hard = listOf(
            SlotTier.WARM_UP, SlotTier.PRIMARY_COMPOUND, SlotTier.SECONDARY_COMPOUND, SlotTier.ACCESSORY,
            SlotTier.ISOLATION, SlotTier.ISOLATION, SlotTier.CORE, SlotTier.CONDITIONING, SlotTier.MOBILITY
        )

        private val WishLists: Map<SessionFocus, List<SlotTier>> = SessionFocus.entries.associateWith { focus ->
            when (focus) {
                SessionFocus.FULL_BODY -> Hard - SlotTier.ISOLATION
                SessionFocus.ACTIVE_RECOVERY -> listOf(
                    SlotTier.WARM_UP, SlotTier.CORE, SlotTier.CONDITIONING, SlotTier.MOBILITY, SlotTier.MOBILITY
                )
                SessionFocus.MOBILITY_FLOW -> listOf(
                    SlotTier.WARM_UP, SlotTier.CORE, SlotTier.MOBILITY, SlotTier.MOBILITY, SlotTier.MOBILITY,
                    SlotTier.CONDITIONING
                )
                else -> Hard
            }
        }

        private val AllMuscles = MuscleGroup.entries.toSet()

        private fun scopeOf(focus: SessionFocus): Set<MuscleGroup> = when (focus) {
            SessionFocus.UPPER -> setOf(
                MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.TRAPS,
                MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS
            )
            SessionFocus.LOWER, SessionFocus.LEGS -> setOf(
                MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
                MuscleGroup.ABDUCTORS, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES
            )
            SessionFocus.PUSH -> setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
            SessionFocus.PULL -> setOf(
                MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.TRAPS, MuscleGroup.BICEPS,
                MuscleGroup.FOREARMS, MuscleGroup.SHOULDERS
            )
            else -> AllMuscles
        }

        // Preference-ordered, walked until one has movements; a day never
        // repeats a pattern across its compound slots.
        private fun familyOf(focus: SessionFocus, tier: SlotTier): List<MovementPattern> {
            if (!tier.isCompound) return emptyList()
            val families = when (focus) {
                SessionFocus.FULL_BODY -> listOf(
                    listOf(MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE),
                    listOf(MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH),
                    listOf(MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL)
                )
                SessionFocus.UPPER -> listOf(
                    listOf(MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH),
                    listOf(MovementPattern.VERTICAL_PULL, MovementPattern.HORIZONTAL_PULL),
                    listOf(MovementPattern.VERTICAL_PUSH, MovementPattern.HORIZONTAL_PULL, MovementPattern.HORIZONTAL_PUSH)
                )
                SessionFocus.LOWER -> listOf(
                    listOf(MovementPattern.SQUAT, MovementPattern.HINGE),
                    listOf(MovementPattern.HINGE, MovementPattern.SQUAT),
                    listOf(MovementPattern.LUNGE, MovementPattern.SQUAT, MovementPattern.HINGE)
                )
                SessionFocus.PUSH -> listOf(
                    listOf(MovementPattern.HORIZONTAL_PUSH),
                    listOf(MovementPattern.VERTICAL_PUSH),
                    listOf(MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH)
                )
                SessionFocus.PULL -> listOf(
                    listOf(MovementPattern.VERTICAL_PULL, MovementPattern.HORIZONTAL_PULL),
                    listOf(MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL),
                    listOf(MovementPattern.HINGE, MovementPattern.HORIZONTAL_PULL)
                )
                SessionFocus.LEGS -> listOf(
                    listOf(MovementPattern.SQUAT),
                    listOf(MovementPattern.HINGE),
                    listOf(MovementPattern.LUNGE, MovementPattern.SQUAT, MovementPattern.HINGE)
                )
                else -> return emptyList()
            }
            return families[tier.ordinal - SlotTier.PRIMARY_COMPOUND.ordinal]
        }

        private fun idOf(tier: SlotTier, instance: Int): String = when (tier) {
            SlotTier.WARM_UP -> "warm_up"
            SlotTier.PRIMARY_COMPOUND -> "primary"
            SlotTier.SECONDARY_COMPOUND -> "secondary"
            SlotTier.ACCESSORY -> "accessory"
            SlotTier.ISOLATION -> "isolation_$instance"
            SlotTier.CORE -> "core"
            SlotTier.CONDITIONING -> "conditioning"
            SlotTier.MOBILITY -> "mobility_$instance"
        }

        private fun labelOf(tier: SlotTier): String = when (tier) {
            SlotTier.WARM_UP -> "the warm-up"
            SlotTier.PRIMARY_COMPOUND -> "the main lift"
            SlotTier.SECONDARY_COMPOUND -> "the second lift"
            SlotTier.ACCESSORY -> "the accessory lift"
            SlotTier.ISOLATION -> "an isolation movement"
            SlotTier.CORE -> "the core movement"
            SlotTier.CONDITIONING -> "the conditioning"
            SlotTier.MOBILITY -> "the cool-down"
        }
    }
}
