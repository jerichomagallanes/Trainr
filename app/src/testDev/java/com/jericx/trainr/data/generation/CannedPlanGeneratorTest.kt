package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Test

// Lives in the dev unit-test source set because the generator it covers is only
// compiled into the dev flavour.
class CannedPlanGeneratorTest {

    private fun request(daysPerWeek: Int = 3, weekNumber: Int = 1, previous: Any? = null) =
        PlanRequest(
            user = UserProfile(id = 1, firstName = "Jericho", workoutDaysPerWeek = daysPerWeek),
            weekNumber = weekNumber,
            startDateMillis = 1_000L,
            languageCode = "en"
        )

    private suspend fun plan(daysPerWeek: Int = 3) =
        (CannedPlanGenerator().generate(request(daysPerWeek)) as PlanGenerationResult.Generated).plan

    // The parser rejects a week with the wrong number of days, and so does the
    // generator's own check, so a canned week that ignored the profile would
    // fail the same way a bad answer from the model does.
    @Test
    fun itHonoursTheNumberOfDaysAsked() = runTest {
        for (days in 1..7) {
            assertThat(plan(days).workoutDays).hasSize(days)
        }
    }

    @Test
    fun itSpacesSessionsAcrossTheWeekWithoutRepeatingADay() = runTest {
        val slots = plan(daysPerWeek = 3).workoutDays.map { it.dayNumber }

        assertThat(slots).containsNoDuplicates()
        assertThat(slots).isInOrder()
        assertThat(slots.all { it in 1..7 }).isTrue()
    }

    // Every key has to be one the catalog knows, or the tutorials that make the
    // dev build worth looking at simply do not render.
    @Test
    fun everyExerciseUsesAKeyTheCatalogKnows() = runTest {
        val keys = plan().workoutDays.flatMap { it.exercises }.map { it.exerciseKey }

        assertThat(keys).isNotEmpty()
        // The same movement recurs across days, so compare the distinct set.
        assertThat(ExerciseVideoCatalog.videoIds.keys).containsAtLeastElementsIn(keys.distinct())
    }

    @Test
    fun itCarriesTheRequestsWeekAndStartDate() = runTest {
        val result = CannedPlanGenerator().generate(request(weekNumber = 4))
        val plan = (result as PlanGenerationResult.Generated).plan

        assertThat(plan.weekNumber).isEqualTo(4)
        assertThat(plan.startDateMillis).isEqualTo(1_000L)
        assertThat(plan.userId).isEqualTo(1)
    }

    // A canned week that never moved would make progression impossible to look
    // at while building the screens that show it.
    @Test
    fun itProgressesFromTheWeekBefore() = runTest {
        val first = plan()
        val second = (
            CannedPlanGenerator().generate(
                PlanRequest(
                    user = UserProfile(id = 1, workoutDaysPerWeek = 3),
                    weekNumber = 2,
                    startDateMillis = 2_000L,
                    languageCode = "en",
                    previousWeek = first
                )
            ) as PlanGenerationResult.Generated
        ).plan

        val before = first.workoutDays.first().exercises
            .first { it.exerciseKey == "goblet_squat" }.sets.first().targetWeightKg!!
        val after = second.workoutDays.first().exercises
            .first { it.exerciseKey == "goblet_squat" }.sets.first().targetWeightKg!!

        assertThat(after).isGreaterThan(before)
    }

    @Test
    fun itNeverFails() = runTest {
        assertThat(CannedPlanGenerator().generate(request()))
            .isInstanceOf(PlanGenerationResult.Generated::class.java)
    }
}
