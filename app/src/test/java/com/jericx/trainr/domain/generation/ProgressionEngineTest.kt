package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeightUnit
import org.junit.Test

class ProgressionEngineTest {

    private fun movement(
        key: String,
        equipment: Equipment,
        measure: ExerciseMeasure,
        pattern: MovementPattern,
        primary: MuscleGroup = MuscleGroup.CHEST,
        oneHanded: Boolean = false
    ) = CatalogExercise(
        key = key, name = key, primary = primary, secondary = emptyList(), equipment = equipment,
        measure = measure, pattern = pattern, staple = true, oneHanded = oneHanded
    )

    private val bench = movement(
        "barbell_bench_press", Equipment.BARBELL, ExerciseMeasure.WEIGHT_AND_REPS,
        MovementPattern.HORIZONTAL_PUSH
    )
    private val pushUp = movement("push_up", Equipment.NONE, ExerciseMeasure.REPS, MovementPattern.HORIZONTAL_PUSH)
    private val plank = movement("plank", Equipment.NONE, ExerciseMeasure.DURATION, MovementPattern.CORE, MuscleGroup.ABDOMINALS)
    private val walking = movement("walking", Equipment.NONE, ExerciseMeasure.DURATION, MovementPattern.CONDITIONING, MuscleGroup.CARDIO)
    private val warmUp = movement("warm_up", Equipment.NONE, ExerciseMeasure.DURATION, MovementPattern.MOBILITY, MuscleGroup.FULL_BODY)

    // Muscle gain, intermediate: a compound's window is 6-10.
    private val lifter = UserProfile(
        age = 30, gender = Gender.MALE, weight = 80f,
        fitnessGoal = FitnessGoal.MUSCLE_GAIN, experienceLevel = ExperienceLevel.INTERMEDIATE
    )

    private fun logged(
        reps: Int,
        kg: Float? = null,
        sets: Int = 3,
        done: Int = sets,
        actual: Int = reps,
        at: Long? = null,
        measure: ExerciseMeasure = ExerciseMeasure.WEIGHT_AND_REPS
    ) = LoggedSession(
        performedAtMillis = at,
        prescribedSets = sets,
        measure = measure,
        sets = (1..sets).map { number ->
            val ticked = number <= done
            if (measure == ExerciseMeasure.DURATION) {
                ExerciseSet(
                    setNumber = number, targetSeconds = reps,
                    actualSeconds = if (ticked) actual else null, isCompleted = ticked
                )
            } else {
                ExerciseSet(
                    setNumber = number, targetReps = reps, targetWeightKg = kg,
                    actualReps = if (ticked) actual else null,
                    actualWeightKg = if (ticked) kg else null, isCompleted = ticked
                )
            }
        }
    )

    private fun next(
        vararg newestFirst: LoggedSession,
        exercise: CatalogExercise = bench,
        user: UserProfile = lifter,
        sets: Int = 3,
        now: Long = 0L,
        deload: Boolean = false
    ) = ProgressionEngine.next(
        ProgressionRequest(user, exercise, ExerciseHistory(newestFirst.toList()), sets, now, deload)
    )

    private val ProgressionTarget.load get() = sets.first().targetWeightKg!!
    private val ProgressionTarget.reps get() = sets.first().targetReps!!
    private val ProgressionTarget.seconds get() = sets.first().targetSeconds!!

    private fun days(n: Int) = n * 86_400_000L

    // Nobody has lifted anything yet, so the first number is a guess made on
    // the light side, at the bottom of the window, and the card says so.
    @Test
    fun theFirstWeekIsACalibratedGuessAtTheBottomOfTheWindow() {
        val target = next()

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.CALIBRATED)
        assertThat(target.isEstimate).isTrue()
        assertThat(target.sets.map { it.targetReps }.distinct()).containsExactly(6)
        assertThat(target.load).isAtLeast(20f)
    }

    // The default tick-off logs exactly the target, so the ladder climbs on
    // hitting it: a rep first, then the load, then back to the bottom.
    @Test
    fun aMetWeekClimbsARepBeforeItClimbsTheLoad() {
        val afterOne = next(logged(6, 60f))
        assertThat(afterOne.outcome).isEqualTo(ProgressionOutcome.HELD)
        assertThat(afterOne.reps).isEqualTo(7)
        assertThat(afterOne.load).isEqualTo(60f)

        val afterTwo = next(logged(7, 60f), logged(6, 60f))
        assertThat(afterTwo.outcome).isEqualTo(ProgressionOutcome.LOAD_ADDED)
        assertThat(afterTwo.reps).isEqualTo(6)
        assertThat(afterTwo.load).isGreaterThan(60f)
    }

    // 45 lb plus three and a half per cent snaps back to 45 lb, which is no
    // increase at all, so one plate is the least a load-up can add.
    @Test
    fun aLoadIncreaseIsAlwaysAtLeastOnePlate() {
        val imperial = lifter.copy(liftingUnitSystem = UnitSystem.IMPERIAL)
        val bar = WeightUnit.toKilograms(45f, UnitSystem.IMPERIAL)

        val target = next(logged(7, bar), logged(6, bar), user = imperial)

        assertThat(WeightUnit.forDisplay(target.load, UnitSystem.IMPERIAL)).isWithin(0.1f).of(50f)
    }

    @Test
    fun oneSetShortByARepRepeatsTheWeek() {
        val short = LoggedSession(
            prescribedSets = 3, measure = ExerciseMeasure.WEIGHT_AND_REPS,
            sets = listOf(10, 10, 9).mapIndexed { i, done ->
                ExerciseSet(
                    setNumber = i + 1, targetReps = 10, targetWeightKg = 60f,
                    actualReps = done, actualWeightKg = 60f, isCompleted = true
                )
            }
        )

        val target = next(short)

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.REPEATED)
        assertThat(target.load).isEqualTo(60f)
        assertThat(target.reps).isEqualTo(10)
    }

    // Short on a session that was not the first. On the very first one the
    // same numbers mean the guess was wrong, which is a reseed, not a stall.
    @Test
    fun aStallTakesAboutATenthOffAndIsCounted() {
        val target = next(logged(8, 60f, actual = 5), logged(8, 60f))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.REDUCED)
        assertThat(target.load).isLessThan(60f)
        assertThat(target.load).isAtLeast(48f)
        assertThat(target.stallCount).isEqualTo(1)
    }

    // A set typed at a lighter weight did not earn the next one.
    @Test
    fun repsAtALighterWeightThanAskedDoNotEarnALoadIncrease() {
        val lighter = LoggedSession(
            prescribedSets = 2, measure = ExerciseMeasure.WEIGHT_AND_REPS,
            sets = (1..2).map {
                ExerciseSet(
                    setNumber = it, targetReps = 7, targetWeightKg = 60f,
                    actualReps = 7, actualWeightKg = 50f, isCompleted = true
                )
            }
        )

        val target = next(lighter, logged(6, 60f))

        assertThat(target.outcome).isNotEqualTo(ProgressionOutcome.LOAD_ADDED)
        assertThat(target.load).isAtMost(60f)
    }

    // One set done of four is an interrupted day, not a failed one.
    @Test
    fun anInterruptedWeekIsRepeatedRatherThanJudged() {
        val target = next(logged(6, 60f, sets = 4, done = 1))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.REPEATED)
        assertThat(target.load).isEqualTo(60f)
    }

    @Test
    fun aMovementPrescribedAndNeverDoneIsRepeatedAndStaysAGuess() {
        val target = next(logged(6, 60f, done = 0))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.REPEATED)
        assertThat(target.isEstimate).isTrue()
        assertThat(target.load).isEqualTo(60f)
    }

    @Test
    fun aDeloadHalvesTheSetsAndKeepsTheLoad() {
        val target = next(logged(6, 60f, sets = 4), sets = 4, deload = true)

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.DELOADED)
        assertThat(target.sets).hasSize(2)
        assertThat(target.load).isEqualTo(60f)
    }

    @Test
    fun aFortnightAwayRepeatsTheWeek() {
        val target = next(logged(6, 60f, at = days(100)), now = days(115))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.REPEATED)
        assertThat(target.load).isEqualTo(60f)
    }

    @Test
    fun aMonthAwayComesBackLighterAndWithASetLess() {
        val target = next(logged(6, 60f, at = days(100)), now = days(130))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.REDUCED)
        assertThat(target.sets).hasSize(2)
        assertThat(target.load).isLessThan(60f)
    }

    // A break is not a catastrophe, but nobody resumes at the old number
    // after two months.
    @Test
    fun aLongLayoffStartsOverFromAGuess() {
        val target = next(logged(6, 100f, at = days(100)), now = days(170))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.CALIBRATED)
        assertThat(target.isEstimate).isTrue()
        assertThat(target.load).isAtMost(70f)
    }

    // The week after coming back climbs back quicker than a normal week does.
    @Test
    fun theWeekAfterComingBackRampsTheLoadUp() {
        val target = next(
            logged(6, 54f, at = days(130)), logged(6, 60f, at = days(100)), now = days(137)
        )

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.RAMPED_BACK)
        assertThat(target.load).isGreaterThan(52.5f)
    }

    // Wildly off on the very first session means the guess was wrong, so the
    // load is worked out again from what was actually managed.
    @Test
    fun aBadFirstGuessIsCorrectedFromWhatWasManaged() {
        val target = next(logged(8, 60f, actual = 3))

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.RESEEDED)
        assertThat(target.isEstimate).isTrue()
        assertThat(target.load).isLessThan(60f)
    }

    // Plans written by the old model carry unsnapped kilograms.
    @Test
    fun anUnsnappedWeightInHistoryIsSnappedDownBeforeAnythingElse() {
        val target = next(logged(6, 61.3f))

        assertThat(target.load).isEqualTo(60f)
    }

    @Test
    fun neverMoreSetsThanTheSkeletonPaidFor() {
        val target = next(logged(6, 60f, sets = 5), sets = 3)

        assertThat(target.sets).hasSize(3)
    }

    // A garbage number in history cannot become a garbage prescription, and
    // the cap never blocks a single increment.
    @Test
    fun noWeekMovesTheLoadMoreThanAFifthOrOneIncrement() {
        listOf(20f, 22.5f, 40f, 60f, 100f, 180f).forEach { load ->
            listOf(logged(6, load), logged(8, load, actual = 3), logged(7, load)).forEach { session ->
                val target = next(session, logged(6, load))
                val allowed = maxOf(
                    load * 0.2f,
                    LoadStep.nextUp(load, bench, UnitSystem.METRIC) - load
                )

                assertWithMessage("$load kg").that(kotlin.math.abs(target.load - load))
                    .isAtMost(allowed + 0.01f)
            }
        }
    }

    @Test
    fun theEngineIsAPureFunctionOfItsRequest() {
        val history = arrayOf(logged(7, 60f), logged(6, 60f))

        assertThat(next(*history)).isEqualTo(next(*history))
    }

    // Moving from strength to endurance changes what the numbers mean, so the
    // load is worked out again rather than carried over.
    @Test
    fun aGoalChangeWorksTheLoadOutAgain() {
        val endurance = lifter.copy(fitnessGoal = FitnessGoal.ENDURANCE)

        val target = next(logged(3, 100f), user = endurance)

        assertThat(target.outcome).isEqualTo(ProgressionOutcome.RE_ANCHORED)
        assertThat(target.isEstimate).isTrue()
        assertThat(target.reps).isEqualTo(12)
        assertThat(target.load).isLessThan(100f)
    }

    @Test
    fun aBodyweightMovementClimbsRepsThenAsksForSomethingHarder() {
        val climbing = next(logged(8, measure = ExerciseMeasure.REPS), exercise = pushUp)
        assertThat(climbing.outcome).isEqualTo(ProgressionOutcome.REPS_ADDED)
        assertThat(climbing.reps).isEqualTo(9)

        val topped = next(logged(10, measure = ExerciseMeasure.REPS), exercise = pushUp)
        assertThat(topped.notes).contains(ProgressionNote.NEEDS_HARDER_VARIATION)
        assertThat(topped.reps).isEqualTo(10)
    }

    @Test
    fun aHoldGrowsFiveSecondsAndStopsAtNinety() {
        val growing = next(logged(40, measure = ExerciseMeasure.DURATION), exercise = plank)
        assertThat(growing.seconds).isEqualTo(45)

        val topped = next(logged(90, measure = ExerciseMeasure.DURATION), exercise = plank)
        assertThat(topped.notes).contains(ProgressionNote.NEEDS_HARDER_VARIATION)
        assertThat(topped.seconds).isEqualTo(90)
    }

    @Test
    fun conditioningGrowsByAtLeastAMinuteInHalfMinutes() {
        val target = next(logged(600, measure = ExerciseMeasure.DURATION), exercise = walking)

        assertThat(target.seconds).isAtLeast(660)
        assertThat(target.seconds % 30).isEqualTo(0)
    }

    // A warm-up that grows five seconds a week is thirteen minutes of warm-up
    // in a year.
    @Test
    fun aWarmUpNeverGrows() {
        val target = next(logged(300, measure = ExerciseMeasure.DURATION), exercise = warmUp)

        assertThat(target.seconds).isEqualTo(300)
        assertThat(target.outcome).isEqualTo(ProgressionOutcome.HELD)
    }

    // weightKg is one bell, and the seed is for the whole load.
    @Test
    fun aPairOfDumbbellsIsSeededPerBell() {
        val pair = movement("dumbbell_bench_press", Equipment.DUMBBELL, ExerciseMeasure.WEIGHT_AND_REPS, MovementPattern.HORIZONTAL_PUSH)
        val single = pair.copy(key = "one_bell_press", oneHanded = true)

        assertThat(next(exercise = pair).load).isLessThan(next(exercise = single).load)
    }

    // The lightest barbell is still 20 kg. A guess lighter than that has to
    // ask for another movement, not pretend.
    @Test
    fun aGuessLighterThanTheEmptyBarSaysSo() {
        val press = movement("barbell_overhead_press", Equipment.BARBELL, ExerciseMeasure.WEIGHT_AND_REPS, MovementPattern.VERTICAL_PUSH, MuscleGroup.SHOULDERS)
        val light = UserProfile(
            age = 30, gender = Gender.FEMALE, weight = 50f,
            fitnessGoal = FitnessGoal.MUSCLE_GAIN, experienceLevel = ExperienceLevel.BEGINNER
        )

        val target = next(exercise = press, user = light)

        assertThat(target.notes).contains(ProgressionNote.LIGHTER_THAN_THE_BAR)
        assertThat(target.load).isEqualTo(20f)
    }
}
