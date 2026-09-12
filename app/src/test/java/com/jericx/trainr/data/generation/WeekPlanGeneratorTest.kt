package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.PatternRequirement
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
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

class WeekPlanGeneratorTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val generator = WeekPlanGenerator(catalog)

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

    private fun generate(
        user: UserProfile,
        history: List<WeeklyWorkoutPlan> = emptyList(),
        week: Int = history.size + 1,
        fresh: Boolean = false
    ) = runBlocking {
        generator.generate(
            PlanRequest(user, week, (week - 1) * 7 * DAY, history.sortedByDescending { it.weekNumber }, freshCast = fresh)
        )
    }

    private fun planFor(
        user: UserProfile,
        history: List<WeeklyWorkoutPlan> = emptyList(),
        week: Int = history.size + 1,
        fresh: Boolean = false
    ) = (generate(user, history, week, fresh) as PlanGenerationResult.Generated).plan

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

    private fun climbed(from: WeeklyWorkoutPlan, to: WeeklyWorkoutPlan): Int {
        val before = from.workoutDays.flatMap { it.exercises }.associateBy { it.exerciseKey }
        return to.workoutDays.flatMap { it.exercises }.count { exercise ->
            val previous = before[exercise.exerciseKey] ?: return@count false
            val now = exercise.sets.first()
            val then = previous.sets.first()
            (now.targetReps ?: 0) > (then.targetReps ?: 0) ||
                (now.targetWeightKg ?: 0f) > (then.targetWeightKg ?: 0f) ||
                (now.targetSeconds ?: 0) > (then.targetSeconds ?: 0)
        }
    }

    // The whole point: every answer the setup screen allows gets a week, and
    // every such week passes the parser's checks with the limits the skeleton set.
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


    // With no movements there is nothing to build, and it says so rather
    // than handing over an empty week.
    @Test
    fun anEmptyCatalogIsTheOneThingItCannotBuildFrom() {
        val empty = WeekPlanGenerator(InMemoryExerciseCatalog(emptyList()))

        assertThat(runBlocking { empty.generate(PlanRequest(user(), 1, 0L)) })
            .isEqualTo(PlanGenerationResult.Failed)
    }

    // Two people who answered the same way should not train the same week for
    // ever, and one person rebuilding their own week should get it back.
    @Test
    fun twoClientsWhoAnsweredTheSameWayDoNotGetTheSameWeek() {
        fun movements(user: UserProfile) = planFor(user).movements().flatten()

        val alex = user()
        val sam = user().copy(id = 2)

        assertThat(movements(alex)).isEqualTo(movements(alex))
        assertThat(movements(alex)).isNotEqualTo(movements(sam))
    }

    @Test
    fun everyMovementChosenIsStillOneOfTheBestTheSlotOffered() {
        val skeleton = PlanSkeletonBuilder(catalog).build(PlanRequest(user(), 1, 0L))
        val chosen = planFor(user()).movements().flatten()

        val topThree = skeleton.days.flatMap { day -> day.slots.flatMap { it.candidates.take(3) } }.toSet()
        chosen.forEach { assertThat(topThree).contains(it) }
    }

    // Asking again is asking for something different.
    @Test
    fun aFreshCastIsADifferentWeek() {
        fun movements(fresh: Boolean) = planFor(user(), week = 2, fresh = fresh).movements().flatten()

        assertThat(movements(fresh = true)).isNotEqualTo(movements(fresh = false))
    }

    @Test
    fun aWeekNothingForcesToChangeIsLastWeeksMovementsUnderLastWeeksTitles() {
        val first = planFor(user()).logged()

        val second = planFor(user(), listOf(first))

        assertThat(second.movements()).isEqualTo(first.movements())
        assertThat(second.workoutDays.map { it.title }).isEqualTo(first.workoutDays.map { it.title })
    }

    @Test
    fun aWeekDoneInFullIsCarriedForwardHarder() {
        val first = planFor(user()).logged()

        val second = planFor(user(), listOf(first))

        assertThat(climbed(first, second)).isGreaterThan(0)
    }

    // Kit given up takes its movements with it, so the week is chosen afresh.
    @Test
    fun aProfileEditThatRulesAMovementOutPicksTheWeekAfresh() {
        val first = planFor(user()).logged()
        val bodyweight = user(kit = listOf(Equipment.NONE))

        val second = planFor(bodyweight, listOf(first))

        assertThat(second.movements()).isNotEqualTo(first.movements())
        assertThat(second.movements()).isEqualTo(planFor(bodyweight, listOf(first)).movements())
        second.movements().flatten().forEach {
            assertThat(catalog[it]!!.equipment).isEqualTo(Equipment.NONE)
        }
    }

    @Test
    fun aDifferentNumberOfDaysPicksTheWeekAfresh() {
        val first = planFor(user()).logged()

        val second = planFor(user(days = 4), listOf(first))

        assertThat(second.workoutDays).hasSize(4)
        assertThat(second.movements()).isNotEqualTo(first.movements())
    }

    @Test
    fun askingForNewMovementsIsNeverAnsweredWithLastWeeks() {
        val first = planFor(user()).logged()

        val second = planFor(user(), listOf(first), fresh = true)

        assertThat(second.movements()).isNotEqualTo(first.movements())
    }


    // Six weeks without a lighter one, and most sets left undone: a lighter
    // week is due, and it keeps the movements while cutting the work.
    @Test
    fun aDeloadWeekKeepsTheMovementsAndCutsTheSets() {
        val week = planFor(user())
        val history = (1..6).map { number ->
            week.copy(weekNumber = number, startDateMillis = (number - 1) * 7 * DAY).logged { it == 0 }
        }

        val deload = planFor(user(), history)

        assertThat(deload.movements()).isEqualTo(week.movements())
        assertThat(deload.workoutDays.sumOf { day -> day.exercises.sumOf { it.sets.size } })
            .isLessThan(week.workoutDays.sumOf { day -> day.exercises.sumOf { it.sets.size } })
    }

    private companion object {
        const val DAY = 86_400_000L
    }

    // Forty clients who answered the same way get forty weeks, not two. A pair
    // looks varied by a coin flip; only a crowd shows a choice riding on one bit.
    @Test
    fun fortyClientsWhoAnsweredTheSameWayGetFortyDifferentWeeks() {
        val weeks = (1L..40L).map { id ->
            planFor(user().copy(id = id)).workoutDays.flatMap { day -> day.exercises.map { it.exerciseKey } }
        }

        assertThat(weeks.toSet()).hasSize(40)
    }
}
