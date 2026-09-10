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

    // Days arrive out of order and carry an unknown key: ordering is ours, unknown keys are ignored
    private val goodJson = """
        {
          "title": "Week 1",
          "coachNote": "an extra key the contract does not define",
          "days": [
            {
              "dayNumber": 3,
              "title": "Cardio & Core",
              "exercises": [
                {
                  "exerciseKey": "warm_up_jog",
                  "prescription": "5 minutes",
                  "instructions": "Light jogging in place to warm up.",
                  "sets": [{ "seconds": 300 }]
                },
                {
                  "exerciseKey": "bicycle_crunch",
                  "prescription": "2 sets of 20 reps",
                  "instructions": "Alternate elbow to knee.",
                  "restSeconds": 30,
                  "sets": [{ "reps": 20 }, { "reps": 20 }]
                }
              ]
            },
            {
              "dayNumber": 1,
              "title": "Full Body Strength",
              "exercises": [
                {
                  "exerciseKey": "goblet_squat",
                  "prescription": "3 sets of 12 reps",
                  "instructions": "Squat holding a dumbbell at your chest.",
                  "restSeconds": 60,
                  "sets": [
                    { "reps": 12, "weightKg": 20 },
                    { "reps": 11, "weightKg": 20 },
                    { "reps": 10, "weightKg": 22.5 }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private fun parseGood(): WeeklyWorkoutPlan {
        val result = parser.parse(
            goodJson,
            userId = 7,
            weekNumber = 2,
            startDateMillis = 1_753_056_000_000L
        )
        return (result as PlanParseResult.Parsed).plan
    }

    private fun errorsOf(json: String): List<String> {
        val result = parser.parse(json, userId = 1, weekNumber = 1, startDateMillis = 0L)
        return (result as PlanParseResult.Invalid).errors
    }

    private fun goodJsonWith(from: String, to: String): String {
        assertThat(goodJson).contains(from)
        return goodJson.replace(from, to)
    }

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
    // spent walking from one to the other, whatever the model would have
    // claimed.
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
        assertThat(squat.prescription).isEqualTo("3 sets of 12 reps")
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
    // from the catalog and the model never gets to disagree with it.
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

    // Asking a model to restate the day's kit only gave it a way to name
    // equipment the client does not own.
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
        val plan = (parser.parse(goodJson, 7, 2, 0L) as PlanParseResult.Parsed).plan

        assertThat(plan.workoutDays.first { it.dayNumber == 1 }.equipment)
            .containsExactly("Dumbbell")
        assertThat(plan.workoutDays.first { it.dayNumber == 3 }.equipment).isEmpty()
    }

    @Test
    fun aStrayTargetTheMeasureDoesNotRenderIsStripped() {
        val plan = parser.parse(
            goodJsonWith("{ \"reps\": 20 },", "{ \"reps\": 20, \"weightKg\": 8, \"seconds\": 40 },"),
            userId = 1, weekNumber = 1, startDateMillis = 0L
        )

        val stripped = (plan as PlanParseResult.Parsed).plan
            .workoutDays.first { it.dayNumber == 3 }
            .exercises.first { it.exerciseKey == "bicycle_crunch" }.sets.first()
        assertThat(stripped.targetReps).isEqualTo(20)
        assertThat(stripped.targetWeightKg).isNull()
        assertThat(stripped.targetSeconds).isNull()
    }

    @Test
    fun malformedJsonIsInvalidNotAnException() {
        assertThat(errorsOf("here is your plan! { \"title\": ")).hasSize(1)
        assertThat(errorsOf("{}")).isNotEmpty()
    }

    @Test
    fun aBlankTitleAndNoDaysAreBothReported() {
        val errors = errorsOf("""{ "title": " ", "days": [] }""")

        assertThat(errors).containsExactly("plan: title is blank", "plan: has no days")
    }

    @Test
    fun aRepeatedDayNumberIsRejected() {
        val errors = errorsOf(goodJsonWith("\"dayNumber\": 3,", "\"dayNumber\": 1,"))

        assertThat(errors).containsExactly("plan: day 1 appears more than once")
    }

    @Test
    fun aDayNumberOutsideTheWeekIsRejected() {
        val errors = errorsOf(goodJsonWith("\"dayNumber\": 3,", "\"dayNumber\": 8,"))

        assertThat(errors).containsExactly("day 8: dayNumber must be 1..7, Monday to Sunday")
    }

    @Test
    fun anExerciseKeyThatIsNotASlugIsRejected() {
        val errors = errorsOf(goodJsonWith("goblet_squat", "Goblet Squat"))

        assertThat(errors).containsExactly(
            "day 1, Goblet Squat: exerciseKey 'Goblet Squat' is not a lower_snake_case slug"
        )
    }

    @Test
    fun theSameExerciseTwiceInOneDayIsRejected() {
        val errors = errorsOf(goodJsonWith("warm_up_jog", "bicycle_crunch"))

        assertThat(errors).contains("day 3: exerciseKey 'bicycle_crunch' appears more than once")
    }

    @Test
    fun aSetMissingTheTargetItsMeasureNeedsIsRejected() {
        val repsErrors = errorsOf(goodJsonWith("{ \"reps\": 12, \"weightKg\": 20 },", "{},"))
        val secondsErrors = errorsOf(goodJsonWith("{ \"seconds\": 300 }", "{ \"reps\": 300 }"))

        assertThat(repsErrors)
            .containsExactly("day 1, goblet_squat, set 1: needs reps between 1 and 100")
        assertThat(secondsErrors)
            .containsExactly("day 3, warm_up_jog, set 1: needs seconds between 5 and 5400")
    }

    @Test
    fun numbersNoClientCouldPerformAreRejected() {
        assertThat(errorsOf(goodJsonWith("\"restSeconds\": 30,", "\"restSeconds\": -30,")))
            .containsExactly("day 3, bicycle_crunch: restSeconds must be 5..600")
        assertThat(errorsOf(goodJsonWith("\"weightKg\": 22.5", "\"weightKg\": 0")))
            .containsExactly("day 1, goblet_squat, set 3: weightKg must be between 0.5 and 500.0")
        assertThat(errorsOf(goodJsonWith("{ \"reps\": 12, \"weightKg\": 20 },", "{ \"reps\": 400 },")))
            .containsExactly("day 1, goblet_squat, set 1: needs reps between 1 and 100")
    }

    // A session is a time budget: nine sets is not something half an hour of
    // heavy work pays for, and the model is told so before it is asked again.
    @Test
    fun aDayThatOverspendsTheSessionIsRejected() {
        val result = parser.parse(
            goodJson,
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
        val walk = """
            {
              "title": "Conditioning",
              "days": [{
                "dayNumber": 1,
                "title": "Easy Miles",
                "exercises": [{
                  "exerciseKey": "warm_up_jog",
                  "prescription": "3 x 30 minutes",
                  "instructions": "Keep the pace conversational throughout.",
                  "restSeconds": 60,
                  "sets": [
                    { "seconds": 1800 }, { "seconds": 1800 }, { "seconds": 1800 }
                  ]
                }]
              }]
            }
        """.trimIndent()

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
        val errors = errorsOf(goodJsonWith("\"sets\": [{ \"seconds\": 300 }]", "\"sets\": []"))

        assertThat(errors).containsExactly("day 3, warm_up_jog: has no sets")
    }

    @Test
    fun everyProblemIsReportedNotJustTheFirst() {
        val errors = errorsOf(
            goodJsonWith("\"prescription\": \"5 minutes\"", "\"prescription\": \"\"")
                .replace("\"instructions\": \"Alternate elbow to knee.\"", "\"instructions\": \" \"")
        )

        assertThat(errors).containsExactly(
            "day 3, warm_up_jog: prescription is blank",
            "day 3, bicycle_crunch: instructions are blank"
        )
    }

    @Test
    fun theContractDocumentsOwnExampleParses() {
        val squats = """
            {
              "exerciseKey": "goblet_squat",
              "prescription": "3 sets of 12 reps",
              "instructions": "Squat holding a dumbbell at your chest to build the legs and brace the core.",
              "restSeconds": 60,
              "sets": [
                { "reps": 12, "weightKg": 20 },
                { "reps": 12, "weightKg": 20 },
                { "reps": 12, "weightKg": 20 }
              ]
            }
        """.trimIndent()
        val plank = """
            {
              "exerciseKey": "plank",
              "prescription": "3 sets of 45 seconds",
              "instructions": "Hold a straight line from head to heels to brace the whole core.",
              "sets": [{ "seconds": 45 }, { "seconds": 45 }, { "seconds": 45 }]
            }
        """.trimIndent()
        val example = """
            {
              "title": "Week 1",
              "days": [
                {
                  "dayNumber": 1,
                  "title": "Full Body Strength",
                  "exercises": [$squats, $plank]
                }
              ]
            }
        """.trimIndent()

        val result = parser.parse(example, userId = 1, weekNumber = 1, startDateMillis = 0L)

        assertThat(result).isInstanceOf(PlanParseResult.Parsed::class.java)
    }
}
