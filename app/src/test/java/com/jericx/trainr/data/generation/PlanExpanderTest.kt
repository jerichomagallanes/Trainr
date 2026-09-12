package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.SessionFocus
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot
import com.jericx.trainr.domain.generation.SlotTier
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import java.io.File
import org.junit.Test

class PlanExpanderTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val expander = PlanExpander(catalog)

    private val lifter = UserProfile(
        id = 7, age = 30, gender = Gender.MALE, weight = 80f,
        fitnessGoal = FitnessGoal.MUSCLE_GAIN, experienceLevel = ExperienceLevel.INTERMEDIATE,
        availableEquipment = Equipment.entries.toList()
    )

    private fun slot(
        vararg keys: String,
        tier: SlotTier = SlotTier.PRIMARY_COMPOUND,
        secondsPerSet: Int? = null
    ) = SkeletonSlot(
        id = "primary", tier = tier, candidates = keys.toList(), sets = 3, restSeconds = 120,
        secondsPerSet = secondsPerSet
    )

    private fun skeleton(vararg slots: SkeletonSlot) = PlanSkeleton(
        title = "Test Week",
        days = listOf(SkeletonDay(dayNumber = 1, focus = SessionFocus.FULL_BODY, slots = slots.toList())),
        maxSetsPerSession = 20, sessionCeilingMinutes = 90, uncoveredPatterns = emptySet()
    )

    private fun expand(
        skeleton: PlanSkeleton,
        user: UserProfile = lifter,
        selection: PlanSelection = PlanSelection(),
        history: List<WeeklyWorkoutPlan> = emptyList()
    ) = expander.expand(skeleton, selection, PlanRequest(user, 1, 0L, history))

    private fun choose(key: String, title: String = "") =
        PlanSelection(mapOf("day1" to DaySelection(slots = mapOf("primary" to key), title = title)))

    private val GeneratedPlan.only get() = days.single().exercises.single()

    @Test
    fun aChoiceTheSlotOfferedIsHonoured() {
        val picked = expand(skeleton(slot("goblet_squat", "dumbbell_squat")), selection = choose("dumbbell_squat"))

        assertThat(picked.only.exerciseKey).isEqualTo("dumbbell_squat")
    }

    // A key from outside the slot's own list is not a choice the skeleton
    // offered, so the slot keeps its own first.
    @Test
    fun aChoiceTheSlotNeverOfferedIsIgnored() {
        val picked = expand(skeleton(slot("goblet_squat", "dumbbell_squat")), selection = choose("push_up"))

        assertThat(picked.only.exerciseKey).isEqualTo("goblet_squat")
    }

    // A starting weight lighter than an empty bar is answered with the next
    // movement on the list, not a 20 kg lie.
    @Test
    fun aGuessLighterThanTheBarTakesTheNextMovement() {
        val light = lifter.copy(gender = Gender.FEMALE, weight = 50f, experienceLevel = ExperienceLevel.BEGINNER)

        val picked = expand(skeleton(slot("barbell_overhead_press", "dumbbell_shoulder_press")), user = light)

        assertThat(picked.only.exerciseKey).isEqualTo("dumbbell_shoulder_press")
    }

    // The day was fitted to the seconds the skeleton budgeted; the engine's
    // own guess may be longer and has to give way.
    @Test
    fun aTimedSetNeverRunsPastWhatTheDayBudgeted() {
        val losing = lifter.copy(fitnessGoal = FitnessGoal.WEIGHT_LOSS)

        val walk = expand(
            skeleton(slot("walking", tier = SlotTier.CONDITIONING, secondsPerSet = 300)), user = losing
        ).only

        assertThat(walk.sets.map { it.seconds }.distinct()).containsExactly(300)
    }

    // Last week's lifting reaches the engine, so a met week climbs.
    @Test
    fun lastWeeksLiftingIsWhatThisWeekProgressesFrom() {
        val lastWeek = WeeklyWorkoutPlan(
            userId = 7, weekNumber = 1, title = "Week",
            workoutDays = listOf(
                WorkoutDay(
                    dayNumber = 1, title = "Day", duration = 40, exerciseCount = 1, equipment = emptyList(),
                    exercises = listOf(
                        WorkoutExercise(
                            exerciseKey = "goblet_squat", name = "Goblet Squat",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            sets = (1..3).map {
                                ExerciseSet(
                                    setNumber = it, targetReps = 6, targetWeightKg = 20f,
                                    actualReps = 6, actualWeightKg = 20f, isCompleted = true
                                )
                            }
                        )
                    )
                )
            )
        )

        val squat = expand(skeleton(slot("goblet_squat")), history = listOf(lastWeek)).only

        assertThat(squat.sets.first().reps).isEqualTo(7)
        assertThat(squat.sets.first().weightKg).isEqualTo(20f)
    }

    @Test
    fun aBlankTitleFallsBackToTheSessionAndALongOneIsCut() {
        val blank = expand(skeleton(slot("goblet_squat")))
        val long = expand(skeleton(slot("goblet_squat")), selection = choose("goblet_squat", "x".repeat(80)))

        assertThat(blank.days.single().title).isEqualTo("Full Body")
        assertThat(long.days.single().title).hasLength(40)
    }
}
