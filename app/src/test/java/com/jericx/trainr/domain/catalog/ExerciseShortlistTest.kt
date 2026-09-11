package com.jericx.trainr.domain.catalog

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import org.junit.Test

class ExerciseShortlistTest {

    private fun exercise(
        key: String,
        muscle: MuscleGroup = MuscleGroup.CHEST,
        equipment: Equipment = Equipment.NONE,
        pattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH,
        staple: Boolean = false
    ) = CatalogExercise(
        key = key,
        name = key,
        primary = muscle,
        secondary = emptyList(),
        equipment = equipment,
        measure = ExerciseMeasure.REPS,
        pattern = pattern,
        staple = staple
    )

    private fun profile(
        vararg owned: Equipment,
        goal: FitnessGoal = FitnessGoal.GENERAL_FITNESS
    ) = UserProfile(availableEquipment = owned.toList(), fitnessGoal = goal)

    // A barbell movement offered to someone with no barbell is a movement they
    // cannot do, and the model has no way to know that.
    @Test
    fun onlyMovementsTheClientCanPerformAreOffered() {
        val catalog = InMemoryExerciseCatalog(
            listOf(
                exercise("push_up"),
                exercise("barbell_bench_press", equipment = Equipment.BARBELL)
            )
        )

        val offered = ExerciseShortlist.forRequest(catalog, profile(Equipment.DUMBBELL))

        assertThat(offered.map { it.key }).containsExactly("push_up")
    }

    // Bodyweight is the one category everybody owns; everything else has to
    // be ticked on the setup screen before it can be prescribed.
    @Test
    fun bodyweightIsAvailableToEveryoneAndNothingElseIs() {
        val catalog = InMemoryExerciseCatalog(
            listOf(
                exercise("push_up"),
                exercise("machine_leg_press", equipment = Equipment.MACHINE)
            )
        )

        assertThat(ExerciseShortlist.forRequest(catalog, profile(Equipment.NONE)).map { it.key })
            .containsExactly("push_up")
        assertThat(ExerciseShortlist.forRequest(catalog, profile(Equipment.MACHINE)).map { it.key })
            .containsExactly("push_up", "machine_leg_press")
    }

    // A key the model cannot name again is a lift whose history stops there.
    @Test
    fun lastWeeksMovementsSurviveTheCap() {
        val filler = (1..200).map { exercise("filler_$it") }
        val catalog = InMemoryExerciseCatalog(filler + exercise("goblet_squat"))

        val offered = ExerciseShortlist.forRequest(
            catalog,
            profile(Equipment.NONE),
            carriedOver = setOf("goblet_squat")
        )

        assertThat(offered.map { it.key }).contains("goblet_squat")
    }

    @Test
    fun theShortlistIsCappedSoTheSchemaStaysAffordable() {
        val catalog = InMemoryExerciseCatalog((1..400).map { exercise("filler_$it") })

        assertThat(ExerciseShortlist.forRequest(catalog, profile(Equipment.NONE)))
            .hasSize(80)
    }

    // A cap that took the first eighty alphabetically would hand back a week
    // of chest and no legs.
    @Test
    fun theCapIsSpreadAcrossRegionsRatherThanTakenOffTheTop() {
        val chest = (1..100).map { exercise("chest_$it", muscle = MuscleGroup.CHEST) }
        val quads = (1..100).map {
            exercise("quad_$it", muscle = MuscleGroup.QUADRICEPS, pattern = MovementPattern.SQUAT)
        }
        val catalog = InMemoryExerciseCatalog(chest + quads)

        val offered = ExerciseShortlist.forRequest(catalog, profile(Equipment.NONE))

        assertThat(offered.count { it.primary == MuscleGroup.QUADRICEPS }).isEqualTo(40)
        assertThat(offered.count { it.primary == MuscleGroup.CHEST }).isEqualTo(40)
    }

    @Test
    fun staplesAreReachedForFirst() {
        val catalog = InMemoryExerciseCatalog(
            (1..100).map { exercise("filler_$it") } + exercise("push_up", staple = true)
        )

        assertThat(ExerciseShortlist.forRequest(catalog, profile(Equipment.NONE)).map { it.key })
            .contains("push_up")
    }

    // Nothing is demanded that the client's kit cannot supply.
    @Test
    fun onlyPatternsTheShortlistCanSatisfyAreRequired() {
        val pushOnly = listOf(exercise("push_up", pattern = MovementPattern.HORIZONTAL_PUSH))

        assertThat(ExerciseShortlist.requiredPatterns(pushOnly))
            .containsExactly(PatternRequirement.UPPER_PUSH)
    }

    @Test
    fun aFullShortlistDemandsAllThreePatterns() {
        val full = listOf(
            exercise("bodyweight_squat", pattern = MovementPattern.SQUAT),
            exercise("push_up", pattern = MovementPattern.HORIZONTAL_PUSH),
            exercise("pull_up", pattern = MovementPattern.VERTICAL_PULL)
        )

        assertThat(ExerciseShortlist.requiredPatterns(full))
            .containsExactly(
                PatternRequirement.LOWER_PUSH,
                PatternRequirement.UPPER_PUSH,
                PatternRequirement.UPPER_PULL
            )
    }

    private fun conditioning(key: String, staple: Boolean = false) = exercise(
        key = key,
        muscle = MuscleGroup.CARDIO,
        pattern = MovementPattern.CONDITIONING,
        staple = staple
    )

    private fun mobility(key: String) =
        exercise(key = key, muscle = MuscleGroup.FULL_BODY, pattern = MovementPattern.MOBILITY)

    private fun mixedCatalog() = InMemoryExerciseCatalog(
        buildList {
            add(conditioning("walking", staple = true))
            repeat(9) { add(conditioning("zz_activity_$it")) }
            add(mobility("stretching"))
            repeat(3) { add(mobility("zz_mobility_$it")) }
            MuscleGroup.entries
                .filter { it.region.isTrainable }
                .forEach { muscle -> repeat(12) { add(exercise("lift_${muscle}_$it", muscle = muscle)) } }
        }
    )

    // Ranked among the muscle regions, conditioning and mobility lost every
    // slot to the alphabet: a client who asked for flexibility was offered a
    // single stretch, and one who asked to lose weight was never offered a walk.
    @Test
    fun theGoalDecidesHowMuchConditioningAndMobilityIsOffered() {
        val catalog = mixedCatalog()

        val forMuscle = ExerciseShortlist.forRequest(
            catalog, profile(Equipment.NONE, goal = FitnessGoal.MUSCLE_GAIN)
        )
        val forFlexibility = ExerciseShortlist.forRequest(
            catalog, profile(Equipment.NONE, goal = FitnessGoal.FLEXIBILITY)
        )
        val forWeightLoss = ExerciseShortlist.forRequest(
            catalog, profile(Equipment.NONE, goal = FitnessGoal.WEIGHT_LOSS)
        )

        fun List<CatalogExercise>.mobility() = count { it.pattern == MovementPattern.MOBILITY }
        fun List<CatalogExercise>.conditioning() = count { it.primary == MuscleGroup.CARDIO }

        assertThat(forFlexibility.mobility()).isGreaterThan(forMuscle.mobility())
        assertThat(forWeightLoss.conditioning()).isGreaterThan(forMuscle.conditioning())
        assertThat(forMuscle.mobility()).isAtLeast(1)
    }

    // Every goal needs a warm-up, and the staple is what a coach reaches for
    // rather than whatever sorts first.
    @Test
    fun theStapleConditioningAndMobilityAreTheOnesOffered() {
        val shortlist = ExerciseShortlist.forRequest(
            mixedCatalog(), profile(Equipment.NONE, goal = FitnessGoal.WEIGHT_LOSS)
        )

        assertThat(shortlist.map { it.key }).containsAtLeast("walking", "stretching")
    }

    // Someone who came for mobility should not have their week rejected for
    // holding no squat.
    @Test
    fun aFlexibilityGoalIsNotHeldToTheSquatPressPullRule() {
        val shortlist = ExerciseShortlist.forRequest(mixedCatalog(), profile(Equipment.NONE))

        assertThat(ExerciseShortlist.requiredPatterns(shortlist, FitnessGoal.MUSCLE_GAIN))
            .isNotEmpty()
        assertThat(ExerciseShortlist.requiredPatterns(shortlist, FitnessGoal.FLEXIBILITY))
            .isEmpty()
    }

    // Ruled out means never offered, so the model cannot pick it and the
    // parser never has to catch it.
    @Test
    fun aMovementAnInjuryRulesOutIsNeverOffered() {
        val catalog = InMemoryExerciseCatalog(
            listOf(
                exercise("overhead_press", pattern = MovementPattern.VERTICAL_PUSH),
                exercise("push_up")
            )
        )
        val shoulder = UserProfile(
            availableEquipment = listOf(Equipment.NONE),
            injuries = listOf(com.jericx.trainr.domain.model.Injury.SHOULDER)
        )

        val offered = ExerciseShortlist.forRequest(catalog, shoulder).map { it.key }

        assertThat(offered).containsExactly("push_up")
    }
}
