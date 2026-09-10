> **Two corrections applied after verifying against the repo.** The design agents raised two
> findings that turned out to be wrong as stated:
>
> 1. *"The repos are not in lockstep, iOS is four commits behind."* False — the agents compared
>    against `main`, not the feature branches both platforms are on. The branches carry the same
>    work. The one real thing inside this claim: iOS `Equipment.loaded` was missing `.plate`,
>    so a plates-only client was never asked what their weights are marked in. Fixed separately.
>    **C0 below is void.**
> 2. *"Tick-off writes `actualReps = targetReps`, so 'progress when reps exceed target' is
>    unreachable."* Overstated. `loggedAsPrescribed()` uses `actualReps ?: targetReps` — a typed
>    number is kept. What is true, and what the progression rules must respect, is that the
>    **default** tick-off records exactly the target, so progression keys off *hitting* the
>    target rather than exceeding it. Exceeding remains a valid bonus signal for clients who log.

---

# Trainr Stage 1 — the reshape, one implementation document

Read with `docs/on-device-generation.md` §4 and §5. This supersedes the five subsystem specs wherever they disagree with it.

## 0. Reconciliation — where the specs fought, what won

| Fight | Taken | Why |
| --- | --- | --- |
| `PlanSkeleton` shape ×4 | parser-and-validators' `PlanSkeleton`/`SkeletonDay`/`SkeletonSlot` + `PlanSkeletonBuilder` | Only one with a builder taking a `PlanRequest`, and it carries the wire names the schema needs. |
| Slot tier enum ×3 | 8-member `SlotTier` including `MOBILITY` | Flexibility weeks are unrepresentable without it; parser-and-validators' 7-member list cannot hold its own §3.1 table. |
| `intensity` in the contract | **Deleted entirely** | RIR is gone, so it moved nothing but rest; it cost ~38% of the schema and the app clamped it. Removing it kills four contradictions at once. |
| RIR overlay | Dropped (progression-engine) | `ExerciseSet` has no effort field. Verified. |
| Who owns sets vs reps vs load | Skeleton owns **sets and rest**; engine owns **reps, seconds and load** | One sentence, kills the trim loop and the three set tables. |
| Rep windows ×3 | One `RepWindow`, with session-skeleton's beginner-strength widening | The widening is the only one with a defensible reason and a named test. |
| Isolation rest ×3 | ¾ of compound, rounded to 15, floor 30 | Reproduces the existing prompt's "90–120 multi-joint, 60–90 isolation" exactly. |
| Load snapping ×3 | progression-engine's `LoadStep` (display-unit, base offset, kettlebell rungs) | Kilogram-space rounding puts 22.5 kg on a barbell. Critique agreed. |
| `WeightUnit.loadable` ×3 | Signature unchanged; **`RoutineMapper` stops calling it** | One snapper, at generation. The `POUND_STEP` conflict then evaporates. |
| Seed table ×2 | progression-engine's, **plus** a muscle-group override table | Critique 3 is right that `(equipment, pattern)` is off by 3–5× on calves and lateral raises. |
| `GeneratedPlan` fate ×4 | **Kept, demoted** to the expander's output | Keeps one validation gate over our own new arithmetic; keeps the minutes round-trip test possible. |
| DB version ×3 | **Stays at 3. No entity change.** | Release hardening is live; a destructive wipe for two unused columns is not payable. |
| Estimate label storage | Derived: `previousSets.isEmpty()` | No column, no migration, and semantically exact. |
| Injury handling ×3 | `InjuryGuard`: a small hard deny-list **plus** a soft caution, with required patterns computed **after** filtering | Critique 1 proved a hard-filter-only design produces an unsatisfiable week for wrist+shoulder+bodyweight. |
| Deterministic tier name ×3 | `TemplatePlanGenerator` + `FallbackPlanGenerator`, `PlanSource{COACH,PROGRESSED,TEMPLATE}` | The three-value enum is the only one that can describe a zero-model week 2. |
| Plan title | App templates it | Model's version reliably needed `- Week 2` stripping. |
| Prescription chip ×3 | prescription-and-copy's derived `Prescription` value, **not stored** | A stored chip goes stale the moment a set is added. |
| `unilateral` review | Hand-reviewed pinned list (prescription-and-copy) | fallback's name-match test would flag walking lunge, which alternates within the set. |
| Load-change cadence | **New**: 3-step rep ladder, not full-window creep | Critique 3 proved full-window creep gives an endurance client one load change every 9–11 weeks. |
| Deload trigger 3 | **New**: weeks since last reduction, not "6 weeks of rises" | Uniform ladders make "6 consecutive weeks of rises" unreachable. |
| Repos in lockstep | **False.** iOS is 4 commits behind | Verified against both git logs. C0 exists to fix it. |

---

## 1. Settled contracts

### 1.1 Catalog — `domain/catalog/ExerciseCatalog.kt` (changed)

```kotlin
data class CatalogExercise(
    val key: String,
    val name: String,
    val primary: MuscleGroup,
    val secondary: List<MuscleGroup>,
    val equipment: Equipment,
    val measure: ExerciseMeasure,
    val pattern: MovementPattern,
    val staple: Boolean,
    val summary: String = "",
    val steps: List<String> = emptyList(),
    // Reps are performed on one side and repeated on the other, so the set
    // costs twice the time and the chip says so. A walking lunge alternates
    // inside the set and is not one.
    val unilateral: Boolean = false,
    // One implement held, not a pair. weightKg is the weight of that one
    // implement either way; this says whether the client is holding two.
    val oneHanded: Boolean = false
)
```

`ExerciseCatalogFile.CatalogEntry` gains both with the same defaults. `exercise-catalog.json` gains them in **both repos** (the iOS copy at `Trainr/Resources/exercise-catalog.json` is a separate file — verified, md5-identical today but not shared).

Derived, in the same file:

```kotlin
enum class ExerciseRole { COMPOUND, ISOLATION, TIMED }

val CatalogExercise.role: ExerciseRole get() = when {
    measure == ExerciseMeasure.DURATION -> ExerciseRole.TIMED
    pattern == MovementPattern.ISOLATION || pattern == MovementPattern.CORE -> ExerciseRole.ISOLATION
    pattern == MovementPattern.CONDITIONING || pattern == MovementPattern.MOBILITY -> ExerciseRole.TIMED
    else -> ExerciseRole.COMPOUND
}

val CatalogExercise.isLowerBody: Boolean
    get() = primary.region in setOf(MuscleRegion.QUADS, MuscleRegion.HAMSTRINGS,
                                    MuscleRegion.HIPS, MuscleRegion.CALVES)

val CatalogExercise.isLoadable: Boolean get() = measure == ExerciseMeasure.WEIGHT_AND_REPS
```

`isLoadable` is a property of the **measure**, not the equipment. That is what keeps the five `assisted_*` keys (all `MACHINE`, all `REPS` — verified) out of the load rules.

### 1.2 `domain/generation/SessionSkeleton.kt` (new)

```kotlin
// Session order. Whatever is trained first gains most (Nunes 2021), so the
// tier a slot sits at is a fatigue rule, not a presentation choice.
enum class SlotTier {
    WARM_UP, PRIMARY_COMPOUND, SECONDARY_COMPOUND, ACCESSORY,
    ISOLATION, CORE, CONDITIONING, MOBILITY;

    val isCompound: Boolean
        get() = this == PRIMARY_COMPOUND || this == SECONDARY_COMPOUND || this == ACCESSORY
}

enum class SessionFocus { FULL_BODY, UPPER, LOWER, PUSH, PULL, LEGS, ACTIVE_RECOVERY, MOBILITY_FLOW }

data class SkeletonSlot(
    // Schema property name and wire id: "primary_squat", "accessory_shoulders".
    val id: String,
    // For error strings: "the squat in the primary slot".
    val label: String,
    val tier: SlotTier,
    val patterns: List<MovementPattern>,
    val muscles: Set<MuscleGroup>,
    // Ranked, disjoint within the day, never empty, at most MAX_CANDIDATES.
    val candidates: List<String>,
    // The skeleton owns the set count and the rest. The engine may return
    // fewer sets, never more, and never touches rest.
    val sets: Int,
    val restSeconds: Int
) {
    val isDecided: Boolean get() = candidates.size == 1
}

data class SkeletonDay(
    val id: String,          // "day1".."day7", always == "day$dayNumber"
    val dayNumber: Int,      // 1..7, offset from the week's start day
    val focus: SessionFocus,
    val fallbackTitle: String,
    val slots: List<SkeletonSlot>  // tier order, non-decreasing
) {
    val setCount: Int get() = slots.sumOf { it.sets }
    val openSlots: List<SkeletonSlot> get() = slots.filterNot { it.isDecided }
}

data class PlanSkeleton(
    val title: String,                 // templated from goal x experience
    val days: List<SkeletonDay>,
    val units: UnitSystem,
    val maxSetsPerSession: Int,
    val sessionCeilingMinutes: Int,
    // What the week actually buys per region, counted the way SessionBudget
    // counts. Not what was targeted.
    val weeklySetsByRegion: Map<MuscleRegion, Float>,
    // At one day a week, two of the three patterns have nowhere to go. Named
    // so the caller stops asking for what the week cannot hold.
    val uncoveredPatterns: Set<PatternRequirement>
) {
    val allowedKeys: Set<String> get() = days.flatMap { d -> d.slots.flatMap { it.candidates } }.toSet()
}
```

### 1.3 `domain/generation/PlanSkeletonBuilder.kt` (new)

```kotlin
class PlanSkeletonBuilder(private val catalog: ExerciseCatalog) {
    fun build(request: PlanRequest): PlanSkeleton
    companion object { const val MAX_CANDIDATES = 8 }
}
```

**Invariants — `require()` plus a test, never a runtime branch:**

1. Every day has 3..8 slots.
2. Every slot's candidate list is non-empty.
3. Candidate lists within a day are pairwise **disjoint**, by construction: each slot draws from the day's pool and removes what it takes.
4. Slot ids are unique within a day and `lower_snake_case`.
5. Slots are emitted in non-decreasing tier order.
6. The week's slots cover `uncoveredPatterns`' complement.
7. `day.setCount <= maxSetsPerSession`, and `SessionMinutes.forDay(day) <= sessionCeilingMinutes`.
8. Every candidate resolves in the catalog and survives `InjuryGuard.excludes`.

**Split.** Goal never changes the split except `FLEXIBILITY`, which replaces it.

| Days | dayNumbers | Focus |
| --- | --- | --- |
| 1 | 1 | FULL_BODY |
| 2 | 1, 4 | FULL_BODY ×2 |
| 3 | 1, 3, 5 | FULL_BODY ×3 |
| 4 | 1, 2, 4, 5 | UPPER, LOWER, UPPER, LOWER |
| 5 | 1, 2, 3, 5, 6 | PUSH, PULL, LEGS, UPPER, LOWER |
| 6 | 1, 2, 3, 4, 5, 6 | PUSH, PULL, LEGS, PUSH, PULL, LEGS |
| 7 | 1..7 | PUSH, PULL, LEGS, ACTIVE_RECOVERY, UPPER, LOWER, ACTIVE_RECOVERY |

`FitnessGoal.FLEXIBILITY` → every day is `MOBILITY_FLOW`, placed on the same weekdays. Spacing rule, and the test that guards it: **no three consecutive hard days below 6 days per week.** (session-skeleton's "no two adjacent" test contradicted its own table — corrected.)

**Wish lists.**

| Focus | Slots, tier order |
| --- | --- |
| FULL_BODY | WARM_UP, PRIMARY, SECONDARY, ACCESSORY, ISOLATION, CORE, CONDITIONING, MOBILITY |
| UPPER/LOWER/PUSH/PULL/LEGS | WARM_UP, PRIMARY, SECONDARY, ACCESSORY, ISOLATION#1, ISOLATION#2, CORE, CONDITIONING, MOBILITY |
| ACTIVE_RECOVERY | WARM_UP, CORE, CONDITIONING, MOBILITY#1, MOBILITY#2 |
| MOBILITY_FLOW | WARM_UP, CORE, MOBILITY#1, MOBILITY#2, MOBILITY#3, CONDITIONING |

**Pattern families per compound slot** (preference-ordered; walked until non-empty; a day never repeats a pattern across compound slots):

| Focus | PRIMARY | SECONDARY | ACCESSORY |
| --- | --- | --- | --- |
| FULL_BODY | SQUAT, HINGE, LUNGE | HORIZONTAL_PUSH, VERTICAL_PUSH | HORIZONTAL_PULL, VERTICAL_PULL |
| UPPER | HORIZONTAL_PUSH, VERTICAL_PUSH | VERTICAL_PULL, HORIZONTAL_PULL | VERTICAL_PUSH, HORIZONTAL_PULL, HORIZONTAL_PUSH |
| LOWER | SQUAT, HINGE | HINGE, SQUAT | LUNGE, SQUAT, HINGE |
| PUSH | HORIZONTAL_PUSH | VERTICAL_PUSH | HORIZONTAL_PUSH, VERTICAL_PUSH |
| PULL | VERTICAL_PULL, HORIZONTAL_PULL | HORIZONTAL_PULL, VERTICAL_PULL | HINGE, HORIZONTAL_PULL |
| LEGS | SQUAT | HINGE | LUNGE, SQUAT, HINGE |

ISOLATION → `[ISOLATION]`; CORE → `[CORE]`; MOBILITY/WARM_UP → `[MOBILITY]`; CONDITIONING → `primary == CARDIO && measure == DURATION`.

That last scoping is deliberate and load-bearing: it keeps `dumbbell_walking_lunge` (primary QUADRICEPS) out of the conditioning slot, and it keeps the 35 non-DURATION `CONDITIONING`-pattern movements — every Olympic lift in the catalog — out of the plan entirely, since no compound family names `CONDITIONING`. This app should not prescribe a snatch off a bodyweight-derived seed.

**Muscle scope** — FULL_BODY/ACTIVE_RECOVERY/MOBILITY_FLOW: everything. UPPER: CHEST, LATS, UPPER_BACK, TRAPS, SHOULDERS, BICEPS, TRICEPS, FOREARMS. LOWER/LEGS: QUADRICEPS, HAMSTRINGS, GLUTES, ABDUCTORS, ADDUCTORS, CALVES. PUSH: CHEST, SHOULDERS, TRICEPS. PULL: LATS, UPPER_BACK, TRAPS, BICEPS, FOREARMS, SHOULDERS. CORE slots ignore scope.

**Fitting, three passes.** A: drop in the goal's drop order until `slots.size <= goalExerciseCount`. B: if `sum(minSets) > maxSetsPerSession`, keep dropping; then reduce compound slots to 1 set in reverse tier order. C: top up in tier order toward `preferredSets`, stopping at `maxSetsPerSession`, at `sessionCeilingMinutes`, or at 10 sets in one slot. `WARM_UP`, `PRIMARY_COMPOUND` and any slot carrying a required pattern are in no drop order.

**Required patterns, dealt at week level.** `ExerciseShortlist.requiredPatterns` is computed from the **injury-filtered** pool, so it can never ask for what was removed. Each requirement is assigned to the earliest unassigned `PRIMARY_COMPOUND` slot whose family can satisfy it; that slot's family narrows to the satisfying patterns and becomes undroppable. Unassignable requirements land in `uncoveredPatterns`.

**Region-deficit pass** decides which region each ISOLATION slot serves, but counts **direct sets only** (1 per set for the primary region, assists ignored). Assist-weighted counting is right for reporting and wrong as an allocation objective — it is what makes ARMS project to 9.0 from assists alone and never win a slot while CALVES takes two (verified: 122 catalog movements list an arm as secondary, 8 list calves). `weeklySetsByRegion` still reports assist-weighted, matching `SessionBudget`.

### 1.4 `domain/generation/SlotCandidates.kt` (new)

```kotlin
data class SelectionContext(
    val user: UserProfile,
    val owned: Set<Equipment>,
    val lastWeekBySlotId: Map<String, String>,
    val takenToday: Set<String>,
    val usesThisWeek: Map<String, Int>
)

object SlotCandidates {
    fun rank(slot: SkeletonSlot, pool: List<CatalogExercise>, context: SelectionContext): List<String>
}
```

**Hard filters:** `isAvailableWith(owned)`; `!InjuryGuard.excludes(...)`; pattern ∈ slot family **or** primary ∈ slot muscles; measure matches the slot's prescription kind; `key !in takenToday`; `usesThisWeek[key] < 3`; at most 2 movements per `MovementPattern` and per `primary` in a day.

**Ranking, lexicographic** (never a weighted score, so a failing test names one key): 1 last week's key for this slot id; 2 satisfies a still-unmet required pattern; 3 `staple`; 4 for compound tiers prefer a key already used this week, for every other tier prefer a fresh one; 5 fewest direct sets accumulated for its primary region; 6 key ascending. Then `take(MAX_CANDIDATES)`.

### 1.5 `domain/catalog/InjuryGuard.kt` (new)

There is no injury handling in code today — verified, `Injury` reaches only `PlanPromptBuilder` prose. This is the app's first authored contraindication list, so it is deliberately two-tiered.

```kotlin
enum class InjuryCaution { LOWER_BACK, KNEE, SHOULDER, WRIST, ANKLE, HIP, NECK }

object InjuryGuard {
    // Hard: the movement is never offered.
    fun excludes(exercise: CatalogExercise, injuries: Set<Injury>): Boolean
    // Soft: the movement is offered with a line on the card.
    fun cautionFor(exercise: CatalogExercise, injuries: List<Injury>): InjuryCaution?
}
```

**Hard exclusions** — narrow, named, and every entry traceable to a clause already in today's system instruction:

| Injury | Excluded |
| --- | --- |
| LOWER_BACK | `primary == LOWER_BACK`; `pattern == HINGE` with `equipment in {BARBELL, PLATE}`; loaded spinal flexion: `weighted_sit_up`, `weighted_crunch`, `weighted_decline_crunch`, `weighted_russian_twist` |
| KNEE | `pattern == LUNGE`; `key` in the plyometric list; `pistol_squat`, `assisted_pistol_squats` |
| SHOULDER | `pattern == VERTICAL_PUSH`; `triceps_dip`, `ring_dips`, `floor_triceps_dip`, `assisted_chest_dip`, `assisted_triceps_dip`; upright rows |
| WRIST | `ab_wheel`, `handstand_push_up`, `front_squat`, `clap_push_ups`, `one_arm_push_up` |
| ANKLE | plyometrics; `running`, `jump_rope`, `treadmill`, `stair_climber` |
| HIP | `pattern == LUNGE` with `equipment != NONE`; barbell hinges from the floor |
| NECK | `primary == NECK`; `*_shrug` |

Every deny-listed key is asserted to exist by `ExerciseCatalogIntegrityTest` — the fallback spec's list named `russian_twist` and `decline_sit_up`, neither of which exists, so lower-back clients would have kept loaded flexion silently.

**Soft cautions** by `MovementPattern`: LOWER_BACK → HINGE, SQUAT, CARRY, CORE. KNEE → SQUAT, LUNGE. SHOULDER → VERTICAL_PUSH, HORIZONTAL_PUSH, VERTICAL_PULL. WRIST → HORIZONTAL_PUSH, VERTICAL_PUSH, CARRY. ANKLE → SQUAT, LUNGE, CONDITIONING. HIP → HINGE, SQUAT, LUNGE. NECK → CORE, VERTICAL_PUSH. First declared injury that matches wins; one caution per card.

**Safety valve, and it is a test:** all seven injuries plus bodyweight only must still produce a valid week of ≥3 exercises a day. It passes only because WRIST and SHOULDER are deny-lists rather than pattern sweeps — a pattern sweep removes every bodyweight press in the catalog (verified: all 12 are push-up or `VERTICAL_PUSH` variants) and makes `UPPER_PUSH` unsatisfiable.

### 1.6 `domain/generation/RepWindow.kt` (new) — the single window and rest owner

```kotlin
object RepWindow {
    fun forExercise(user: UserProfile, exercise: CatalogExercise): IntRange
    fun holdSeconds(user: UserProfile): IntRange
    fun loadStepFraction(user: UserProfile, exercise: CatalogExercise): Float
}
```

| Goal | compound | isolation |
| --- | --- | --- |
| STRENGTH | 3–6 | 6–10 |
| MUSCLE_GAIN | 6–10 | 8–15 |
| GENERAL_FITNESS | 8–12 | 10–15 |
| WEIGHT_LOSS, ENDURANCE | 12–20 | 15–25 |
| FLEXIBILITY | timed only | timed only |

Modifiers: `experienceLevel == BEGINNER && goal == STRENGTH` reads the GENERAL_FITNESS row. `age >= 65` or a cautioned region: +2 on both bounds. `age < 18`: `first = max(first, 8)`. Both cap `p` at 0.025.

`loadStepFraction` (the `p` of rule R9): lower-body compound 0.050, upper-body compound 0.035, isolation 0.025.

### 1.7 `domain/generation/SessionBudget.kt` (changed)

- `FLOOR_SETS`, `WORK_SECONDS_PER_SET`, `OVERHEAD_MINUTES`, `REGION_SETS_PER_SET_HALVES` become `internal const val` (they are read by the skeleton and its tests today; they are `private` — verified).
- New: `fun restSeconds(goal: FitnessGoal, role: ExerciseRole): Int` — `COMPOUND` returns today's value unchanged; `ISOLATION` returns `(compound * 3 / 4)` rounded down to a multiple of 15, floored at 30 (STRENGTH 135, MUSCLE_GAIN 90, GENERAL_FITNESS 60, WL/END 45, FLEXIBILITY 30); `TIMED` returns 30.
- `maxSetsPerSession` keeps using the compound value, so the cap stays conservative and no existing test rebaselines.
- `WORK_SECONDS_PER_SET = 40` stays. It errs toward shorter, finishable sessions, and the transition charge below absorbs most of the gap.

### 1.8 `domain/generation/SessionMinutes.kt` (new)

Extracted from `GeneratedPlanParser`'s private `minutes` getter, which the parser then calls, so the skeleton's budgeting and the parser's ceiling check are literally the same function.

```kotlin
object SessionMinutes {
    const val SECONDS_PER_REP = 3
    // Walking to the next station, changing the pin, finding the bench.
    const val TRANSITION_SECONDS = 60

    fun forExercise(
        measure: ExerciseMeasure,
        perSet: List<Int>,        // reps, or seconds for DURATION
        restSeconds: Int,
        unilateral: Boolean
    ): Int

    fun forDay(exercises: List<WorkoutExercise>): Int
}
```

`forExercise` = `ceil(((work) + rest * (sets - 1)) / 60)`, minimum 1, where work doubles for `unilateral` rep-measured movements. This is what `WorkoutExercise.durationMinutes` stores — its meaning is unchanged except for the unilateral doubling.

`forDay` = `sum(durationMinutes) + (exercises.size - 1) * TRANSITION_SECONDS / 60`. This is what `WorkoutDay.duration` stores and what the ceiling is checked against. That resolves session-skeleton's contradiction: the per-exercise number keeps its meaning, the day number gains the transitions, and one function still owns both.

### 1.9 `domain/generation/LoadStep.kt` (new)

```kotlin
enum class Snap { NEAREST, DOWN, UP }

object LoadStep {
    fun snap(kg: Float, exercise: CatalogExercise, units: UnitSystem, how: Snap = Snap.NEAREST): Float
    fun nextUp(kg: Float, exercise: CatalogExercise, units: UnitSystem): Float
    fun nextDown(kg: Float, exercise: CatalogExercise, units: UnitSystem): Float
    fun stepFractionOf(kg: Float, exercise: CatalogExercise, units: UnitSystem): Float
    fun lightest(exercise: CatalogExercise, units: UnitSystem): Float
    fun ceilingKg(equipment: Equipment): Float
}
```

**Snapping happens in the display unit, off a base, never in kilograms.** `shown = WeightUnit.forDisplay(kg, units)`; `rungs = round((shown - base) / step)` coerced ≥ 0; `result = WeightUnit.toKilograms(base + rungs * step, units)`. Kilogram-space rounding lets a barbell land on 22.5 kg, which no 20 kg bar plus a symmetric plate pair can make.

| Equipment | Metric base / step | Imperial base / step |
| --- | --- | --- |
| BARBELL | 20.0 / 2.5 | 45 / 5 |
| DUMBBELL | 2.5 / 2.5 (per bell) | 5 / 5 |
| MACHINE | 5.0 / 5.0 | 10 / 10 |
| PLATE | 1.25 / 1.25 | 2.5 / 2.5 |
| NONE, OTHER | 1.25 / 1.25 | 2.5 / 2.5 |
| KETTLEBELL | rungs 4,6,8,10,12,16,20,24,28,32,36,40,48 kg | rungs 10,15,20,…,60,70,80,90,105 lb |
| RESISTANCE_BAND, SUSPENSION_BAND | not loadable | not loadable |

`ceilingKg`: BARBELL 250, DUMBBELL 50, KETTLEBELL 48, MACHINE 200, PLATE 25, NONE/OTHER 40. Everything additionally clamped to 0.5..500.

For `oneHanded == false && equipment == DUMBBELL`, the seed is halved before snapping — `weightKg` is one bell, so a two-bell movement's total load is twice the stamped number, and the coefficient table is expressed as total load.

`WeightUnit.loadable` keeps its exact signature and `POUND_STEP` stays private and `Int`. `RoutineMapper` **stops calling it** — the prescription is already snapped by `LoadStep` at generation, and a second, coarser, equipment-blind snap could move a machine prescription off its own pin stack.

### 1.10 `domain/generation/SeedLoad.kt` (new)

```kotlin
object SeedLoad {
    fun tenRepMaxKg(user: UserProfile, exercise: CatalogExercise): Float?
    fun atReps(tenRepMaxKg: Float, reps: Int): Float
    fun holdSeconds(user: UserProfile, exercise: CatalogExercise): Int
    fun conditioningSeconds(user: UserProfile): Int
}
```

`seed = user.weight × C × S × A × E`, then `atReps(seed, targetReps)`, then halve for a two-bell dumbbell movement, then `snap(..., Snap.DOWN)`, then clamp to `lightest..ceilingKg`. Returns null where `!exercise.isLoadable`.

**Epley, both directions, reps clamped to 12:** `e1rm = tenRepMax × 4/3`; `atReps(r) = e1rm / (1 + min(r,12)/30)`. Without this a strength client's week 1 is ~14% too light and an endurance client's is too heavy.

**C — total load as a multiple of bodyweight, calibrated at INTERMEDIATE MALE:**

| Equipment | SQUAT | HINGE | LUNGE | H-PUSH | V-PUSH | H-PULL | V-PULL | ISOLATION | CORE | default |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BARBELL | 0.90 | 1.10 | 0.40 | 0.75 | 0.45 | 0.60 | 0.50 | 0.30 | 0.25 | 0.50 |
| DUMBBELL (total) | 0.50 | 0.60 | 0.36 | 0.60 | 0.36 | 0.56 | 0.44 | 0.24 | 0.24 | 0.40 |
| MACHINE | 1.40 | 0.70 | 0.35 | 0.70 | 0.45 | 0.70 | 0.65 | 0.25 | 0.30 | 0.50 |
| KETTLEBELL (per bell) | 0.25 | 0.22 | 0.16 | 0.18 | 0.16 | 0.25 | 0.20 | 0.12 | 0.12 | 0.18 |
| PLATE | 0.25 | 0.25 | 0.15 | 0.15 | 0.12 | 0.15 | 0.15 | 0.15 | 0.15 | 0.15 |
| NONE, OTHER | 0.15 | 0.15 | 0.10 | 0.15 | 0.10 | 0.10 | 0.10 | 0.10 | 0.12 | 0.10 |

**Muscle-group override, applied after C, before S.** `(equipment, pattern)` is 3–5× wrong for several large families; this is the smallest table that fixes them.

| `primary` | Multiplier | Why |
| --- | --- | --- |
| CALVES | 2.5 | A calf press is not a lateral raise. |
| SHOULDERS, when `pattern == ISOLATION` | 0.35 | Lateral and rear-delt raises. |
| HAMSTRINGS, when `pattern == ISOLATION` | 1.5 | Leg curls. |
| QUADRICEPS, when `pattern == ISOLATION` | 2.0 | Leg extensions. |
| BICEPS, TRICEPS, FOREARMS | 0.7 | Direct arm work. |
| ABDOMINALS, LOWER_BACK | 0.6 | Loaded core. |

**S:** MALE 1.00. FEMALE / NON_BINARY / PREFER_NOT_TO_SAY: 0.55 upper-body, 0.70 lower-body. Erring light is self-correcting within one session; erring heavy is an injury.
**A:** `1 − 0.01 × max(0, age − 40)`, floored 0.60; `age < 18` → 0.80 flat.
**E:** BEGINNER 0.65 / INTERMEDIATE 1.00 / ADVANCED 1.30.

**Empty-bar floor.** For BARBELL, if the snapped seed is below 20 kg / 45 lb, the skeleton is asked for a different movement (`ProgressionNote.LIGHTER_THAN_THE_BAR`, consumed — see §1.13). A 55 kg woman's barbell overhead press seeds at 8.8 kg; the engine must not lie about 20 kg and must not be ignored.

**Timed seeds.**

| Sub-kind | Recognised by | Seed |
| --- | --- | --- |
| Hold | `measure == DURATION` and `pattern` in CORE, SQUAT, HORIZONTAL_PULL, ISOLATION | 30 s BEGINNER / 40 INTERMEDIATE / 45 ADVANCED |
| Loaded carry | `pattern == CARRY` | 30 s BEGINNER / 40 / 45 — a carry is a hold, not a walk |
| Conditioning | `primary == CARDIO` | WEIGHT_LOSS/ENDURANCE 900 s, GENERAL_FITNESS 600 s, else 480 s |
| Mobility | `pattern == MOBILITY` | 60 s (ACSM-2011), except a WARM_UP slot which is 300 s |

CARRY as a hold is the fix for the three DURATION carry movements being prescribed as 8–15 minute farmer's walks.

### 1.11 `domain/generation/ExerciseHistory.kt` (new)

```kotlin
// One performance of one movement. A plan stored before startDateMillis
// existed has no date, and a gap nobody can measure is not a gap.
data class LoggedSession(
    val performedAtMillis: Long? = null,
    val prescribedSets: Int = 0,
    val sets: List<ExerciseSet> = emptyList()
)

// Newest first. HISTORY_DEPTH = 4 is the deepest any rule reaches.
data class ExerciseHistory(val sessions: List<LoggedSession> = emptyList()) {
    companion object {
        const val HISTORY_DEPTH = 4
        val None = ExerciseHistory()
        fun from(history: List<WeeklyWorkoutPlan>, exerciseKey: String): ExerciseHistory
    }
}
```

`from` walks the weeks newest-first; for each `WorkoutDay` holding that key it emits a `LoggedSession` with `performedAtMillis = day.completedAt ?: startDateMillis + (dayNumber - 1) days`. The date arithmetic is plain millisecond addition inside `domain/`; it does **not** import `presentation/workout/util/WorkoutWeek`, which is `Calendar`-based and would invert the layering.

A session is **usable** when it has at least one completed set *and* the measure it was logged in matches the movement's measure today. A completed set with a null actual falls back to its own target; a completed set with neither is not completed.

### 1.12 `domain/generation/ProgressionEngine.kt` (new)

```kotlin
data class ProgressionRequest(
    val user: UserProfile,
    val exercise: CatalogExercise,
    val history: ExerciseHistory = ExerciseHistory.None,
    // From the skeleton. The engine may return fewer, never more.
    val sets: Int,
    // Passed in, never read from the clock: a domain object that reads the
    // clock cannot be tested and cannot be replayed.
    val nowMillis: Long = 0L,
    val deload: Boolean = false
)

data class ProgressionTarget(
    val sets: List<ExerciseSet>,   // setNumber from 1, targets only
    val repRange: IntRange? = null,
    val isEstimate: Boolean = false,
    val outcome: ProgressionOutcome,
    val notes: Set<ProgressionNote> = emptySet(),
    val stallCount: Int = 0
)

enum class ProgressionOutcome {
    CALIBRATED, RESEEDED, REPEATED, HELD, LOAD_ADDED, REPS_ADDED,
    SECONDS_ADDED, REDUCED, RE_ANCHORED, RAMPED_BACK, DELOADED
}

enum class ProgressionNote { NEEDS_HARDER_VARIATION, LIGHTER_THAN_THE_BAR }

object ProgressionEngine {
    fun next(request: ProgressionRequest): ProgressionTarget
}
```

Returned sets carry `setNumber` from 1 and **only** targets, matching what `GeneratedPlanParser.toDomain` produces today. Rest is not the engine's; it comes from the slot.

### 1.13 `domain/generation/DeloadCheck.kt` (new)

```kotlin
object DeloadCheck {
    fun isDue(user: UserProfile, weeks: List<WeeklyWorkoutPlan>): Boolean
}
```

Called once per week by the expander; the answer goes into every `ProgressionRequest.deload`.

### 1.14 `domain/generation/Prescription.kt` (new) — the chip value

```kotlin
enum class PrescriptionUnit { REPS, SECONDS, MINUTES }

sealed interface Prescription {
    data object None : Prescription
    data class Fixed(val setCount: Int, val unit: PrescriptionUnit, val amount: Int, val perSide: Boolean) : Prescription
    data class Spread(val setCount: Int, val unit: PrescriptionUnit, val low: Int, val high: Int, val perSide: Boolean) : Prescription

    companion object {
        fun of(sets: List<ExerciseSet>, measure: ExerciseMeasure, perSide: Boolean = false): Prescription
    }
}
```

Never stored. Derived at render from the same `ExerciseSet` list the table below it draws, so adding a fourth set can no longer leave a frozen "3 sets of 12" above four rows. Rules: DURATION reads `targetSeconds`, everything else `targetReps`; sets with no target are not counted; no targets → `None`; seconds < 60 → SECONDS, all whole minutes with the largest ≥ 60 → MINUTES, else SECONDS; one distinct value → `Fixed`, two or more → `Spread(min, max)`.

`presentation/workout/model/PrescriptionText.kt`: `fun Prescription.chipText(resources: Resources): String`, `MAX_CHIP_CHARS = 24`, three rungs — `3 sets of 45 seconds` → `3 sets of 45 sec/side` → `10 × 30-59 sec/side`. `setCount == 1` renders the amount alone. Taking `Resources` rather than being `@Composable` is what lets the length property be swept without a Compose harness.

### 1.15 `data/generation/PlanSelection.kt` (new) — the wire model

The model's entire output is a day title and one movement key per open slot.

```kotlin
@Serializable
data class PlanSelection(val days: Map<String, DaySelection> = emptyMap())

@Serializable
data class DaySelection(
    val slots: Map<String, String> = emptyMap(),  // slot id -> exerciseKey
    val title: String = ""
)
```

Decoded with `Json { ignoreUnknownKeys = true; coerceInputValues = true }`. Every field defaults, because a half-written answer is worth repairing and a rejected one costs a whole round trip. Map keys are the skeleton's own ids, so ordering in the JSON is irrelevant.

**`intensity` is gone.** With RIR unimplementable it moved nothing but a rest interval the app clamps, and it cost roughly 38% of the schema's tokens plus a prompt rule and three repair rules.

Swift needs hand-written `decodeIfPresent` inits — synthesised `Codable` throws on a missing key. That is the one place the two platforms need different code for the same behaviour.

### 1.16 `data/generation/PlanSelectionSchema.kt` (new)

```kotlin
fun planSelectionSchema(skeleton: PlanSkeleton): Schema
fun daySelectionSchema(day: SkeletonDay): Schema   // the on-device per-day entry point, unused in Stage 1
```

`planSelectionSchema` = `Schema.obj(properties = days.filter { it.openSlots.isNotEmpty() }.associate { it.id to daySelectionSchema(it) })`.
`daySelectionSchema` = `Schema.obj(properties = openSlots.associate { it.id to Schema.enumeration(it.candidates) } + ("title" to Schema.string(description = day.focus.describe())))` — slots before title, so the name is written after the things it names.

`Schema.obj` with no `optionalProperties` makes every property required. `Schema.string` has no `maxLength` in firebase-ai 17.16.0, so the 40-character title cap is enforced in the repair, not the schema. An empty candidate list is unreachable (invariant 2), but degrades to `Schema.string` rather than an enum no answer can satisfy.

One call per week on remote: the free allowance is counted per request per model, so per-day would burn it four to five times faster for the same ~250 output tokens.

### 1.17 `data/generation/GeneratedPlan.kt` (changed) — kept, demoted

No longer the wire contract; now the expander's own intermediate on the way into the unchanged parser. Two fields leave, one tightens.

```kotlin
@Serializable data class GeneratedPlan(val title: String, val days: List<GeneratedDay>)
@Serializable data class GeneratedDay(val dayNumber: Int, val title: String, val exercises: List<GeneratedExercise>)
@Serializable data class GeneratedExercise(
    val exerciseKey: String,
    val restSeconds: Int,          // was Int? — a null is now a bug, not an omission
    val sets: List<GeneratedSet>
)
@Serializable data class GeneratedSet(val reps: Int? = null, val weightKg: Float? = null, val seconds: Int? = null)
```

`@Serializable` stays: `GeneratedPlanParser.parse(json, …)` is the public entry point and the golden fixtures on both platforms are JSON.

### 1.18 `domain/generation/PlanGenerator.kt` (changed)

```kotlin
data class PlanRequest(
    val user: UserProfile,
    val weekNumber: Int,
    val startDateMillis: Long,
    // Newest first. A stall is two short weeks and a ramp-back spans three,
    // so one previous week is not enough.
    val history: List<WeeklyWorkoutPlan> = emptyList()
) {
    val previousWeek: WeeklyWorkoutPlan? get() = history.firstOrNull()
}

enum class PlanSource { COACH, PROGRESSED, TEMPLATE }

sealed interface PlanGenerationResult {
    data class Generated(
        val plan: WeeklyWorkoutPlan,
        val source: PlanSource = PlanSource.COACH,
        // Why the coach was not used. Null when it was.
        val insteadOf: Failure? = null
    ) : PlanGenerationResult
    sealed interface Failure : PlanGenerationResult
    data object Offline : Failure
    data object Failed : Failure
    data object DailyLimitReached : Failure
}
```

`previousWeek` was a **named constructor argument** at four call sites (`NextWeekViewModel:90`, `:139`, `OnboardingViewModel:156`, plus tests) — verified. They all change; the derived property keeps every *reader* compiling.

### 1.19 Other changed types

- **`WorkoutExercise`** — no field added or removed. `prescription` and `instructions` stop being written (empty string) and stop being read. **The database stays at version 3 and no entity changes.** Dropping the two columns behind a real migration is a later commit, after release.
- **`ExerciseUi`** — `detail: String` becomes `prescription: Prescription`; gains `caution: InjuryCaution?` and `isEstimated: Boolean`. `description` now comes from `CatalogExercise.summary`.
- **`RoutineMapper`** — takes `injuries: List<Injury>`; description from the catalog summary; `prescription = Prescription.of(...)`; `isEstimated = previousSets.isEmpty() && measure == WEIGHT_AND_REPS`; the `WeightUnit.loadable` call is deleted.
- **`PlanModelClient.generate`** — fourth parameter becomes `skeleton: PlanSkeleton`. A flat key list cannot express a per-slot schema, and passing the domain type keeps LiteRT types from leaking past the seam in Stage 2.
- **iOS `Equipment.loaded`** gains `.plate`, matching Android's `LoadedEquipment`. Live drift, verified.

---

## 2. The progression rules, as one table

Per exercise, per week. **First match wins.** Every branch's load output then passes the same tail.

Notation: `W` = `RepWindow.forExercise`. `L` = last usable session's `targetWeightKg`. `minReps` = min over completed sets of `actualReps ?: targetReps`. `metInFull` = every prescribed set completed at reps ≥ its own `targetReps`. `quorum` = `completedSets × 2 >= prescribedSets`. `gap` = days since the last usable session, unknown → 0. `p` = `RepWindow.loadStepFraction`.

**`ladder`** = the number of consecutive prior sessions at the *same prescribed load* that were met in full. Derived by walking history; no column. **`ladderTop`** = `min(2, W.last - W.first)`, extended to `min(5, W.last - W.first)` when `stepFractionOf(L) > 0.25`.

**Why a 3-step ladder and not the whole window:** `RoutineUi.loggedAsPrescribed` writes `actualReps = targetReps` when a set is ticked, so in the ordinary flow reps never exceed target. A rule that waits for the top of the window means an endurance client's load never moves for nine weeks. Three steps gives every goal a load change every third met week, inside its own window.

| # | Rule | Fires when | Reps / seconds | Load | Sets | Outcome |
| --- | --- | --- | --- | --- | --- | --- |
| R0 | **Calibrate** | no usable session | `W.first` (bodyweight compound for a BEGINNER: `min(W.first, 8)`) | `SeedLoad.atReps(seed, reps)`, snap DOWN | as asked | `CALIBRATED`, `isEstimate` |
| R1 | **Deload** | `request.deload` | last reps unchanged | `L` unchanged | `max(1, sets / 2)` | `DELOADED` |
| R2 | **Long layoff** | `gap > 42` | `W.first` | `min(seed, L × 0.70)`, snap DOWN | as asked | `CALIBRATED`, `isEstimate` |
| R3 | **Month away** | `gap in 22..42` | last reps | `L × 0.90`, snap DOWN | `max(1, sets - 1)` | `REDUCED` |
| R4 | **Fortnight away** | `gap in 11..21` | last reps | `L` unchanged | as asked | `REPEATED` |
| R5 | **Ramping back** | any of the last 3 sessions had a 22–42 day gap before it, and fewer than 2 sessions since | `+1` rep | `L × 1.05`, then `max(that, nextUp(L))` | as asked | `RAMPED_BACK` |
| R6 | **Window moved** | `[min(lastTargetReps), max(lastTargetReps)]` does not overlap `W` | `W.first` | Epley re-anchor (below) | as asked | `RE_ANCHORED`, `isEstimate` |
| R7 | **Not performed** | last week held the key but no usable session | last targets unchanged | `L` | as asked | `REPEATED` |
| R8 | **Interrupted** | `!quorum` | last targets unchanged | `L` | as asked | `REPEATED` |
| R9 | **Bad first guess** | exactly one session ever, and `minReps < W.first` or `minReps > W.last` | `W.first` | `L × (minReps + 5) / (targetReps + 5)`, snap DOWN, clamp to `L × 0.6 .. L × 1.5` | as asked | `RESEEDED`, `isEstimate` |
| R10 | **Load up** | `metInFull` and `ladder >= ladderTop` | `W.first` | `max(snap(L × (1 + p), NEAREST), nextUp(L))` | as asked | `LOAD_ADDED` |
| R11 | **Rep up** | `metInFull` and `ladder < ladderTop` | `min(lastReps + 1, W.last)` | `L` | as asked | `HELD` |
| R12 | **One set short** | exactly one completed set short by 1–2 reps | last targets unchanged | `L` | as asked | `REPEATED` |
| R13 | **Stall** | anything else short | `W.first` | `L × 0.90`, snap DOWN | as asked | `REDUCED`, `stallCount + 1` |

**Measure variants**, replacing R9–R13:

| Measure | Rule |
| --- | --- |
| `REPS` (bodyweight) | met in full → `+1` rep to `W.last`; at `W.last` → `NEEDS_HARDER_VARIATION`, repeat; short → reps to `max(W.first, minReps)`, `REDUCED` |
| `DURATION`, hold or carry | met in full → `+5 s` to `HOLD_CEILING_SECONDS = 90`; at the ceiling → `NEEDS_HARDER_VARIATION`, repeat; two consecutive sessions with any set short → `−10 s`, floor 20 s |
| `DURATION`, conditioning | met → `+10%` of last seconds, snapped to 30 s, minimum real increase 60 s, capped at 3600 (WEIGHT_LOSS/ENDURANCE) / 2400 (GENERAL_FITNESS) / 1800; floor 300 s. Short → repeat unchanged: conditioning is missed for reasons that are rarely about capacity |
| `DURATION`, mobility | **never progressed.** Repeat the seed, `HELD`. A warm-up that grows five seconds a week is thirteen minutes of warm-up in a year |

**Epley re-anchor (R6):** `e1rm = L × (1 + min(minReps,12)/30)`; `anchored = e1rm / (1 + min(W.first,12)/30)`; clamp to `L × 0.75 .. L × 1.15`; snap DOWN.

**The tail, applied to every branch:**
1. If `snap(L, DOWN) != L` the units changed under the client — re-snap **down** before any progression, so switching a gym from kilos to pounds can never silently add weight.
2. Snap, then clamp to `lightest..ceilingKg(equipment)`, then to 0.5..500.
3. Clamp sets to `1..request.sets`. The engine never returns more sets than the skeleton paid for, so there is no trim pass anywhere.
4. `p` is capped at 0.025 when `age >= 65`, `age < 18`, or the movement carries an injury caution.
5. History is passed through `LoadStep.snap` before any comparison — plans written by the old model carry unsnapped kilograms.
6. No single week may change the load by more than ±20% of `L`, except R0 and R2. A garbage historical number cannot produce a garbage prescription.

**One increment is the floor, not the ceiling.** ACSM-2009's 2–10% is triggered *by* performing over target; it is not a safety cap. So `max(snapped, nextUp(L))` always applies — 45 lb + 3.5% snaps back to 45 lb, which is no increase, so it becomes 50 lb.

**`stallCount`, derived.** Walk sessions newest to oldest; count consecutive sessions where the quorum was met and the session was short *against its own persisted `targetReps`*. Stop at the first non-short session. No column, and it survives a profile edit because it never consults today's window.

**`DeloadCheck.isDue` — any two of:**

1. `stallCount >= 1` on ≥ 2 exercises in the last completed week.
2. Completion rate < 0.60 over the last two weeks, completed sets over prescribed sets across the whole plan.
3. `>= WEEKS_BETWEEN_DELOADS` weeks since the last deload or the last `REDUCED` on a compound. 6 normally, 4 when `age >= 50`.

Never when `experienceLevel == BEGINNER` and fewer than 8 weeks of plans exist. Trigger 3 is *weeks since a reduction*, not "6 consecutive weeks of load rises" — under a uniform 3-step ladder every movement rises on the same third week, so consecutive rises never reach six and the original trigger was unreachable. All three thresholds are `internal const val`; they rest on single small trials (Coleman 2024, Rogerson 2024) and are meant to be tuned.

**Notes are consumed, not emitted into the void.** `PlanExpander` calls the engine *before* fixing the key: on `LIGHTER_THAN_THE_BAR` or `NEEDS_HARDER_VARIATION` it takes the next-ranked candidate for that slot and calls the engine again, at most twice. A 55 kg woman's barbell overhead press becomes a dumbbell shoulder press rather than a 20 kg lie.

---

## 3. The prompt, ready to paste

### 3.1 System instruction — `PlanPromptBuilder.systemInstruction()`

1,058 characters, down from 5,876.

```kotlin
fun systemInstruction(): String = """
    You are a strength coach choosing which movements a client trains this week.

    The app has already decided the split, which days they train, how many slots
    each day holds and what each slot is for, and it computes every set, load,
    rest and instruction afterwards. Your job is which movement fills each slot,
    and what to call each session.

    Every slot offers only movements this client can perform, with the kit they
    own, that are safe for them. Choose one of them for each slot.

    - Take the candidate that best does that slot's job for this client. Where
      two do it equally well, prefer the one their experience and age suit.
    - A day is one session, not six separate choices: no two slots should take
      near-versions of the same movement, and a day of free weights should not
      send the client across four machines to finish it.
    - Titles are English, two to four words, and name the region and the focus:
      "Upper Body Strength", "Legs and Core" - never "Day 2", "Week 3" or
      "Full Body A".
    - Answer with JSON only, in the shape given.
""".trimIndent()
```

Every deleted rule now has a mechanism instead: the split, spacing and day count are the skeleton's day ids; frequency and volume are the skeleton's region allocation; the set cap, minutes and rest are `SessionBudget`; required patterns and tier ordering are the slot a candidate is dealt into; rep ranges, RIR, loads and increments are `ProgressionEngine` and `LoadStep`; conditioning dose is the conditioning slot; copy is templated and catalog-sourced; the plan title is templated from goal × experience. **Injuries lose their paragraph entirely** — a contraindicated key is absent from every enum, so a sentence asking the model to avoid it changes nothing and gives false comfort. That puts the whole obligation on `InjuryGuard`, which is why V-INJ below exists as a per-injury test.

### 3.2 User prompt — `PlanPromptBuilder.userPrompt(request, skeleton, listCandidates = false)`

```kotlin
fun userPrompt(
    request: PlanRequest,
    skeleton: PlanSkeleton,
    // Remote reads the candidates off the response schema. On device the
    // grammar constrains the sampler without ever entering the context, so
    // there the lists have to be said out loud.
    listCandidates: Boolean = false
): String {
    val user = request.user
    return buildString {
        appendLine("Choose the movements for week ${request.weekNumber}.")
        appendLine()
        appendLine(
            "Client: ${user.age}, ${user.experienceLevel.asText()}, " +
                "training to ${user.fitnessGoal.asText()}."
        )
        appendLine()
        appendLine("Sessions, in the order they are trained:")
        skeleton.days.filter { it.openSlots.isNotEmpty() }.forEach { day ->
            appendLine("- ${day.id}, ${day.focus.asText()}, ${day.openSlots.size} slots")
            if (listCandidates) {
                day.openSlots.forEach { slot ->
                    appendLine("    ${slot.id}: ${slot.candidates.joinToString(", ")}")
                }
            }
        }
        appendLine()
        appendLine("Each slot names the job it does and carries its own list of movements.")
        appendLine("Slots that are already settled are not shown; leave the rest of the week alone.")
    }
}
```

Rendered, week 2, four days, muscle gain — 411 characters:

```
Choose the movements for week 2.

Client: 34, intermediate, training to build muscle.

Sessions, in the order they are trained:
- day1, upper body push, 6 slots
- day3, lower body, 6 slots
- day5, upper body pull, 6 slots
- day6, full body and conditioning, 5 slots

Each slot names the job it does and carries its own list of movements.
Slots that are already settled are not shown; leave the rest of the week alone.
```

Deleted from `PlanPromptBuilder` outright: `appendVocabulary`, `ExerciseMeasure.asHeading`, `appendHistory`, `WorkoutExercise.asHistoryLine`, `appendRequiredPatterns`, `weeklyConditioningMinutes`, `incrementKg`, `UnitSystem.asWeightWord`, `Injury.asText`, `List<Equipment>.asText`, `Equipment.asText`. Height, weight, units, equipment, session length, set caps, weekly targets, required patterns, injuries and the whole history block do not appear at all — each was a rule the model could disobey, and none of them is now.

### 3.3 Retry feedback

`withFeedback` survives unchanged, and is used only by V0 (§4).

---

## 4. Validators, with exact error strings

Two layers. Layer A repairs the model's selection against the skeleton and can reject only once. Layer B is `GeneratedPlanParser`, now a regression net over the app's own arithmetic.

`${session}` is the day's `fallbackTitle`; `${slot}` is `SkeletonSlot.label`.

### Layer A — `PlanSelectionRepair`

| # | Fires | Action | Error string |
| --- | --- | --- | --- |
| **V0** | the answer is not a JSON object | **reject** | `Your answer was not a JSON object. Reply with only the JSON described by the schema, with nothing before or after it.` |
| **V1** | a session the schema named is missing | repair: fill every slot from rank 1, count one repair per slot | `You left out ${session}. Answer every session in the schema, each with its title and each of its slots.` |
| **V2** | a slot is missing | repair: rank-1 candidate | `In ${session} you left out ${slot}. Fill every slot with one movement from that slot's own list.` |
| **V3** | the key is not on that slot's list | repair: highest-ranked candidate for that slot not already taken today | `In ${session}, ${slot} was answered with '${key}'. That is not on that slot's list. Choose only from the keys listed for the slot you are filling.` |
| **V4** | the key is already used elsewhere in that day | repair: next unused candidate | `In ${session}, '${key}' is already used earlier in that session. Each slot needs a different movement.` |
| **V5** | title blank, over 40 characters, or fewer than two / more than four words | repair: `fallbackTitle` | `The title for ${session} must be two to four words naming the body region and the focus, like "Upper Body Strength". "${title}" is not.` |
| **V6** | title matches `(?i)\b(day|week|session|workout|phase)\s*\d\|\b[a-z]$` | repair: `fallbackTitle` | `The title for ${session} is an index label. The app already shows which day and which week it is; name what the session trains.` |
| **V7** | title contains `good form`, `engage your core`, `full body workout` or `training session` | repair: `fallbackTitle` | `The title for ${session} uses "${phrase}", which says nothing about this session. Name the region and the focus instead.` |
| **V8** | two sessions share a title | repair: the later takes its fallback | `${sessionA} and ${sessionB} are both called "${title}". Each session needs its own name.` |
| **V9** | repairs exceed half the week's open slots, or no session parsed at all | **reject**, feeding V1–V8's accumulated messages back | not a message |

V0 deliberately does not append the exception text: it can quote model output written from the profile, and `GeminiPlanGenerator` already refuses to breadcrumb model text for that reason. Breadcrumbs record the repair **count**, never the key.

**Everything else is unrepresentable**, not validated: session count, duplicate sessions, `dayNumber` range, exercises per day, slot ordering, tier correctness, and within-day duplicate movements. Nineteen of the parser's twenty-three current checks die or move this way.

### Layer B — `GeneratedPlanParser`, over the app's own output

Kept verbatim except as noted. Its job is now to catch a bug in our arithmetic before a client sees it.

| Message | Change |
| --- | --- |
| `not a generated plan: ${e.message}` | kept |
| `plan: title is blank` | kept — the app templates it, so this is a self-check |
| `plan: has no days`, `plan: day N appears more than once`, `day N: dayNumber must be 1..7, Monday to Sunday` | kept |
| `plan: the week has no ${label}, and needs one` | kept, now over `uncoveredPatterns`' complement |
| `day N: title is blank`, `has no exercises`, `has X exercises, more than 12` | kept |
| `day N: has X sets but the client's session length allows at most Y, warm-up included` | kept |
| `day N: runs about X minutes of work and rest, and the client asked for about Y` | kept, now computed by `SessionMinutes.forDay` including transitions |
| `day N: exerciseKey 'k' appears more than once` | kept |
| `..., k: 'k' is not one of the movements offered; choose only from that list` | kept; `allowedKeys` is now `skeleton.allowedKeys` |
| `..., k: 'k' is not a movement the app knows`, `is not a lower_snake_case slug` | kept |
| `..., k: prescription is blank` | **deleted** |
| `..., k: instructions are blank` | **deleted** |
| `..., k: restSeconds must be 5..600` | kept, and now unconditional — `restSeconds` is non-null |
| `..., k: has no sets`, `has X sets, more than 10` | kept |
| `..., k, set N: needs reps between 1 and 100` / `needs seconds between 5 and 5400` | kept |
| `..., k, set N: weightKg must be between 0.5 and 500` | kept |
| `plan: has X days but the client asked for exactly Y` (in `GeminiPlanGenerator`) | **deleted** — unrepresentable |

**V-INJ — not a runtime branch, a test.** `noCandidateListContainsAMovementContraindicatedForTheClientsInjuries`, one case per `Injury` value. The prompt no longer says a word about injuries, so the filter is the entire mechanism and needs its own proof.

### Retry policy

`MAX_ATTEMPTS = 3 → 2`, and the attempt counter merges with the model walk: every content failure now advances the model index *and* spends an attempt. Transport failures behave exactly as today (`Unreachable` returns `Offline`; `ModelUnavailable` and `QuotaSpent` advance the model without spending an attempt). Two rather than one because attempt 1 can be lost to a transient safety block and the second model is genuinely different; two rather than three because with semantic rejection gone a third attempt can only repeat a transport-adjacent failure at 3 s of backoff and one more request from a small daily allowance.

---

## 5. Commit order

Every numbered commit is an Android commit and its Swift mirror **landed in the same session**. Commit messages are one imperative sentence, no body, no trailers.

### C0 — bring iOS back into lockstep

The repos are **not** at the same commit: Android is at `b37cfcb`, iOS at `9286325` (`refactor(onboarding): ask what equipment the client has…` = Android's `a81b2c7`). iOS is missing #95 (lifting units), #96, #97, #98, #99. Everything below assumes `liftingUnitSystem` exists on iOS.

Files: the iOS ports of Android `9f3943b`, `d80184a`, `4354c90`, `65c6a79`, `b37cfcb`, plus `Trainr/Models/UnitSystem.swift` gaining `.plate` to `Equipment.loaded`.
Tests: the iOS mirrors of each of those commits' tests, plus `UnitSystemTests.aClientWhoOnlyOwnsPlatesIsAskedWhatTheyAreMarkedIn`.
Message: `chore(ios): catch the port up to the five commits Android has landed since`

### C1 — catalog fields

Files: `app/src/main/assets/exercise-catalog.json`, `Trainr-iOS/Trainr/Resources/exercise-catalog.json` (separate file, both need the edit), `data/catalog/ExerciseCatalogFile.kt`, `domain/catalog/ExerciseCatalog.kt` (+`unilateral`, `oneHanded`, `role`, `isLowerBody`, `isLoadable`) and the Swift mirrors.
The 41 name-matched `unilateral` candidates and the ~20 `oneHanded` candidates are hand-reviewed against the catalog source and the reviewed lists are pinned in the integrity test — a walking lunge alternates within the set and is not per-side; a goblet squat is one bell but bilateral.
Tests: `ExerciseCatalogIntegrityTest` — every movement has a summary; the unilateral list matches the pinned list exactly; the oneHanded list likewise; no `assisted_*` key is `WEIGHT_AND_REPS`; every `InjuryGuard` deny-listed key exists.
Message: `feat(catalog): record which movements work one side at a time and which use a single implement`

### C2 — snapping

Files: `domain/generation/LoadStep.kt` (new), `presentation/workout/model/RoutineMapper.kt` (delete the `WeightUnit.loadable` call), Swift mirrors.
Tests: `LoadStepTest` — barbell moves in plate pairs off an empty bar; a dumbbell increment is what is stamped on one bell; an imperial gym gets five-pound steps not converted kilograms; a kettlebell climbs real bell sizes; snapping never falls below the lightest loadable weight; a barbell is never snapped below the empty bar; bands and bodyweight have no increment; `nextUp` is always strictly heavier; a light cable reports its increment as more than a tenth. `RoutineMapperTest` — a prescribed weight reaches the screen unchanged.
Message: `fix(units): snap a prescribed weight to what the movement can actually be loaded in`

### C3 — minutes

Files: `domain/generation/SessionMinutes.kt` (new), `data/generation/GeneratedPlanParser.kt` (delete the private `minutes` getter and `SECONDS_PER_REP`, call `SessionMinutes`), Swift mirrors.
Tests: `SessionMinutesTest` — a per-side movement costs twice the rep time; a day charges a minute between exercises; the parser and the skeleton count the same minutes. `GeneratedPlanParserTest` rebaselined for the transition charge in the same commit.
Message: `fix(workout): count the minute between exercises, and the second side of a one-sided movement`

### C4 — rep windows, rest and the chip

Files: `domain/generation/RepWindow.kt`, `domain/generation/Prescription.kt`, `presentation/workout/model/PrescriptionText.kt` (all new), `domain/generation/SessionBudget.kt` (`restSeconds(goal, role)`, widen the four private constants to `internal`), `res/values/strings.xml` (13 prescription strings), Swift mirrors + `scripts/generate-strings.sh`.
Tests: `RepWindowTest` (8 cases including `aBeginnerWithAStrengthGoalIsNotPrescribedThreeRepSets` and `theCompoundRestMatchesWhatTheSessionBudgetPaidFor`), `PrescriptionTest` (13 cases), instrumented `PrescriptionTextTest` (4 cases including the 24-character sweep over set counts 1–20, reps 1–100, seconds 5–5400).
Message: `feat(workout): work out the prescription chip from the sets it describes`

### C5 — injuries

Files: `domain/catalog/InjuryGuard.kt` (new), `presentation/common/EnumExtensions.kt` (`InjuryCaution.text()`), `res/values/strings.xml` (7 caution strings), Swift mirrors.
Tests: `InjuryCautionTest` — no injury gives no caution; a knee injury cautions a squat and not a bench press; two matching injuries give the one declared first; every `Injury` maps to at least one pattern; every `InjuryCaution` is reachable; **all seven injuries with bodyweight only still leave a press, a pull and a squat**.
Message: `feat(workout): say what to watch for when a movement touches an injury the client declared`

### C6 — seeding and progression

Files: `domain/generation/SeedLoad.kt`, `ExerciseHistory.kt`, `ProgressionEngine.kt`, `DeloadCheck.kt` (all new), `domain/generation/PlanGenerator.kt` (`PlanRequest.history`), the four `PlanRequest(` call sites, Swift mirrors.
Tests: `SeedLoadTest` (10 cases — the Epley conversion, the muscle-group overrides, the two-bell halving, the equipment ceilings, the parser-acceptable range), `ProgressionEngineTest` (one per rule R0–R13 plus the four measure variants, plus: history carrying an unsnapped weight is snapped before comparison; no week-over-week change exceeds twenty per cent; the engine never reads the clock; the engine never returns more sets than it was given; every movement in the catalog produces a plan the parser would accept), `DeloadCheckTest` (6 cases).
Message: `feat(generation): work out next week's target from what the client actually lifted`

### C7 — the skeleton

Files: `domain/generation/SessionSkeleton.kt`, `PlanSkeletonBuilder.kt`, `SlotCandidates.kt` (all new), `domain/catalog/ExerciseShortlist.kt` (`requiredPatterns` computed from the injury-filtered pool), Swift mirrors. Nothing calls it yet.
Tests: `PlanSkeletonBuilderTest` (18 cases — the split table, no three consecutive hard days, the full profile cross product against `maxSetsPerSession` and `sessionCeilingMinutes` and the parser's own caps, non-decreasing tiers, disjoint candidate lists, required-pattern coverage, one-day-a-week admits what it cannot cover, a flexibility week is never given a squat, no repeated pattern across compound slots, mobility slots never outnumber the catalog's five mobility keys, every trainable region reaches the four-set floor, the warm-up survives the tightest session, determinism plus a checked-in expected rendering shared with iOS), `SlotCandidatesTest` (10 cases).
Message: `feat(generation): lay out a week's slots and what each could hold before anything picks`

### C8 — the deterministic week

Files: `data/generation/PlanSelection.kt`, `PlanExpander.kt`, `TemplatePlanGenerator.kt` (all new), `data/generation/GeneratedPlan.kt` (drop two fields, tighten `restSeconds`), `GeneratedPlanParser.kt` (drop the two blank checks), `app/src/dev/.../PlanGeneratorFactory.kt` (point at `TemplatePlanGenerator`), **delete** `app/src/dev/.../CannedPlanGenerator.kt` and `app/src/testDev/.../CannedPlanGeneratorTest.kt`, Swift mirrors (`CannedPlanGenerator.swift`, `CannedPlanGeneratorTests.swift`, `CannedDeterminismTests.swift` retargeted).
Prod is untouched, so this is low-risk despite its size. The 15 `ExerciseVideoCatalog` keys survive as ranking key 6's tiebreak, so dev builds keep their tutorials.
Tests: `PlanExpanderTest` (4 cases — instructions from the catalog, the chip agrees with the sets, a `LIGHTER_THAN_THE_BAR` note swaps the movement, an injury caution reaches the card), `TemplatePlanGeneratorTest` — **the property test**: 6 goals × 4 durations × 7 day counts × 3 experience levels × 8 equipment subsets × {no injuries, each single injury, all seven} × 2 unit systems, every week passing `GeneratedPlanParser` with the same `PlanLimits` the model path uses; plus determinism; plus a bodyweight-only client still gets a squat, a press and a pull; plus an empty catalog is the only way it can fail; plus no network is touched. `CannedDeterminismTests.sessionMatchesTheRequestedLength` becomes "within the ceiling and at least two thirds of the answered length" — the template fits work under the ceiling, it does not pad to hit a number.
Message: `feat(generation): build a whole week from the catalog with no model at all`

### C9 — the model contract

Files: `data/generation/PlanSelectionSchema.kt`, `PlanSelectionRepair.kt` (new), `docs/fixtures/plan-selection-schema.json` (new golden, asserted byte-identical by both platforms), `PlanPromptBuilder.kt` (rewritten), `GeminiPlanGenerator.kt` (skeleton → schema → repair → expander, `MAX_ATTEMPTS = 2`, day-count branch deleted), `PlanModelClient.kt` + `app/src/prod/.../FirebaseAiClient.kt` (signature), **delete** `GeneratedPlanSchema.kt`, Swift mirrors.
The four pieces cannot be split — turning the new validators on while the old whole-week prompt is live would raise the live rejection rate — and the schema string is shared verbatim between platforms, so both must land in the same session.
Tests: `PlanPromptBuilderTest` (12 cases, mostly absence assertions: the brief no longer prices sets, splits the week, restates injury rules or writes copy, and stays under 1,500 characters; the prompt carries no vocabulary, no history and no budget, and stays under 600 characters), `PlanSelectionSchemaTest` (12 cases including disjointness, decided slots absent, the golden fixture, and the 12 KB byte budget for the largest supported skeleton), `PlanSelectionRepairTest` (V0–V9, one each, plus malformed JSON and "no model at all yields the same selection as an empty answer"), `GeminiPlanGeneratorTest` updated (the offline, quota and breadcrumb cases survive verbatim).
Message: `feat(generation): ask the model only which movement fills each slot, and compute the rest`

### C10 — generation can no longer fail

Files: `data/generation/FallbackPlanGenerator.kt` (new), `domain/generation/PlanGenerator.kt` (`PlanSource`, `insteadOf`), `app/src/prod/.../PlanGeneratorFactory.kt`, `presentation/onboarding/screens/GeneratingScreen.kt`, `presentation/workout/NextWeekViewModel.kt`, `domain/purchases/ProGate` call site, `res/values/strings.xml`, Swift mirrors.
Tests: `FallbackPlanGeneratorTest` — a coached answer passes through as `COACH`; every failure kind produces a template week naming the failure in `insteadOf`; an empty catalog reports the coach's original failure rather than inventing one; the free allowance is spent only for `COACH`. `GeneratingScreenTest` — the provenance note appears for `TEMPLATE` and not for `COACH`. `OnboardingViewModelTest` updated.
Message: `feat(generation): fall back to a week we can build ourselves when the coach cannot answer`

### C11 — free week twos

Files: `data/generation/FallbackPlanGenerator.kt` (`canCarry`), `presentation/workout/NextWeekViewModel.kt`, Swift mirrors.
`canCarry` is true when the previous week's every key still resolves in the catalog and still passes the equipment and injury filters. When it is, the week is expanded from last week's cast with no model call at all — `PlanSource.PROGRESSED`.
Tests: `NextWeekViewModelTest` — a carryable week never calls the coach and does not spend the allowance; a profile edit that invalidates a key falls back to the coached path; a deload week keeps the load and halves the sets.
Message: `feat(generation): carry last week's movements forward without asking the model again`

### C12 — the card

Files: `presentation/workout/model/ExerciseUi.kt`, `RoutineMapper.kt`, `components/ExerciseCard.kt`, `RoutineDetailViewModel.kt`, `sample/SampleWorkoutData.kt` (regenerated through the expander), Swift mirrors.
The description becomes `CatalogExercise.summary` and is hidden when blank; the chip is derived and hidden when `None`; the caution line and the "Estimated" chip appear.
Tests: `RoutineMapperTest` (5 cases), `RoutineUiTest` (adding and removing a set re-derives the chip; clearing progress leaves it unchanged), `SampleWorkoutDataTest`, instrumented `ExerciseCardTest` (3 cases), `WorkoutPersistenceTest` (an expander-built plan round-trips through Room unchanged, proving no schema change was needed).
Message: `feat(workout): show what the app worked out, and say when a starting weight is a guess`

### C13 — the docs

Files: `docs/generation-contract.md` (rewritten: three columns — model, app, catalog; the selection schema; the expansion contract; V0–V9 and the parser's surviving checks; the three tiers), `docs/programming-evidence.md` (extended with the ~15 new sources and an explicit statement that the seed table, the deload triggers and the gap bands are tunable heuristics), `docs/on-device-generation.md` (correct §5's dumbbell-pair row, RIR overlay and kettlebell step to match what shipped).
Tests: the contract's worked example parses and assembles — the successor to `theContractDocumentsOwnExampleParses`, re-pointed here rather than in C9.
Message: `docs(generation): describe the contract the app now actually follows`

**Shippable independently:** C0, C1, C2, C3, C4, C5, C6, C7, C12, C13. **Must land together:** C9's four pieces, and each commit's Android and Swift halves. **Ordering constraints:** C1 before C2 and C8; C7 before C8; C8 before C9 (so we already know a deterministic week satisfies the new validators when they turn on); C9 before C10.

---

## 6. Open questions for the product owner

Everything else in this document is decided. These five are not mine to decide.

**1. What does the app say when a week came from the template tier — and does the daily-limit screen survive?**
`FallbackPlanGenerator` turns every failure into a plan, which is the point, but `PlanGenerationResult.DailyLimitReached` and the copy shipped in #98 exist so the retry button can say the limit is the reason. Swallowing that silently regresses #98 into "the app quietly got worse and did not say so"; showing a failure is now factually wrong. The iOS `PlanGenerator` also carries an explicit comment that a caller must never mistake a failure for a plan.
**Recommendation:** substitute automatically and label it — one quiet line on the generating screen naming the reason, `PROGRESSED` says nothing. Reword the daily-limit copy from "we could not build your plan" to "you are out of AI generations today — here is a plan built from your answers." I need your wording.

**2. Is the free generation allowance spent on a template or a progressed week?**
`ProGate.spend()` fires before generation today. A template week costs nothing to produce and a progressed week costs nothing either.
**Recommendation:** spend only for `PlanSource.COACH`. That makes week 2 onward free for everyone, which is the clearest payoff from the progression engine — but it is a pricing decision.

**3. Does the automatic next-week path stop calling the model entirely?**
Once C11 lands, week N+1 is built with no model call whenever the cast is unchanged. The explicit "regenerate this week" button becomes the only way a client gets a fresh cast, and the only place `DailyLimitReached` still has anything to say.
**Recommendation:** automatic next week uses the progressed path; regenerate forces the coached path and keeps the full failure UX. Say so in the button's confirmation copy, so the two are distinguishable.

**4. Do we add "heaviest weight available" to onboarding?**
`programming-evidence.md` documents the question; `UserProfile` does not have it. Without it the engine can seed a 60 kg dumbbell bench for a large advanced client whose gym tops out at 30 kg, and every week after that repeats it unchanged because the exercise is never performed. The per-equipment ceilings (250/50/48/200/25/40 kg) are a floor on the damage, not a fix.
**Recommendation:** ship the ceilings in C6 and add `heaviestLoadKg: Float?` in the same release if the onboarding screen has room for one more question. It is one more tap against a whole class of unusable prescriptions.

**5. Sign off the seven injury caution sentences and the disclaimer line.**
These are medically adjacent, shown to a named client about their own declared injury, and they replace a model-written cue. They are also the first contraindication list this app has ever authored, so `InjuryGuard.excludes` is now the entire mechanism — the prompt no longer mentions injuries at all.
**Recommendation:** review the seven strings in C5 before it lands, and add a "general fitness information, not medical advice" line where injuries are collected. App Store 1.4.1 and Play's health policy force that positioning regardless, and it is cheap now and expensive later.",
    "openQuestions": [
      {
        "question": "Section 5's increment table has a "dumbbell pair 5.0 / 4.54" row, which contradicts the settled decision that weightKg is one dumbbell. Delete the row?",
        "why": "If both survive, the same catalog key can be progressed at 2.5 kg or 5.0 kg depending on which line of the table an implementer reads, and the catalog has no field distinguishing single-bell from pair movements anyway.",
        "recommendation": "Delete it. Dumbbell is always 2.5 kg / 5 lb per bell, and I edit docs/on-device-generation.md section 5 to say so."
      },
      {
        "question": "Section 5's RIR overlay reads a reps-in-reserve value that nothing in the app logs. Add a logged RIR field in Stage 1, or drop the overlay?",
        "why": "ExerciseSet has actualReps, actualWeightKg, actualSeconds and isCompleted, and no effort field. The overlay is unimplementable as written, so it will silently become dead code or a fabricated value.",
        "recommendation": "Drop it from Stage 1. Section 5 itself argues beginners cannot self-report RIR and everyone under-predicts, so reps actually completed is the honest driver. Revisit only if a per-set effort control earns its place in the logging UI."
      },
      {
        "question": "Week-1 estimate labelling needs a place to live. Add loadIsEstimated to WorkoutExercise plus a DB column, or fold the sentence into instructions?",
        "why": "The column is a Room migration off version 3 and a mirrored SwiftData change; the instructions hack needs no migration but is not queryable, cannot be styled as a chip, and re-appears when a plan is repeated.",
        "recommendation": "Add the column, riding the migration Stage 1 already needs for the isWarmup flag. Doing two migrations for one release is the avoidable cost here."
      },
      {
        "question": "programming-evidence.md documents a "Heaviest weight available" onboarding question that UserProfile does not have. Add it, or ship the per-equipment ceiling clamps as the stand-in?",
        "why": "Without it the engine can seed a 60 kg dumbbell bench for a large advanced client whose gym tops out at 30 kg, and every week after that repeats it unchanged because the exercise is never performed.",
        "recommendation": "Ship the ceilings now (250/50/48/200/25/40 kg) and add heaviestLoadKg: Float? to UserProfile in the same release if the onboarding screen has room. The clamp is a floor on the damage, not a fix."
      },
      {
        "question": "Should the deload decision live in DeloadCheck as a separate object, or as a flag the caller derives?",
        "why": "Its triggers span multiple exercises and up to six weeks of plans, so it cannot be computed inside a per-exercise call, but putting it in the engine's file would make ProgressionEngine take a WeeklyWorkoutPlan list and stop being a per-exercise object.",
        "recommendation": "Separate object, called once per week, result passed in as ProgressionRequest.deload. Keeps the engine's input surface to one movement."
      },
      {
        "question": "iOS Equipment.loaded omits .plate where Android's LoadedEquipment includes it. Fix in this change or separately?",
        "why": "A plates-only iOS client is never asked which units their gym marks weights in, so liftingUnitSystem stays nil. Harmless today; once the engine snaps plate loads onto a 5 lb ladder it produces a different prescription on the two platforms from the same profile.",
        "recommendation": "Fix it first, as a one-line commit of its own, so the lockstep diff for this work stays about progression."
      },
      {
        "question": "SessionBudget.WORK_SECONDS_PER_SET is a flat 40 s, but the parser costs a set at reps × 3 s. At a strength goal's 3-rep sets those differ by about 4x, so maxSetsPerSession is priced for a ~13-rep set and the skeleton's own minutes come out at 33 for a session the budget priced at 57. Do we make the budget rep-aware?",
        "why": "It decides whether a 60-minute strength answer produces a 33-minute or a 50-minute prescription, and it moves maxSetsPerSession for every profile, so it rebaselines every existing SessionBudget test.",
        "recommendation": "Not in this change. Ship the skeleton against the current cap — it errs toward shorter, finishable sessions — and fix the minutes omission below first, which absorbs most of the gap. Revisit WORK_SECONDS_PER_SET only if the two still disagree afterwards."
      },
      {
        "question": "The minutes arithmetic counts rest between sets but never between exercises, so every day under-estimates by roughly (exercises − 1) × rest. Should SessionMinutes add it as it is extracted?",
        "why": "Example B day 3 is 33 minutes on the current formula and 48 with transitions counted. It is the difference between a plan that reads short and one that matches the answered session length, and it is also most of the gap in the question above.",
        "recommendation": "Add it in SessionMinutes, charging the compound rest for compound-to-compound transitions and zero into conditioning and cool-down, and rebaseline the parser's ceiling test in the same commit. The parser and the skeleton move together because they share the function, which is the point of the extraction."
      },
      {
        "question": "SessionBudget.coversEveryRegion(user) returns true at 7 sets per region for the 3x45 muscle-gain profile, but the skeleton can only deliver 4-6 on five of the nine regions. Should coversEveryRegion be answered from the skeleton instead of from the budget's estimate?",
        "why": "The prompt currently prints a weekly set target the week cannot buy, which is the same class of problem the SessionBudget comment already describes: a target that is never checked is a rule that is quietly dropped.",
        "recommendation": "Make WeekSkeleton.weeklySetsByRegion the answer and have PlanPromptBuilder print achieved-per-region rather than the target. Leave SessionBudget.weeklySetsPerMuscle as the input the skeleton aims at."
      },
      {
        "question": "Should slot candidate scoping and ranking (SlotCandidates) live in the same file as PlanSkeleton, or separately?",
        "why": "The prompt's per-slot enum and the deterministic fallback must walk exactly the same order, or the fallback can pick something the model was never offered.",
        "recommendation": "Same package, separate file (domain/generation/SlotCandidates.kt), one object with one public function. Keeping it out of PlanSkeleton keeps the split and budget tests free of the catalog; keeping it in the same package keeps the ordering in one place."
      },
      {
        "question": "The catalog has one generic warm-up key (warm_up) and one generic stretch (stretching), so every day of every plan opens and closes with the same two movements. Accept for now?",
        "why": "The prompt already asks for "easy versions of the movements that follow, not a generic routine", and repeating one key across all seven days is the kind of thing that reads as templated.",
        "recommendation": "Accept for this stage — it is honest and it is what the catalog holds. Follow up by letting the WARM_UP slot fall back to an easy, unloaded set of the day's primary movement when the shortlist has no unused mobility key, which needs no catalog change."
      },
      {
        "question": "At 7 days a week, do the two ACTIVE_RECOVERY days count toward workoutDaysPerWeek and appear as loggable workouts?",
        "why": "GeminiPlanGenerator asserts plan.workoutDays.size == user.workoutDaysPerWeek, so a recovery day that is not a workout day would make every 7-day plan fail that check.",
        "recommendation": "Yes, they count and they are loggable. The client asked for seven days; the prompt already says at most five of them are hard."
      },
      {
        "question": "Has a build shipped? The design drops two database columns and relies on `fallbackToDestructiveMigration` (version 3 → 4), which wipes every stored plan.",
        "why": "`TrainrDatabase` says the version is bumped without a migration on purpose because nothing has shipped, but the current work item is release hardening, so that may no longer be true. After a public build, wiping a user's logged sets to delete two unused columns is unacceptable.",
        "recommendation": "If nothing has shipped, bump to 4 and keep the destructive fallback. If it has, leave both columns in the entity as unread dead weight for one release, stop writing them, and drop them in a real table-rebuild migration later. Nothing in the design depends on the columns going away immediately."
      },
      {
        "question": "Three ladder rungs and 13 strings, or two rungs and 9?",
        "why": "Dropping the middle rung (full sets phrase, short unit word) removes four resources — `prescription_seconds_short`, `prescription_seconds_range_short`, `prescription_minutes_short`, `prescription_minutes_range_short` — at the cost of chips jumping straight from "3 sets of 45 seconds" to "3 × 45 seconds", and of the widest per-side timed case landing at 23 of the 24 characters instead of 19.",
        "recommendation": "Keep all three rungs. The middle one is chosen 19,488 times out of 193k in the sweep, all of them per-side timed work, which is exactly where a day would otherwise show two visibly different chip grammars side by side."
      },
      {
        "question": "Which of the 41 name-matched movements are genuinely per-side?",
        "why": "`unilateral` drives the "/side" suffix, and the doubling it implies also affects the time budget (`reps × 3 s` is half the true cost for a per-side movement). A walking lunge alternates within the set and is not per-side; a Bulgarian split squat is. A name scan cannot tell them apart.",
        "recommendation": "Hand-review the 41 candidates against the catalog source and land the flag with the reviewed list pinned in `ExerciseCatalogIntegrityTest`, so a later catalog edit cannot silently add or drop one."
      },
      {
        "question": "Should the injury caution ship in this change or immediately after?",
        "why": "It is the only genuinely new user-visible feature here; the rest is a straight substitution. Bundling it makes the change harder to review and the copy harder to argue about separately.",
        "recommendation": "Ship it in the same change. Removing the model-written cue and adding its replacement in one commit means there is never a release where a client's declared injury goes unmentioned on the card."
      },
      {
        "question": "Keep `intensity` in the contract at all?",
        "why": "It is 38% of the new schema's tokens (875 of 2,291) and the app clamps it anyway — at most two hard slots per day, never hard on an opener, and its only effect is a ±1 RIR / ±1 rep nudge inside the goal×role window. We may be paying 875 tokens, one prompt rule and three repair rules for something we then overrule.",
        "recommendation": "Keep it for Stage 1 and instrument it: log how often the repair clamps it and how often it differs from the tier default the app would have chosen. If it agrees with the default more than ~85% of the time after Stage 0, delete it in Stage 2 and let the slot value be a bare enum string — that alone takes the week-1 schema from 9,164 to 5,666 characters."
      },
      {
        "question": "Should the remote prompt repeat the candidate keys the schema already carries?",
        "why": "Gemini is conditioned on the response schema, so listing candidates again roughly doubles the prompt for no new information. But how strongly a model attends to a deeply nested enum versus prose in the user turn is not something I can verify from here, and a weak-attention failure looks like bland picks rather than an error.",
        "recommendation": "Ship remote with `listCandidates = false` (the schema and the slot property names carry it) and keep the flag. It is a one-line A/B in Stage 0/1 against the ≥80% human-acceptance target; flip it if acceptance is short. On-device it must be true regardless, since the grammar never enters the context."
      },
      {
        "question": "One call per week, or one per day, on the remote path?",
        "why": "§4 designs per-day for on-device latency and repairability. On remote the free allowance is counted in requests per model per day (`SpentModels`, `DailyLimitReached`), so per-day multiplies quota consumption by four or five and makes the daily-limit screen far more likely.",
        "recommendation": "Per week on remote, per day on device. The schema supports both without change — a one-day skeleton produces a one-day schema — so this is a call-site decision, not a contract decision."
      },
      {
        "question": "What does the daily-limit UX mean once generation can never fail?",
        "why": "`PlanGenerationResult.DailyLimitReached` and the retry button it drives were built for a world where no model meant no plan. With tier 2 deterministic selection, quota exhaustion produces a real plan of slightly lower quality. Silently downgrading is dishonest; showing a failure is now wrong.",
        "recommendation": "Keep the quota machinery (§6 says do not delete it until <5% of generations use tier 3), but change the result to `Generated(plan, source = DETERMINISTIC)` and let the UI decide whether to say so. Do not decide this in the contract change — it is a product call and it touches the generation screens. Flag it to the user before implementation starts."
      },
      {
        "question": "Fallback if Stage 0 measures LLGuidance mask latency over 24 distinct enums as prohibitive on ARM.",
        "why": "Per-token mask computation over many small enums is unmeasured anywhere (§ Remaining uncertainties #2), and it sits on top of a 6–8 tok/s decode.",
        "recommendation": "The retreat is §4's original shape: `slots` becomes an array with `minItems = maxItems = slotCount` over a single per-day union enum. Only `planSelectionSchema` changes; the data classes, prompt and repair survive. Budget for restoring three prompt rules (choose from the list, no repeats, tier order) and two validators if that happens, and note that per-day union enums are also ~1,270 tokens cheaper — so if Stage 0 says the model obeys those rules anyway, the retreat is not purely a loss."
      },
      {
        "question": "One call for the whole week, or one call per training day, against remote Gemini?",
        "why": "docs/on-device-generation.md §4 specifies per-day calls, but that reasoning is about KV cache size and per-day latency on a 2B model. Against Gemini the binding constraint is the free per-model daily request allowance, which is counted per request — five calls a plan burns it five times faster, and the whole-week answer is still only ~300 output tokens.",
        "recommendation": "One call per week for the remote tier. Ship daySelectionSchema(day) and PlanSelectionParser.parseDay as public entry points from day one so the on-device tier can go per-day later against the same parser and the same assembler with no reshape of its own."
      },
      {
        "question": "Once generation can never fail, what does the UI say when the plan came from the template tier — especially for the daily-limit case that commit #98 deliberately surfaced?",
        "why": "TieredPlanGenerator turns every PlanGenerationResult.Failure into a plan, which is the point. But DailyLimitReached exists so the retry button can say the limit is the reason, and swallowing it silently would regress #98 into 'the app quietly got worse and did not say so'.",
        "recommendation": "Add PlanSource { MODEL, TEMPLATE } and an optional fallback reason to Generated, and keep the existing DailyLimitReached copy, reworded from 'we could not build your plan' to 'you are out of AI generations today — here is a plan built from your answers'. The exact copy needs the user's call."
      },
      {
        "question": "Where does per-exercise history come from? PlanRequest carries only previousWeek.",
        "why": "§5 of the plan needs the last completed session of that exercise, with different arithmetic at 11-21, 22-42 and 42+ day gaps. previousWeek cannot answer 'when was this last performed' for a movement that was skipped last week but done three weeks ago, so those branches would be dead code.",
        "recommendation": "Extend PlanRequest with lastPerformed: Map<String, ExerciseOutcome>, sourced from the DB by the repository, in the ProgressionEngine task. PlanAssembler passes it straight through; the seam in this design already takes ExerciseOutcome? rather than reaching into previousWeek."
      },
      {
        "question": "Should the model still write the week's plan title?",
        "why": "The reshape takes the plan title away and templates it from goal × experience. That removes a validator and a call to withoutWeekNumber(), but it also removes the one place a plan gets a name with any personality.",
        "recommendation": "Template it. The doc's own example of a good title ('Beginner Muscle Building') is exactly what goal × experience produces, and the model's version reliably needed the '- Week 2' stripped off it."
      },
      {
        "question": "RoutineMapper still snaps every target weight with WeightUnit.loadable, which is the identity function in metric and a flat 5 lb step in imperial.",
        "why": "Once the ProgressionEngine snaps with an equipment-aware step (2.5 kg barbell, 5.0 kg machine, 4.0 kg kettlebell), RoutineMapper's snap re-rounds an already-correct number to a coarser grid and can move a machine prescription off its own pin stack. It is a double-snap that only one of the two owners knows about.",
        "recommendation": "Delete the snap from RoutineMapper as part of the ProgressionEngine task and let the assembled plan be the single source of the target, since the comment on that function ('shown and logged equal') is satisfied more directly by snapping once at assembly."
      },
      {
        "question": "Delete CannedPlanGenerator in favour of TemplatePlanGenerator for dev builds?",
        "why": "TemplatePlanGenerator is a real deterministic generator over the real catalog, skeleton and progression engine. CannedPlanGenerator is 303 lines of a parallel hand-built exercise pool that dev builds exercise instead of the production assembly path, so dev never sees a bug in the code that ships.",
        "recommendation": "Delete it. Point the dev PlanGeneratorFactory at TemplatePlanGenerator, which keeps the original reason for the file (dev builds never spend the model allowance) while making dev builds a real test of the assembler."
      },
      {
        "question": "Should a template-built week be persisted as such, or is a one-time note at generation enough?",
        "why": "Persisting provenance needs a `source` column on `weekly_workout_plans`, which bumps the DB from 3 to 4 and — because `fallbackToDestructiveMigration(dropAllTables = true)` is still in place — wipes every locally stored plan on update. That is the only thing in the whole Stage-1 reshape that would force a wipe. Without it, a client who reopens the plan a week later has no way to tell it was built offline.",
        "recommendation": "Do not persist it in Stage 1. Keep `PlanSource` on `PlanGenerationResult` only and show the note once, on the generating screen. Revisit when the destructive fallback is replaced with a real migration before release, at which point the column is free."
      },
      {
        "question": "When the coach fails, should the app substitute a template week silently, or keep the current failure screen and offer the template as a choice?",
        "why": "The iOS `PlanGenerator` carries an explicit comment that a caller must never mistake a failure for a plan, and #98 shipped deliberate UX for the daily-limit case. §4 of the plan says generation must never fail. These pull against each other, and the answer changes whether `DailyLimitReached` ever reaches a screen again.",
        "recommendation": "Substitute automatically, but label it: `Generated(plan, source = TEMPLATE, insteadOf = failure)` with one quiet line naming the reason. Getting a usable week now beats an empty state, and the label keeps the honesty the comment is defending. Keep the failure copy reachable from the explicit regenerate action, which should still force the coached path."
      },
      {
        "question": "Should the free generation allowance be spent on a template or progressed week?",
        "why": "`ProGate.spend()` is called before generation and marks the one free week used. A template week costs nothing to produce and a progressed week costs nothing either, so charging the allowance for them takes something real from the client in exchange for something free.",
        "recommendation": "Spend only for `PlanSource.COACH`. This makes week 2 onward free for everyone, which is a genuine product improvement and the clearest payoff from the progression engine."
      },
      {
        "question": "Does the explicit "regenerate this week" button use the coached path or the progressed path?",
        "why": "Once week N+1 can be built with no model call, the automatic path never asks the coach. The regenerate button is the only remaining way a client can get a fresh cast of movements, and it is also the only place `DailyLimitReached` still has anything to say.",
        "recommendation": "Regenerate forces the coached path and keeps the full failure UX. The automatic next-week path uses the progressed path. Say so in the button's confirmation copy so the two are distinguishable."
      },
      {
        "question": "How aggressive should `InjuryFilter` be, given nobody has reviewed it clinically?",
        "why": "There is no injury handling in code today — it is entirely prompt prose, so a wrong exclusion has never been the app's fault. §8 names this exactly: moving prescription into deterministic code is better engineering and a sharper medical-adjacent liability surface. Over-filtering hands a knee-injured client a week with no leg work; under-filtering is worse.",
        "recommendation": "Ship the table as drafted, keep every entry traceable to a line already in the current system instruction so nothing new is claimed, and make the safety-valve test (all seven injuries, no equipment, still a valid week) a hard gate. Add a visible "general fitness information, not medical advice" line where injuries are collected, which App Store 1.4.1 and Play health policy force anyway."
      },
      {
        "question": "Is `restTime` worth surfacing, or should it be deleted?",
        "why": "It is computed by the parser, persisted in `workout_exercises`, mapped through `UserMapper` in both directions, and read by no UI on either platform. The reshape makes it always non-null, so the app will be storing a precise number it never shows.",
        "recommendation": "Surface it: a rest line on the exercise card, or a rest countdown between sets. The number is now trustworthy for the first time (computed per tier from `SessionBudget`) and rest is the thing that actually spends the session. If that is out of scope, say in the contract doc that it is write-only, so the next reader does not assume it renders."
      }
    ]
  },
  "workflowProgress": [
    {
      "type": "workflow_phase",
      "index": 1,
      "title": "Design"
    },
    {
      "type": "workflow_phase",
      "index": 2,
      "title": "Verify"
    },
    {
      "type": "workflow_phase",
      "index": 3,
      "title": "Synthesize"
    },
    {
      "type": "workflow_agent",
      "index": 1,
      "label": "design:progression-engine",
      "phaseIndex": 1,
      "phaseTitle": "Design",
      "agentId": "abc33972a18fbb3da",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789057511990,
      "queuedAt": 1789057510653,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Design for `ProgressionEngine`: a pure, clock-free domain o…",
      "promptPreview": "ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt
  app/src/main/java/com/jericx/trainr/data/generation/Generated…",
      "lastProgressAt": 1789058096485,
      "tokens": 91966,
      "toolCalls": 28,
      "durationMs": 584495,
      "resultPreview": "{"summary":"Design for `ProgressionEngine`: a pure, clock-free domain object in `domain/generation/` that turns logged history into next-week targets. Five new Kotlin files plus Swift mirrors, with a full rule ladder (calibration → deload → time gap → window re-anchor → not-performed → partial → measure-specific progression), an equipment-and-unit increment ladder that snaps in the display unit ra…"
    },
    {
      "type": "workflow_agent",
      "index": 2,
      "label": "design:session-skeleton",
      "phaseIndex": 1,
      "phaseTitle": "Design",
      "agentId": "a9609712cc40088b0",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789057512040,
      "queuedAt": 1789057510653,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Design for a deterministic session skeleton (`PlanSkeleton`…",
      "promptPreview": "ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt
  app/src/main/java/com/jericx/trainr/data/generation/Generated…",
      "lastProgressAt": 1789058428068,
      "tokens": 139658,
      "toolCalls": 24,
      "durationMs": 914910,
      "resultPreview": "{"summary":"Design for a deterministic session skeleton (`PlanSkeleton`) that decides each day's shape — how many exercises, which slot each one is, how many sets, what rep window, what rest — before any model is asked. Eight-slot vocabulary ordered by tier; split table matching the rules already in `PlanPromptBuilder.systemInstruction()`; goal shapes that change slot mix, set counts and drop orde…"
    },
    {
      "type": "workflow_agent",
      "index": 3,
      "label": "design:prescription-and-copy",
      "phaseIndex": 1,
      "phaseTitle": "Design",
      "agentId": "a3be8a5e6a892476f",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789057512135,
      "queuedAt": 1789057510653,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Designed the move of the prescription chip and the exercise…",
      "promptPreview": "ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt
  app/src/main/java/com/jericx/trainr/data/generation/Generated…",
      "lastProgressAt": 1789058288321,
      "tokens": 125142,
      "toolCalls": 58,
      "durationMs": 775163,
      "resultPreview": "{"summary":"Designed the move of the prescription chip and the exercise description out of the model and into the app. The chip becomes a pure `Prescription` value derived from the computed sets plus the movement's measure and a new catalog `unilateral` flag, rendered through a three-rung string ladder that is proven (by sweep) never to exceed 24 characters. `WorkoutExercise.instructions` is redun…"
    },
    {
      "type": "workflow_agent",
      "index": 4,
      "label": "design:schema-and-prompt",
      "phaseIndex": 1,
      "phaseTitle": "Design",
      "agentId": "a6899035438f35f92",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789057512116,
      "queuedAt": 1789057510653,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Designed the shrunken model contract for Stage 1: the model…",
      "promptPreview": "ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt
  app/src/main/java/com/jericx/trainr/data/generation/Generated…",
      "lastProgressAt": 1789058140430,
      "tokens": 100232,
      "toolCalls": 31,
      "durationMs": 627272,
      "resultPreview": "{"summary":"Designed the shrunken model contract for Stage 1: the model returns a plan title, a title per day, and one `{exerciseKey, intensity}` per app-defined slot — nothing else. Key improvement over docs/on-device-generation.md §4: slots become **named object properties with disjoint per-slot enums** rather than an array over a per-day enum, which makes \"choose from this slot's list\", \"one…"
    },
    {
      "type": "workflow_agent",
      "index": 5,
      "label": "design:parser-and-validators",
      "phaseIndex": 1,
      "phaseTitle": "Design",
      "agentId": "a76f779f517e85726",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789057512222,
      "queuedAt": 1789057510653,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "{"summary":"placeholder"}",
      "promptPreview": "ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt
  app/src/main/java/com/jericx/trainr/data/generation/Generated…",
      "lastProgressAt": 1789058104464,
      "tokens": 91313,
      "toolCalls": 22,
      "durationMs": 591305,
      "resultPreview": "{"parameters":"{\"summary\":\"placeholder\"}","summary":"Design for the reshaped parse-and-assemble step: a slot-named JSON object schema that makes almost every current validator unrepresentable, a 5-validator repair-first parser, and a PlanAssembler that builds the domain plan from the skeleton, the catalog and the ProgressionEngine. 19 of the 23 current validators die or move; the retry loop sh…"
    },
    {
      "type": "workflow_agent",
      "index": 6,
      "label": "design:fallback-and-migration",
      "phaseIndex": 1,
      "phaseTitle": "Design",
      "agentId": "a78cf1ef4907fdad2",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789057512215,
      "queuedAt": 1789057510653,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "Design for (A) a zero-model deterministic plan generator — …",
      "promptPreview": "ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt
  app/src/main/java/com/jericx/trainr/data/generation/Generated…",
      "lastProgressAt": 1789058321709,
      "tokens": 151198,
      "toolCalls": 49,
      "durationMs": 808549,
      "resultPreview": "{"summary":"Design for (A) a zero-model deterministic plan generator — skeleton + ranked catalog selection + ProgressionEngine — that satisfies the same validators as model output and sits behind the existing `PlanGenerator` interface alongside `GeminiPlanGenerator`, and (B) the migration/blast-radius analysis for the Stage-1 reshape. Headline findings: **no Room or SwiftData schema change is need…"
    },
    {
      "type": "workflow_agent",
      "index": 7,
      "label": "verify:lens1",
      "phaseIndex": 2,
      "phaseTitle": "Verify",
      "agentId": "a38e4677f72f9f392",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789058431837,
      "queuedAt": 1789058429655,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "needs-work",
      "promptPreview": "You are reviewing a set of implementation specs for one coherent change to a real codebase.

ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/dat…",
      "lastProgressAt": 1789059043896,
      "tokens": 200147,
      "toolCalls": 41,
      "durationMs": 612058,
      "resultPreview": "{"contradictions":["Four specs each declare a different `PlanSkeleton` in `com.jericx.trainr.domain.generation`: session-skeleton's `object PlanSkeleton` + `WeekSkeleton`/`DaySkeleton(dayNumber, focus, slots)`/`SlotSkeleton`; schema-and-prompt's `data class PlanSkeleton(days)` + `DaySkeleton(dayNumber, theme, slots)` + `PlanSlot`; parser-and-validators' `data class PlanSkeleton(title, days, units,…"
    },
    {
      "type": "workflow_agent",
      "index": 8,
      "label": "verify:lens2",
      "phaseIndex": 2,
      "phaseTitle": "Verify",
      "agentId": "a2a47b2168d86663e",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789058432748,
      "queuedAt": 1789058429655,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "needs-work",
      "promptPreview": "You are reviewing a set of implementation specs for one coherent change to a real codebase.

ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/dat…",
      "lastProgressAt": 1789058887054,
      "tokens": 166526,
      "toolCalls": 18,
      "durationMs": 453257,
      "resultPreview": "{"contradictions":["**`GeneratedPlan` is simultaneously deleted, kept and edited.** parser-and-validators §2.1/§6.4 says `GeneratedPlan`/`GeneratedDay`/`GeneratedExercise`/`GeneratedSet` and `GeneratedPlanParser.kt` (with `PlanLimits`, `PlanParseResult`) are **deleted** and `PlanAssembler` builds domain types directly. schema-and-prompt §1 says they **stay, in place, demoted** to the expander's ou…"
    },
    {
      "type": "workflow_agent",
      "index": 9,
      "label": "verify:lens3",
      "phaseIndex": 2,
      "phaseTitle": "Verify",
      "agentId": "aea26d591c9a59a19",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789058433643,
      "queuedAt": 1789058429655,
      "attempt": 1,
      "lastToolName": "StructuredOutput",
      "lastToolSummary": "needs-work",
      "promptPreview": "You are reviewing a set of implementation specs for one coherent change to a real codebase.

ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete any file.

Android:
  app/src/main/java/com/jericx/trainr/dat…",
      "lastProgressAt": 1789058913250,
      "tokens": 163406,
      "toolCalls": 21,
      "durationMs": 479454,
      "resultPreview": "{"contradictions":["Three incompatible week-1 seed tables. progression-engine §2.1 gives C(BARBELL,SQUAT)=0.90, C(BARBELL,HINGE)=1.10, C(MACHINE,SQUAT)=1.40, E=0.65/1.00/1.30, A floored at 0.60, then an Epley conversion from the 10RM to the goal's rep target. fallback-and-migration §A1 SeedLoads gives 0.75 / 0.90 / 1.30, E=1.0/1.3/1.6 with a further x0.65 for beginners, x0.6 under 18, x0.8 over 65…"
    },
    {
      "type": "workflow_agent",
      "index": 10,
      "label": "synthesize:stage1",
      "phaseIndex": 3,
      "phaseTitle": "Synthesize",
      "agentId": "a0aeb5ccc839a3254",
      "model": "claude-opus-5",
      "state": "done",
      "startedAt": 1789059053914,
      "queuedAt": 1789059045924,
      "attempt": 1,
      "lastToolName": "Bash",
      "lastToolSummary": "grep -n "loggedAsPrescribed" -A 25 app/src/main/java/com/je…",
      "promptPreview": "Turn these subsystem specs and their critiques into ONE implementation document that a single engineer will follow end to end, on both Android and iOS.

ANDROID repo root: /Users/jericho/StudioProjects/Trainr
iOS repo root:     /Users/jericho/StudioProjects/Trainr-iOS  (1:1 SwiftUI port, must stay in lockstep)

READ THESE BEFORE WRITING ANYTHING. You are READ-ONLY - do not edit, create or delete a…",
      "lastProgressAt": 1789059604939,
      "tokens": 235316,
      "toolCalls": 17,
      "durationMs": 551023,
      "resultPreview": "> **Two corrections applied after verifying against the repo.** The design agents raised two
> findings that turned out to be wrong as stated:
>
> 1. *"The repos are not in lockstep, iOS is four commits behind."* False — the agents compared
>    against `main`, not the feature branches both platforms are on. The branches carry the same
>    work. The one real thing inside this claim: iOS `Equipment.loaded` was missing `.plate`,
>    so a plates-only client was never asked what their weights are marked in. Fixed separately.
>    **C0 below is void.**
> 2. *"Tick-off writes `actualReps = targetReps`, so 'progress when reps exceed target' is
>    unreachable."* Overstated. `loggedAsPrescribed()` uses `actualReps ?: targetReps` — a typed
>    number is kept. What is true, and what the progression rules must respect, is that the
>    **default** tick-off records exactly the target, so progression keys off *hitting* the
>    target rather than exceeding it. Exceeding remains a valid bonus signal for clients who log.

---

#…"
    }
  ],
  "totalTokens": 1464904,
  "totalToolCalls": 309
}