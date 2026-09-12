package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.PatternRequirement
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import java.io.File
import org.junit.Test

class PlanSkeletonBuilderTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val builder = PlanSkeletonBuilder(catalog)

    private val everything = Equipment.entries.toList()
    private val kits = listOf(
        listOf(Equipment.NONE),
        listOf(Equipment.DUMBBELL),
        listOf(Equipment.RESISTANCE_BAND),
        listOf(Equipment.MACHINE),
        listOf(Equipment.BARBELL, Equipment.DUMBBELL),
        everything
    )

    private fun user(
        goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
        days: Int = 3,
        minutes: Int = 45,
        kit: List<Equipment> = everything,
        injuries: List<Injury> = emptyList(),
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE
    ) = UserProfile(
        age = 30, weight = 80f, fitnessGoal = goal, workoutDaysPerWeek = days, workoutDuration = minutes,
        availableEquipment = kit, injuries = injuries, experienceLevel = experience
    )

    private fun build(user: UserProfile, previous: WeeklyWorkoutPlan? = null) =
        builder.build(PlanRequest(user = user, weekNumber = 1, startDateMillis = 0L, history = listOfNotNull(previous)))

    // Every answer the setup screen allows, so no combination can slip past.
    private fun everyAnswer(): List<UserProfile> = FitnessGoal.entries.flatMap { goal ->
        listOf(30, 45, 60, 90).flatMap { minutes ->
            (1..7).flatMap { days -> kits.map { kit -> user(goal, days, minutes, kit) } }
        }
    }

    @Test
    fun theSplitFollowsHowManyDaysWereAskedFor() {
        (1..7).forEach { days ->
            assertWithMessage("$days days").that(build(user(days = days)).days).hasSize(days)
        }
        assertThat(build(user(days = 3)).days.map { it.focus }.distinct()).containsExactly(SessionFocus.FULL_BODY)
        assertThat(build(user(days = 4)).days.map { it.focus }).containsExactly(
            SessionFocus.UPPER, SessionFocus.LOWER, SessionFocus.UPPER, SessionFocus.LOWER
        ).inOrder()
        assertThat(build(user(days = 6)).days.map { it.focus }.distinct()).containsExactly(
            SessionFocus.PUSH, SessionFocus.PULL, SessionFocus.LEGS
        )
    }

    // Recovery needs a day between hard sessions where the week has room for
    // one.
    @Test
    fun noThreeHardDaysInARowBelowSixDaysAWeek() {
        (1..5).forEach { days ->
            val hard = build(user(days = days)).days.filter { it.focus.isHard }.map { it.dayNumber }.toSet()
            (1..5).forEach { start ->
                assertWithMessage("$days days from day $start")
                    .that(setOf(start, start + 1, start + 2).all { it in hard }).isFalse()
            }
        }
    }

    // Someone who came for mobility is never handed a squat rack.
    @Test
    fun aFlexibilityWeekIsMobilityWorkAndNeverAHeavyLift() {
        val week = build(user(goal = FitnessGoal.FLEXIBILITY, days = 4))

        assertThat(week.days.map { it.focus }.distinct()).containsExactly(SessionFocus.MOBILITY_FLOW)
        assertThat(week.days.flatMap { it.slots }.map { it.tier }.filter { it.isCompound }).isEmpty()
        assertThat(week.uncoveredPatterns).isEmpty()
    }

    @Test
    fun everyDayHasThreeToEightMovementsInSessionOrderWithUniqueIds() {
        everyAnswer().forEach { user ->
            build(user).days.forEach { day ->
                val where = "${user.fitnessGoal} ${user.workoutDaysPerWeek}d ${user.workoutDuration}m " +
                    "${user.availableEquipment} ${day.id}"
                assertWithMessage(where).that(day.slots.size).isIn(3..8)
                assertWithMessage(where).that(day.slots.map { it.tier.ordinal }).isInOrder()
                assertWithMessage(where).that(day.slots.map { it.id }).containsNoDuplicates()
                assertWithMessage(where).that(day.slots.all { it.id.matches(Regex("[a-z][a-z0-9_]*")) }).isTrue()
            }
        }
    }

    // One key per slot; two slots offering the same key could put one
    // movement in a session twice.
    @Test
    fun candidatesWithinADayAreNeverEmptyAndNeverShared() {
        everyAnswer().forEach { user ->
            build(user).days.forEach { day ->
                val where = "${user.fitnessGoal} ${user.availableEquipment} ${day.id}"
                day.slots.forEach { assertWithMessage("$where ${it.id}").that(it.candidates).isNotEmpty() }
                assertWithMessage(where).that(day.slots.flatMap { it.candidates }).containsNoDuplicates()
            }
        }
    }

    // What the week offers is what the client owns and may do.
    @Test
    fun everyCandidateIsOwnedAndSurvivesTheInjuryGuard() {
        val sore = user(kit = listOf(Equipment.DUMBBELL), injuries = listOf(Injury.SHOULDER, Injury.KNEE))

        build(sore).allowedKeys.forEach { key ->
            val movement = catalog[key]
            assertWithMessage(key).that(movement).isNotNull()
            assertWithMessage(key).that(movement!!.isAvailableWith(setOf(Equipment.DUMBBELL))).isTrue()
            assertWithMessage(key).that(InjuryGuard.excludes(movement, sore.injuries)).isFalse()
        }
    }

    // The filter is the whole mechanism, so every injury needs its own proof.
    @Test
    fun noCandidateListContainsAMovementContraindicatedForTheClientsInjuries() {
        Injury.entries.forEach { injury ->
            FitnessGoal.entries.forEach { goal ->
                build(user(goal = goal, days = 5, minutes = 60, injuries = listOf(injury))).allowedKeys.forEach { key ->
                    assertWithMessage("$injury $goal $key")
                        .that(InjuryGuard.excludes(catalog[key]!!, listOf(injury))).isFalse()
                }
            }
        }
    }

    // The two limits the parser checks are built in before anything is
    // generated, priced at the top of every rep window.
    @Test
    fun everyAnswerFitsTheSessionCapAndItsLength() {
        everyAnswer().forEach { user ->
            val week = build(user)
            week.days.forEach { day ->
                val where = "${user.fitnessGoal} ${user.workoutDaysPerWeek}d ${user.workoutDuration}m " +
                    "${user.availableEquipment} ${day.id}"
                assertWithMessage(where).that(day.setCount).isAtMost(week.maxSetsPerSession)
                assertWithMessage(where).that(day.slots.maxOf { it.sets }).isAtMost(10)
            }
        }
    }

    // A full-body week has its press and its pull in the second and third
    // slots, not the first; dealt only to the first, every such week would
    // report itself as having neither.
    @Test
    fun aFullGymWeekCoversASquatAPressAndAPull() {
        listOf(1, 3, 4, 6).forEach { days ->
            val week = build(user(days = days))
            val patterns = week.days.flatMap { it.slots }.mapNotNull { it.candidates.firstOrNull() }
                .mapNotNull { catalog[it]?.pattern }

            assertWithMessage("$days days").that(week.uncoveredPatterns).isEmpty()
            PatternRequirement.entries.forEach { requirement ->
                assertWithMessage("$days days $requirement").that(patterns.any(requirement::isMetBy)).isTrue()
            }
        }
    }

    // The worst case the setup screen allows.
    @Test
    fun everyInjuryWithNoEquipmentStillBuildsAWholeWeek() {
        val week = build(user(kit = listOf(Equipment.NONE), injuries = Injury.entries))

        assertThat(week.days).hasSize(3)
        week.days.forEach { assertThat(it.slots.size).isAtLeast(3) }
    }

    @Test
    fun theWarmUpComesFirstEvenInTheTightestSession() {
        FitnessGoal.entries.forEach { goal ->
            build(user(goal = goal, minutes = 30)).days.forEach { day ->
                assertWithMessage("$goal ${day.id}").that(day.slots.first().tier).isEqualTo(SlotTier.WARM_UP)
            }
        }
    }

    @Test
    fun aWeightLossWeekCarriesMoreConditioningThanAStrengthWeek() {
        fun conditioningDays(goal: FitnessGoal) = build(user(goal = goal, days = 5)).days
            .count { day -> day.slots.any { it.tier == SlotTier.CONDITIONING } }

        assertThat(conditioningDays(FitnessGoal.WEIGHT_LOSS)).isGreaterThan(conditioningDays(FitnessGoal.STRENGTH))
    }

    // Conditioning slots only ever hold cardio measured in time: never a
    // walking lunge, never a clean off a bodyweight guess.
    @Test
    fun conditioningIsOnlyEverTimedCardio() {
        build(user(goal = FitnessGoal.WEIGHT_LOSS, days = 5)).days.flatMap { it.slots }
            .filter { it.tier == SlotTier.CONDITIONING }
            .flatMap { it.candidates }
            .forEach { key ->
                val movement = catalog[key]!!
                assertWithMessage(key).that(movement.measure).isEqualTo(ExerciseMeasure.DURATION)
                assertWithMessage(key).that(movement.pattern).isEqualTo(MovementPattern.CONDITIONING)
            }
    }

    // A key the client lifted last week is the one offered first, or its
    // history stops here.
    @Test
    fun lastWeeksMovementsAreOfferedFirst() {
        val first = build(user())
        val primary = first.days.first().slots.first { it.tier == SlotTier.PRIMARY_COMPOUND }
        val runnerUp = primary.candidates[1]
        val lastWeek = WeeklyWorkoutPlan(
            userId = 1, weekNumber = 1, title = "Week",
            workoutDays = listOf(
                WorkoutDay(
                    dayNumber = 1, title = "Day", duration = 45, exerciseCount = 1, equipment = emptyList(),
                    exercises = listOf(WorkoutExercise(exerciseKey = runnerUp, name = runnerUp))
                )
            )
        )

        val next = build(user(), previous = lastWeek)

        assertThat(next.days.first().slots.first { it.tier == SlotTier.PRIMARY_COMPOUND }.candidates.first())
            .isEqualTo(runnerUp)
    }

    // Isolation is dealt to where the week is short, so it cannot all land on
    // one muscle.
    @Test
    fun isolationWorkIsSpreadAcrossMoreThanOneRegion() {
        val regions = build(user(days = 3)).days.flatMap { it.slots }
            .filter { it.tier == SlotTier.ISOLATION }
            .mapNotNull { catalog[it.candidates.first()]?.primary?.region }
            .distinct()

        assertThat(regions.size).isAtLeast(2)
    }

    @Test
    fun theSameAnswersAlwaysBuildTheSameWeek() {
        assertThat(build(user())).isEqualTo(build(user()))
    }

    @Test
    fun theWeekIsTitledForWhoItIsFor() {
        assertThat(build(user(experience = ExperienceLevel.BEGINNER)).title).isEqualTo("Beginner Muscle Building")
    }
}
