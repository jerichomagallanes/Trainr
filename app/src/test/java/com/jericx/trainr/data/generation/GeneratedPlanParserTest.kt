package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutStatus
import org.junit.Test

class GeneratedPlanParserTest {

    private val parser = GeneratedPlanParser()

    // Days arrive out of order and carry a key the schema doesn't know, to pin
    // that ordering is ours and unknown keys are ignored.
    private val goodJson = """
        {
          "title": "Week 1",
          "coachNote": "an extra key the contract does not define",
          "days": [
            {
              "dayNumber": 3,
              "title": "Cardio & Core",
              "equipment": ["Yoga Mat"],
              "exercises": [
                {
                  "exerciseKey": "warm_up_jog",
                  "name": "Warm-up jog",
                  "measure": "DURATION",
                  "durationMinutes": 5,
                  "prescription": "5 minutes",
                  "instructions": "Light jogging in place to warm up.",
                  "sets": [{ "seconds": 300 }]
                },
                {
                  "exerciseKey": "bicycle_crunch",
                  "name": "Bicycle Crunches",
                  "measure": "REPS",
                  "durationMinutes": 4,
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
              "equipment": ["Dumbbells"],
              "exercises": [
                {
                  "exerciseKey": "goblet_squat",
                  "name": "Goblet Squats",
                  "measure": "WEIGHT_AND_REPS",
                  "durationMinutes": 8,
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

    @Test
    fun aDaysNumbersAreDerivedNotAccepted() {
        val cardio = parseGood().workoutDays.first { it.dayNumber == 3 }

        assertThat(cardio.duration).isEqualTo(9)
        assertThat(cardio.exerciseCount).isEqualTo(2)
    }

    @Test
    fun anExerciseArrivesWithEverythingItsCardShows() {
        val squat = parseGood().workoutDays.first { it.dayNumber == 1 }.exercises.single()

        assertThat(squat.exerciseKey).isEqualTo("goblet_squat")
        assertThat(squat.name).isEqualTo("Goblet Squats")
        assertThat(squat.measure).isEqualTo(ExerciseMeasure.WEIGHT_AND_REPS)
        assertThat(squat.durationMinutes).isEqualTo(8)
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

    @Test
    fun anUnknownMeasureDegradesToReps() {
        val plan = parser.parse(
            goodJsonWith("\"measure\": \"REPS\"", "\"measure\": \"DISTANCE\""),
            userId = 1, weekNumber = 1, startDateMillis = 0L
        )

        val crunches = (plan as PlanParseResult.Parsed).plan
            .workoutDays.first { it.dayNumber == 3 }
            .exercises.first { it.exerciseKey == "bicycle_crunch" }
        assertThat(crunches.measure).isEqualTo(ExerciseMeasure.REPS)
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

        assertThat(errors).containsExactly("day 3: exerciseKey 'bicycle_crunch' appears more than once")
    }

    @Test
    fun aSetMissingTheTargetItsMeasureNeedsIsRejected() {
        val repsErrors = errorsOf(goodJsonWith("{ \"reps\": 12, \"weightKg\": 20 },", "{},"))
        val secondsErrors = errorsOf(goodJsonWith("{ \"seconds\": 300 }", "{ \"reps\": 300 }"))

        assertThat(repsErrors).containsExactly("day 1, goblet_squat, set 1: needs reps above zero")
        assertThat(secondsErrors)
            .containsExactly("day 3, warm_up_jog, set 1: needs seconds above zero")
    }

    @Test
    fun nonPositiveNumbersAreRejected() {
        assertThat(errorsOf(goodJsonWith("\"durationMinutes\": 5,", "\"durationMinutes\": 0,")))
            .containsExactly("day 3, warm_up_jog: durationMinutes must be above zero")
        assertThat(errorsOf(goodJsonWith("\"restSeconds\": 30,", "\"restSeconds\": -30,")))
            .containsExactly("day 3, bicycle_crunch: restSeconds must be above zero")
        assertThat(errorsOf(goodJsonWith("\"weightKg\": 22.5", "\"weightKg\": 0")))
            .containsExactly("day 1, goblet_squat, set 3: weightKg must be above zero")
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
              "name": "Goblet Squats",
              "measure": "WEIGHT_AND_REPS",
              "durationMinutes": 8,
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
              "name": "Plank",
              "measure": "DURATION",
              "durationMinutes": 6,
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
                  "equipment": ["Dumbbells", "Yoga Mat"],
                  "exercises": [$squats, $plank]
                }
              ]
            }
        """.trimIndent()

        val result = parser.parse(example, userId = 1, weekNumber = 1, startDateMillis = 0L)

        assertThat(result).isInstanceOf(PlanParseResult.Parsed::class.java)
    }
}
