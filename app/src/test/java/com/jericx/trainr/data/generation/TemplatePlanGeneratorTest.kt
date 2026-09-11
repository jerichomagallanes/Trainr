package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.PatternRequirement
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TemplatePlanGeneratorTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val generator = TemplatePlanGenerator(catalog)

    private fun user(
        goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
        days: Int = 3,
        minutes: Int = 45,
        kit: List<Equipment> = Equipment.entries.toList(),
        injuries: List<Injury> = emptyList(),
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
        units: UnitSystem = UnitSystem.METRIC
    ) = UserProfile(
        id = 7, age = 30, weight = 80f, fitnessGoal = goal, workoutDaysPerWeek = days,
        workoutDuration = minutes, availableEquipment = kit, injuries = injuries,
        experienceLevel = experience, liftingUnitSystem = units
    )

    private fun generate(user: UserProfile, history: List<WeeklyWorkoutPlan> = emptyList(), week: Int = 1) =
        runBlocking { generator.generate(PlanRequest(user, week, (week - 1) * 7 * DAY, history)) }

    private fun planFor(user: UserProfile, history: List<WeeklyWorkoutPlan> = emptyList(), week: Int = 1) =
        (generate(user, history, week) as PlanGenerationResult.Generated).plan

    // The whole point: every answer the setup screen allows gets a week, and
    // every such week passes the same parser, with the same limits, that a
    // model's answer has to.
    @Test
    fun everyAnswerTheSetupScreenAllowsBuildsAWeekTheParserAccepts() {
        val kits = listOf(
            listOf(Equipment.NONE), listOf(Equipment.DUMBBELL),
            listOf(Equipment.BARBELL, Equipment.DUMBBELL), Equipment.entries.toList()
        )
        FitnessGoal.entries.forEach { goal ->
            listOf(30, 90).forEach { minutes ->
                (1..7).forEach { days ->
                    listOf(ExperienceLevel.BEGINNER, ExperienceLevel.ADVANCED).forEach { experience ->
                        kits.forEach { kit ->
                            listOf(emptyList(), Injury.entries).forEach { injuries ->
                                UnitSystem.entries.forEach { units ->
                                    val answer = user(goal, days, minutes, kit, injuries, experience, units)
                                    val result = generate(answer)
                                    assertWithMessage(
                                        "$goal ${days}d ${minutes}m $experience $kit ${injuries.size} injuries $units"
                                    ).that(result).isInstanceOf(PlanGenerationResult.Generated::class.java)
                                    assertThat((result as PlanGenerationResult.Generated).plan.workoutDays)
                                        .hasSize(days)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun aClientWithNoEquipmentStillSquatsPressesAndPulls() {
        val patterns = planFor(user(kit = listOf(Equipment.NONE))).workoutDays
            .flatMap { it.exercises }
            .mapNotNull { catalog[it.exerciseKey]?.pattern }

        PatternRequirement.entries.forEach { requirement ->
            assertWithMessage(requirement.name).that(patterns.any(requirement::isMetBy)).isTrue()
        }
    }

    @Test
    fun theSameAnswersAlwaysBuildTheSameWeek() {
        val first = planFor(user())
        val second = planFor(user())

        assertThat(second.title).isEqualTo(first.title)
        assertThat(second.workoutDays).isEqualTo(first.workoutDays)
    }

    // Asking for more time never buys a shorter session. Where the catalog
    // runs out of things to fill a long one it plateaus; it never shrinks.
    @Test
    fun aLongerAnswerNeverGetsAShorterSession() {
        val kits = listOf(listOf(Equipment.NONE), listOf(Equipment.DUMBBELL), Equipment.entries.toList())
        FitnessGoal.entries.forEach { goal ->
            kits.forEach { kit ->
                listOf(3, 5).forEach { days ->
                    val shortest = listOf(30, 45, 60, 90).map { minutes ->
                        planFor(user(goal, days, minutes, kit)).workoutDays.minOf { it.duration }
                    }
                    assertWithMessage("$goal $kit ${days}d").that(shortest).isInOrder()
                }
            }
        }
    }

    // Weight loss and endurance take the rest of the session as conditioning,
    // so their sessions are the length that was asked for, never past it and
    // never a fraction of it.
    @Test
    fun aWeightLossSessionIsAboutTheLengthThatWasAskedFor() {
        listOf(FitnessGoal.WEIGHT_LOSS, FitnessGoal.ENDURANCE).forEach { goal ->
            listOf(listOf(Equipment.NONE), Equipment.entries.toList()).forEach { kit ->
                listOf(30, 45, 60, 90).forEach { minutes ->
                    planFor(user(goal, 3, minutes, kit)).workoutDays.forEach { day ->
                        assertWithMessage("$goal $kit ${minutes}m day ${day.dayNumber}")
                            .that(day.duration).isIn(minutes * 3 / 4..minutes)
                    }
                }
            }
        }
    }

    // A week done in full is progressed from: the second week is not the
    // first week again.
    @Test
    fun aSecondWeekClimbsFromAFirstWeekDoneInFull() {
        val first = planFor(user())
        val done = first.copy(
            workoutDays = first.workoutDays.map { day ->
                day.copy(
                    exercises = day.exercises.map { exercise ->
                        exercise.copy(
                            sets = exercise.sets.map {
                                it.copy(
                                    actualReps = it.targetReps, actualWeightKg = it.targetWeightKg,
                                    actualSeconds = it.targetSeconds, isCompleted = true
                                )
                            }
                        )
                    }
                )
            }
        )

        val second = planFor(user(), history = listOf(done), week = 2)
        val before = done.workoutDays.flatMap { it.exercises }.associateBy { it.exerciseKey }
        val climbed = second.workoutDays.flatMap { it.exercises }.count { exercise ->
            val previous = before[exercise.exerciseKey] ?: return@count false
            val now = exercise.sets.first()
            val then = previous.sets.first()
            (now.targetReps ?: 0) > (then.targetReps ?: 0) ||
                (now.targetWeightKg ?: 0f) > (then.targetWeightKg ?: 0f) ||
                (now.targetSeconds ?: 0) > (then.targetSeconds ?: 0)
        }

        assertThat(climbed).isGreaterThan(0)
    }

    // With no movements there is nothing to build, and it says so rather
    // than handing over an empty week.
    @Test
    fun anEmptyCatalogIsTheOneThingItCannotBuildFrom() {
        val empty = TemplatePlanGenerator(InMemoryExerciseCatalog(emptyList()))

        assertThat(runBlocking { empty.generate(PlanRequest(user(), 1, 0L)) })
            .isEqualTo(PlanGenerationResult.Failed)
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
