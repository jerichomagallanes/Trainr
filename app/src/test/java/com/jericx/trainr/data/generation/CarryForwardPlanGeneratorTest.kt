package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSource
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CarryForwardPlanGeneratorTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())

    private class Recording : PlanGenerator {
        val asked = mutableListOf<PlanRequest>()
        override suspend fun generate(request: PlanRequest): PlanGenerationResult {
            asked += request
            return PlanGenerationResult.Failed
        }
    }

    private fun user(kit: List<Equipment> = Equipment.entries.toList(), days: Int = 3) = UserProfile(
        id = 7, age = 30, weight = 80f, fitnessGoal = FitnessGoal.MUSCLE_GAIN,
        experienceLevel = ExperienceLevel.INTERMEDIATE, availableEquipment = kit,
        workoutDaysPerWeek = days, workoutDuration = 45
    )

    private fun firstWeek(user: UserProfile = user()) = runBlocking {
        (TemplatePlanGenerator(catalog).generate(PlanRequest(user, 1, 0L)) as PlanGenerationResult.Generated).plan
    }

    private fun request(user: UserProfile, history: List<WeeklyWorkoutPlan>, freshCast: Boolean = false) = PlanRequest(
        user = user,
        weekNumber = history.size + 1,
        startDateMillis = history.size * 7 * DAY,
        history = history.sortedByDescending { it.weekNumber },
        freshCast = freshCast
    )

    private fun carry(request: PlanRequest, next: PlanGenerator = Recording()) =
        runBlocking { CarryForwardPlanGenerator(catalog, next).generate(request) }

    private fun WeeklyWorkoutPlan.movements() = workoutDays.map { day -> day.exercises.map { it.exerciseKey } }

    private fun WeeklyWorkoutPlan.logged(which: (Int) -> Boolean = { true }) = copy(
        workoutDays = workoutDays.map { day ->
            day.copy(
                exercises = day.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.mapIndexed { index, set ->
                            if (!which(index)) set else set.copy(
                                actualReps = set.targetReps, actualWeightKg = set.targetWeightKg,
                                actualSeconds = set.targetSeconds, isCompleted = true
                            )
                        }
                    )
                }
            )
        }
    )

    @Test
    fun aWeekNothingForcesToChangeIsLastWeeksMovementsWithNoModelAsked() {
        val first = firstWeek().logged()
        val next = Recording()

        val result = carry(request(user(), listOf(first)), next) as PlanGenerationResult.Generated

        assertThat(result.source).isEqualTo(PlanSource.PROGRESSED)
        assertThat(result.plan.movements()).isEqualTo(first.movements())
        assertThat(result.plan.workoutDays.map { it.title }).isEqualTo(first.workoutDays.map { it.title })
        assertThat(next.asked).isEmpty()
    }

    @Test
    fun aWeekDoneInFullIsCarriedForwardHarder() {
        val first = firstWeek().logged()

        val second = (carry(request(user(), listOf(first))) as PlanGenerationResult.Generated).plan
        val before = first.workoutDays.flatMap { it.exercises }.associateBy { it.exerciseKey }
        val climbed = second.workoutDays.flatMap { it.exercises }.count { exercise ->
            val now = exercise.sets.first()
            val then = before.getValue(exercise.exerciseKey).sets.first()
            (now.targetReps ?: 0) > (then.targetReps ?: 0) ||
                (now.targetWeightKg ?: 0f) > (then.targetWeightKg ?: 0f) ||
                (now.targetSeconds ?: 0) > (then.targetSeconds ?: 0)
        }

        assertThat(climbed).isGreaterThan(0)
    }

    // Kit given up takes its movements with it, so the week is chosen afresh.
    @Test
    fun aProfileEditThatRulesAMovementOutHandsTheWeekOn() {
        val next = Recording()

        carry(request(user(kit = listOf(Equipment.NONE)), listOf(firstWeek().logged())), next)

        assertThat(next.asked).hasSize(1)
    }

    @Test
    fun aDifferentNumberOfDaysHandsTheWeekOn() {
        val next = Recording()

        carry(request(user(days = 4), listOf(firstWeek().logged())), next)

        assertThat(next.asked).hasSize(1)
    }

    @Test
    fun askingForNewMovementsIsNeverAnsweredWithLastWeeks() {
        val next = Recording()

        carry(request(user(), listOf(firstWeek().logged()), freshCast = true), next)

        assertThat(next.asked.single().freshCast).isTrue()
    }

    @Test
    fun aFirstWeekHasNothingToCarry() {
        val next = Recording()

        carry(PlanRequest(user(), 1, 0L), next)

        assertThat(next.asked).hasSize(1)
    }

    // Six weeks without a lighter one, and most sets left undone: a lighter
    // week is due, and it keeps the movements while cutting the work.
    @Test
    fun aDeloadWeekKeepsTheMovementsAndCutsTheSets() {
        val week = firstWeek()
        val history = (1..6).map { number ->
            week.copy(weekNumber = number, startDateMillis = (number - 1) * 7 * DAY).logged { it == 0 }
        }

        val deload = (carry(request(user(), history)) as PlanGenerationResult.Generated).plan

        assertThat(deload.movements()).isEqualTo(week.movements())
        assertThat(deload.workoutDays.sumOf { day -> day.exercises.sumOf { it.sets.size } })
            .isLessThan(week.workoutDays.sumOf { day -> day.exercises.sumOf { it.sets.size } })
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
