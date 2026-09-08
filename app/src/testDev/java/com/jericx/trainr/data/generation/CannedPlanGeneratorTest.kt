package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Test

// In the dev unit-test source set: the generator it covers is dev-only.
class CannedPlanGeneratorTest {

    private fun request(
        daysPerWeek: Int = 3,
        weekNumber: Int = 1,
        duration: Int = 45,
        equipment: List<Equipment> = listOf(Equipment.DUMBBELLS)
    ) = PlanRequest(
        user = UserProfile(
            id = 1,
            firstName = "Jericho",
            workoutDaysPerWeek = daysPerWeek,
            workoutDuration = duration,
            availableEquipment = equipment
        ),
        weekNumber = weekNumber,
        startDateMillis = 1_000L,
        languageCode = "en"
    )

    private suspend fun plan(
        daysPerWeek: Int = 3,
        duration: Int = 45,
        equipment: List<Equipment> = listOf(Equipment.DUMBBELLS)
    ) = (
        CannedPlanGenerator().generate(request(daysPerWeek, duration = duration, equipment = equipment))
            as PlanGenerationResult.Generated
        ).plan

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

    @Test
    fun everyExerciseUsesAKeyTheCatalogKnows() = runTest {
        val keys = plan().workoutDays.flatMap { it.exercises }.map { it.exerciseKey }

        assertThat(keys).isNotEmpty()
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

    @Test
    fun itProgressesFromTheWeekBefore() = runTest {
        val first = plan()
        val second = (
            CannedPlanGenerator().generate(
                PlanRequest(
                    user = UserProfile(
                        id = 1,
                        workoutDaysPerWeek = 3,
                        availableEquipment = listOf(Equipment.DUMBBELLS)
                    ),
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

    @Test
    fun itFillsTheSessionLengthThatWasAskedFor() = runTest {
        for (requested in listOf(30, 45, 60, 90)) {
            val day = plan(duration = requested).workoutDays.first()

            assertThat(day.duration).isEqualTo(requested)
            assertThat(day.exercises.sumOf { it.durationMinutes }).isEqualTo(requested)
        }
    }

    @Test
    fun itOnlyPrescribesMovementsTheClientHasTheKitFor() = runTest {
        val keys = plan(equipment = listOf(Equipment.NONE))
            .workoutDays.flatMap { it.exercises }.map { it.exerciseKey }.distinct()

        assertThat(keys).isNotEmpty()
        assertThat(keys).containsNoneOf("goblet_squat", "dumbbell_floor_press", "overhead_press")
    }

    @Test
    fun aLoadedProfileStillGetsLoadedWork() = runTest {
        val keys = plan(equipment = listOf(Equipment.BARBELL))
            .workoutDays.flatMap { it.exercises }.map { it.exerciseKey }.distinct()

        assertThat(keys).contains("bent_over_row")
    }

    @Test
    fun itNamesOnlyTheEquipmentTheDayActuallyNeeds() = runTest {
        val loaded = plan(
            equipment = listOf(Equipment.DUMBBELLS, Equipment.SQUAT_RACK)
        ).workoutDays.first()
        assertThat(loaded.equipment).containsExactly("Dumbbells")

        val bodyweight = plan(equipment = listOf(Equipment.NONE)).workoutDays.first()
        assertThat(bodyweight.equipment).containsExactly("Bodyweight")
    }

    @Test
    fun theExerciseCountMatchesTheExercises() = runTest {
        for (day in plan(duration = 60).workoutDays) {
            assertThat(day.exerciseCount).isEqualTo(day.exercises.size)
        }
    }
}
