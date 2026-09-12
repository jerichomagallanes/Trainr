package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus
import org.junit.Test

private fun catalogExercise(
    key: String,
    muscle: MuscleGroup,
    measure: ExerciseMeasure,
    pattern: MovementPattern,
    equipment: Equipment = Equipment.NONE
) = CatalogExercise(key, key.replace('_', ' '), muscle, emptyList(), equipment, measure, pattern, staple = true)

class GeneratedPlanParserTest {

    private val catalog = InMemoryExerciseCatalog(
        listOf(
            catalogExercise("warm_up_jog", MuscleGroup.CARDIO, ExerciseMeasure.DURATION, MovementPattern.CONDITIONING),
            catalogExercise("bicycle_crunch", MuscleGroup.ABDOMINALS, ExerciseMeasure.REPS, MovementPattern.CORE),
            catalogExercise("goblet_squat", MuscleGroup.QUADRICEPS, ExerciseMeasure.WEIGHT_AND_REPS, MovementPattern.SQUAT),
            catalogExercise("plank", MuscleGroup.ABDOMINALS, ExerciseMeasure.DURATION, MovementPattern.CORE)
        )
    )

    private val parser = GeneratedPlanParser(catalog)

    private val unbounded = PlanLimits(maxSetsPerSession = Int.MAX_VALUE)

    // Days arrive out of order: ordering is ours.
    private val good = GeneratedPlan(
        title = "Week 1",
        days = listOf(
            GeneratedDay(
                dayNumber = 3,
                title = "Cardio & Core",
                exercises = listOf(
                    GeneratedExercise(
                        exerciseKey = "warm_up_jog",
                        sets = listOf(GeneratedSet(seconds = 300))
                    ),
                    GeneratedExercise(
                        exerciseKey = "bicycle_crunch",
                        restSeconds = 30,
                        sets = listOf(GeneratedSet(reps = 20), GeneratedSet(reps = 20))
                    )
                )
            ),
            GeneratedDay(
                dayNumber = 1,
                title = "Full Body Strength",
                exercises = listOf(
                    GeneratedExercise(
                        exerciseKey = "goblet_squat",
                        restSeconds = 60,
                        sets = listOf(
                            GeneratedSet(reps = 12, weightKg = 20f),
                            GeneratedSet(reps = 11, weightKg = 20f),
                            GeneratedSet(reps = 10, weightKg = 22.5f)
                        )
                    )
                )
            )
        )
    )

    private fun parseGood(): WeeklyWorkoutPlan {
        val result = parser.parse(
            good,
            userId = 7,
            weekNumber = 2,
            startDateMillis = 1_753_056_000_000L,
            limits = unbounded
        )
        return (result as PlanParseResult.Parsed).plan
    }

    private fun errorsOf(plan: GeneratedPlan): List<String> {
        val result = parser.parse(plan, userId = 1, weekNumber = 1, startDateMillis = 0L, limits = unbounded)
        return (result as PlanParseResult.Invalid).errors
    }

    private fun GeneratedPlan.mapDays(change: (GeneratedDay) -> GeneratedDay) = copy(days = days.map(change))

    private fun GeneratedPlan.mapExercise(key: String, change: (GeneratedExercise) -> GeneratedExercise) =
        mapDays { day -> day.copy(exercises = day.exercises.map { if (it.exerciseKey == key) change(it) else it }) }

    private fun GeneratedExercise.withSet(index: Int, set: GeneratedSet) =
        copy(sets = sets.mapIndexed { i, it -> if (i == index) set else it })

    @Test
    fun theAppSuppliedFieldsLandOnThePlan() {
        val plan = parseGood()

        assertThat(plan.userId).isEqualTo(7)
        assertThat(plan.weekNumber).isEqualTo(2)
        assertThat(plan.startDateMillis).isEqualTo(1_753_056_000_000L)
        assertThat(plan.title).isEqualTo("Week 1")
    }

    @Test
    fun daysComeOutSortedByDayNumber() {
        assertThat(parseGood().workoutDays.map { it.dayNumber })
            .containsExactly(1, 3).inOrder()
    }

    // Five minutes of jogging, then two sets of twenty at three seconds a rep
    // with thirty seconds between them: eight minutes of work, plus the minute
    // spent walking from one to the other.
    @Test
    fun aDaysNumbersAreDerivedNotAccepted() {
        val cardio = parseGood().workoutDays.first { it.dayNumber == 3 }

        assertThat(cardio.duration).isEqualTo(9)
        assertThat(cardio.exerciseCount).isEqualTo(2)
        assertThat(cardio.duration)
            .isEqualTo(cardio.exercises.sumOf { it.durationMinutes } + 1)
    }

    @Test
    fun anExerciseArrivesWithEverythingItsCardShows() {
        val squat = parseGood().workoutDays.first { it.dayNumber == 1 }.exercises.single()

        assertThat(squat.exerciseKey).isEqualTo("goblet_squat")
        assertThat(squat.name).isEqualTo("goblet squat")
        assertThat(squat.measure).isEqualTo(ExerciseMeasure.WEIGHT_AND_REPS)
        assertThat(squat.durationMinutes).isEqualTo(4)
        assertThat(squat.restTime).isEqualTo(60)
        assertThat(squat.setCount).isEqualTo(3)
    }

    @Test
    fun setsAreNumberedInOrderAndCarryOnlyTargets() {
        val squatSets = parseGood().workoutDays.first { it.dayNumber == 1 }.exercises.single().sets

        assertThat(squatSets.map { it.setNumber }).containsExactly(1, 2, 3).inOrder()
        assertThat(squatSets.map { it.targetReps }).containsExactly(12, 11, 10).inOrder()
        assertThat(squatSets.last().targetWeightKg).isEqualTo(22.5f)
        squatSets.forEach {
            assertThat(it.actualReps).isNull()
            assertThat(it.actualWeightKg).isNull()
            assertThat(it.isCompleted).isFalse()
        }
    }

    @Test
    fun aFreshPlanStartsWithNothingDone() {
        parseGood().workoutDays.forEach { day ->
            assertThat(day.status).isEqualTo(WorkoutStatus.NOT_STARTED)
            assertThat(day.completedAt).isNull()
            day.exercises.forEach { exercise ->
                assertThat(exercise.isCompleted).isFalse()
                assertThat(exercise.videoTutorialUrl).isNull()
            }
        }
    }

    // How a movement is measured is a fact about the movement, so it comes
    // from the catalog, whatever the plan says.
    @Test
    fun theCatalogDecidesHowAMovementIsMeasured() {
        val plan = parseGood()
        val jog = plan.workoutDays.first { it.dayNumber == 3 }.exercises.first()
        val squat = plan.workoutDays.first { it.dayNumber == 1 }.exercises.single()

        assertThat(jog.measure).isEqualTo(ExerciseMeasure.DURATION)
        assertThat(squat.measure).isEqualTo(ExerciseMeasure.WEIGHT_AND_REPS)
    }

    // The name shown on the card is the catalog's, so a plan cannot invent a
    // movement that reads like one the app knows.
    @Test
    fun theCatalogNamesTheMovement() {
        val squat = parseGood().workoutDays.first { it.dayNumber == 1 }.exercises.single()

        assertThat(squat.name).isEqualTo("goblet squat")
    }

    // The day's kit is read off the catalog, so it can only name equipment
    // the movements actually need.
    @Test
    fun theDaysEquipmentComesFromItsMovements() {
        val parser = GeneratedPlanParser(
            InMemoryExerciseCatalog(
                listOf(
                    catalogExercise(
                        "goblet_squat", MuscleGroup.QUADRICEPS,
                        ExerciseMeasure.WEIGHT_AND_REPS, MovementPattern.SQUAT,
                        equipment = Equipment.DUMBBELL
                    ),
                    catalogExercise("warm_up_jog", MuscleGroup.CARDIO, ExerciseMeasure.DURATION, MovementPattern.CONDITIONING),
                    catalogExercise("bicycle_crunch", MuscleGroup.ABDOMINALS, ExerciseMeasure.REPS, MovementPattern.CORE)
                )
            )
        )
        val plan = (parser.parse(good, 7, 2, 0L, unbounded) as PlanParseResult.Parsed).plan

        assertThat(plan.workoutDays.first { it.dayNumber == 1 }.equipment)
            .containsExactly("Dumbbell")
        assertThat(plan.workoutDays.first { it.dayNumber == 3 }.equipment).isEmpty()
    }

    @Test
    fun aStrayTargetTheMeasureDoesNotRenderIsStripped() {
        val plan = parser.parse(
            good.mapExercise("bicycle_crunch") { it.withSet(0, GeneratedSet(reps = 20, weightKg = 8f, seconds = 40)) },
            userId = 1, weekNumber = 1, startDateMillis = 0L, limits = unbounded
        )

        val stripped = (plan as PlanParseResult.Parsed).plan
            .workoutDays.first { it.dayNumber == 3 }
            .exercises.first { it.exerciseKey == "bicycle_crunch" }.sets.first()
        assertThat(stripped.targetReps).isEqualTo(20)
        assertThat(stripped.targetWeightKg).isNull()
        assertThat(stripped.targetSeconds).isNull()
    }

    @Test
    fun aBlankTitleAndNoDaysAreBothReported() {
        val errors = errorsOf(GeneratedPlan(title = " ", days = emptyList()))

        assertThat(errors).containsExactly("plan: title is blank", "plan: has no days")
    }

    @Test
    fun aRepeatedDayNumberIsRejected() {
        val errors = errorsOf(good.mapDays { if (it.dayNumber == 3) it.copy(dayNumber = 1) else it })

        assertThat(errors).containsExactly("plan: day 1 appears more than once")
    }

    @Test
    fun aDayNumberOutsideTheWeekIsRejected() {
        val errors = errorsOf(good.mapDays { if (it.dayNumber == 3) it.copy(dayNumber = 8) else it })

        assertThat(errors).containsExactly("day 8: dayNumber must be 1..7, Monday to Sunday")
    }

    @Test
    fun anExerciseKeyThatIsNotASlugIsRejected() {
        val errors = errorsOf(good.mapExercise("goblet_squat") { it.copy(exerciseKey = "Goblet Squat") })

        assertThat(errors).containsExactly(
            "day 1, Goblet Squat: exerciseKey 'Goblet Squat' is not a lower_snake_case slug"
        )
    }

    @Test
    fun theSameExerciseTwiceInOneDayIsRejected() {
        val errors = errorsOf(good.mapExercise("warm_up_jog") { it.copy(exerciseKey = "bicycle_crunch") })

        assertThat(errors).contains("day 3: exerciseKey 'bicycle_crunch' appears more than once")
    }

    @Test
    fun aSetMissingTheTargetItsMeasureNeedsIsRejected() {
        val repsErrors = errorsOf(good.mapExercise("goblet_squat") { it.withSet(0, GeneratedSet()) })
        val secondsErrors = errorsOf(good.mapExercise("warm_up_jog") { it.withSet(0, GeneratedSet(reps = 300)) })

        assertThat(repsErrors)
            .containsExactly("day 1, goblet_squat, set 1: needs reps between 1 and 100")
        assertThat(secondsErrors)
            .containsExactly("day 3, warm_up_jog, set 1: needs seconds between 5 and 5400")
    }

    @Test
    fun numbersNoClientCouldPerformAreRejected() {
        assertThat(errorsOf(good.mapExercise("bicycle_crunch") { it.copy(restSeconds = -30) }))
            .containsExactly("day 3, bicycle_crunch: restSeconds must be 5..600")
        assertThat(errorsOf(good.mapExercise("goblet_squat") { it.withSet(2, GeneratedSet(reps = 10, weightKg = 0f)) }))
            .containsExactly("day 1, goblet_squat, set 3: weightKg must be between 0.5 and 500.0")
        assertThat(errorsOf(good.mapExercise("goblet_squat") { it.withSet(0, GeneratedSet(reps = 400)) }))
            .containsExactly("day 1, goblet_squat, set 1: needs reps between 1 and 100")
    }

    // A session is a time budget: nine sets is not something half an hour of
    // heavy work pays for.
    @Test
    fun aDayThatOverspendsTheSessionIsRejected() {
        val result = parser.parse(
            good,
            userId = 7,
            weekNumber = 2,
            startDateMillis = 1_753_056_000_000L,
            limits = PlanLimits(maxSetsPerSession = 2)
        )

        assertThat((result as PlanParseResult.Invalid).errors).containsExactly(
            "day 1: has 3 sets but the client's session length allows at most 2, " +
                "warm-up included",
            "day 3: has 3 sets but the client's session length allows at most 2, " +
                "warm-up included"
        )
    }

    // The set cap is a proxy for time and a timed set breaks it: three
    // half-hour walks are three sets and a ninety-minute day.
    @Test
    fun aDayLongerThanTheAnsweredSessionIsRejected() {
        val walk = GeneratedPlan(
            title = "Conditioning",
            days = listOf(
                GeneratedDay(
                    dayNumber = 1,
                    title = "Easy Miles",
                    exercises = listOf(
                        GeneratedExercise(
                            exerciseKey = "warm_up_jog",
                            restSeconds = 60,
                            sets = List(3) { GeneratedSet(seconds = 1800) }
                        )
                    )
                )
            )
        )

        val result = parser.parse(
            walk,
            userId = 7,
            weekNumber = 1,
            startDateMillis = 1_753_056_000_000L,
            limits = PlanLimits(
                maxSetsPerSession = 20,
                sessionMinutes = 45,
                sessionCeilingMinutes = 67
            )
        )

        assertThat((result as PlanParseResult.Invalid).errors).containsExactly(
            "day 1: runs about 92 minutes of work and rest, and the client asked for about 45"
        )
    }

    @Test
    fun anExerciseWithNoSetsIsRejected() {
        val errors = errorsOf(good.mapExercise("warm_up_jog") { it.copy(sets = emptyList()) })

        assertThat(errors).containsExactly("day 3, warm_up_jog: has no sets")
    }

    // A rejected week names every problem it has, not just the first.
    @Test
    fun everyProblemIsReportedNotJustTheFirst() {
        val errors = errorsOf(
            good.mapExercise("bicycle_crunch") { it.copy(restSeconds = -30) }
                .mapExercise("goblet_squat") { it.withSet(2, GeneratedSet(reps = 10, weightKg = 0f)) }
        )

        assertThat(errors).containsExactly(
            "day 1, goblet_squat, set 3: weightKg must be between 0.5 and 500.0",
            "day 3, bicycle_crunch: restSeconds must be 5..600"
        )
    }
}
