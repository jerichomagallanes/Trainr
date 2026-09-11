package com.jericx.trainr.data.catalog

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.equipmentFor
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog
import java.io.File
import org.junit.Test
import com.jericx.trainr.domain.catalog.ExerciseRole
import com.jericx.trainr.domain.catalog.isLoadable
import com.jericx.trainr.domain.catalog.role
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.model.Injury
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.domain.generation.LoadStep
import com.jericx.trainr.domain.generation.ProgressionEngine
import com.jericx.trainr.domain.generation.ProgressionRequest
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.UserProfile

// The catalog is data, and data that ships wrong is a plan that reads wrong.
// These hold the file itself to account rather than the code that reads it.
class ExerciseCatalogIntegrityTest {

    private val source = File("src/main/assets/exercise-catalog.json").readText()
    private val catalog = ExerciseCatalogReader.read(source)

    // Each category holds exactly what the source's own filter holds. The one
    // entry short of 452 is a user-made "custom" superset, which is somebody's
    // own and not part of the list.
    @Test
    fun eachCategoryHoldsExactlyTheMovementsTheSourceHas() {
        val counted = catalog.all.groupingBy { it.equipment }.eachCount()

        assertThat(counted).containsExactlyEntriesIn(CATALOG_SIZE)
        assertThat(catalog.all).hasSize(451)
    }

    // A dropped entry is silent: the reader skips what it cannot understand,
    // so the count is the only thing that catches a bad enum value.
    @Test
    fun everyEntryInTheFileSurvivesReading() {
        val written = Regex("\"key\"\\s*:").findAll(source).count()

        assertThat(catalog.all).hasSize(written)
    }

    @Test
    fun keysAreUniqueAndShapedLikeSlugs() {
        val shape = Regex("[a-z][a-z0-9_]*")

        assertThat(catalog.all.map { it.key }).containsNoDuplicates()
        assertThat(catalog.all.filterNot { shape.matches(it.key) }).isEmpty()
    }

    // These keys index saved history and hand-verified tutorials. Renaming one
    // silently splits a client's log and drops their video.
    @Test
    fun theKeysTutorialsAreIndexedOnAreAllPresent() {
        val missing = ExerciseVideoCatalog.videoIds.keys.filter { catalog[it] == null }

        assertThat(missing).isEmpty()
    }

    // Whatever the setup screen offers has to lead somewhere.
    @Test
    fun everyCategoryTheSetupScreenOffersCanTrainEveryRegion() {
        val stocked = catalog.all.map { it.equipment }.toSet()

        val offered = equipmentFor(stocked)
        assertThat(offered).isNotEmpty()
        offered.forEach { kit ->
            assertThat(catalog.availableWith(setOf(kit))).isNotEmpty()
        }
    }

    // Someone who owns nothing must still get a whole week, or the app's own
    // "bodyweight only" answer leads to a plan it cannot build.
    @Test
    fun aClientWithNoEquipmentCanPushPullAndSquat() {
        val bodyweight = catalog.availableWith(setOf(Equipment.NONE))

        assertThat(bodyweight.any { it.pattern.isLowerPush }).isTrue()
        assertThat(bodyweight.any { it.pattern.isPush }).isTrue()
        assertThat(bodyweight.any { it.pattern.isPull }).isTrue()
    }

    @Test
    fun aClientWithNoEquipmentCanTrainEveryRegion() {
        val reachable = catalog.availableWith(setOf(Equipment.NONE))
            .map { it.primary.region }
            .toSet()
        val missing = MuscleRegion.entries.filter { it.isTrainable && it !in reachable }

        assertThat(missing).isEmpty()
    }

    @Test
    fun theVocabularyIsTheSourcesOwnNineCategories() {
        assertThat(Equipment.entries).hasSize(9)
    }

    // The catalog is generated from docs/exercise-source.txt and may hold
    // nothing else. Checked both ways: a movement invented into the catalog
    // fails, and one transcribed but lost in generation fails too.
    @Test
    fun theCatalogIsExactlyWhatWasTranscribedFromTheSource() {
        val source = File("../docs/exercise-source.txt").readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { it.split("|") }
            .map { Triple(it[1], it[0], it[2]) }
            .toSet()
        val catalogued = catalog.all
            .map { Triple(it.name, it.equipment.name, it.primary.name) }
            .toSet()

        assertThat(catalogued - source).isEmpty()
        assertThat(source - catalogued).isEmpty()
    }

    // Read one-handed between sets, so the shape matters as much as the
    // content: a wall of text is a step nobody reads.
    @Test
    fun howToStepsFitOnAPhoneScreen() {
        val described = catalog.all.filter { it.steps.isNotEmpty() }

        assertThat(described.filter { it.steps.size !in 4..7 }.map { it.key }).isEmpty()
        assertThat(
            described.flatMap { it.steps }.filter { it.split(" ").size > 16 }
        ).isEmpty()
    }

    // The list is numbered by the UI, so a step that numbers itself renders
    // as "1. 1. Lie back".
    @Test
    fun stepsCarryNoNumberingOfTheirOwn() {
        val numbered = catalog.all
            .flatMap { it.steps }
            .filter { Regex("^\\s*\\d+[.)]").containsMatchIn(it) }

        assertThat(numbered).isEmpty()
    }

    // A step is an instruction, so it opens with the thing to do.
    @Test
    fun everyStepStartsWithACapitalAndEndsWithAStop() {
        val malformed = catalog.all
            .flatMap { it.steps }
            .filter { !it.first().isUpperCase() || !it.endsWith(".") }

        assertThat(malformed).isEmpty()
    }

    // A movement assists muscles; it cannot assist the one it already trains.
    @Test
    fun secondaryMusclesNeverRepeatThePrimary() {
        val confused = catalog.all.filter { it.primary in it.secondary }

        assertThat(confused.map { it.key }).isEmpty()
    }

    private companion object {
        val ReviewedUnilateral = listOf(
            "assisted_pistol_squats",
            "barbell_bulgarian_split_squat",
            "barbell_single_arm_landmine_press",
            "barbell_single_leg_romanian_deadlift",
            "barbell_single_leg_standing_calf_raise",
            "cable_reverse_fly_single_arm",
            "cable_single_arm_curl",
            "cable_single_arm_lateral_raise",
            "cable_single_arm_triceps_pushdown",
            "cable_triceps_kickback",
            "concentration_curl",
            "dumbbell_bulgarian_split_squat",
            "dumbbell_side_bend",
            "dumbbell_single_arm_tricep_extension",
            "dumbbell_single_leg_hip_thrust",
            "dumbbell_single_leg_romanian_deadlift",
            "dumbbell_single_leg_standing_calf_raise",
            "dumbbell_split_squat",
            "dumbbell_step_up",
            "dumbbell_suitcase_carry",
            "dumbbell_triceps_kickback",
            "glute_kickback_on_floor",
            "kettlebell_turkish_get_up",
            "machine_glute_kickback",
            "machine_single_leg_press",
            "machine_single_leg_standing_calf_raise",
            "one_arm_push_up",
            "pistol_squat",
            "reverse_grip_concentration_curl",
            "side_bend",
            "side_plank",
            "single_arm_cable_crossover",
            "single_arm_cable_row",
            "single_arm_lat_pulldown",
            "single_leg_extensions",
            "single_leg_glute_bridge",
            "single_leg_hip_thrust",
            "single_leg_standing_calf_raise",
            "standing_cable_glute_kickbacks",
            "step_up"
        )
        // What each category holds in the source's own equipment filter.
        val CATALOG_SIZE = mapOf(
            Equipment.NONE to 105,
            Equipment.BARBELL to 74,
            Equipment.DUMBBELL to 70,
            Equipment.KETTLEBELL to 13,
            Equipment.MACHINE to 145,
            Equipment.PLATE to 8,
            Equipment.RESISTANCE_BAND to 13,
            Equipment.SUSPENSION_BAND to 7,
            Equipment.OTHER to 16
        )
    }

    // A movement listing kit that is never offered on the setup screen can
    // never be selected, so it is dead weight in every schema that carries it.
    @Test
    fun nothingRequiresEquipmentTheAppNeverAsksAbout() {
        val askedAbout = Equipment.entries.toSet()
        val unknown = catalog.all.map { it.equipment }.toSet() - askedAbout

        assertThat(unknown).isEmpty()
    }

    // A chip is offered only where there are movements behind it, so an empty
    // category must never reach the setup screen.
    @Test
    fun aCategoryWithNoMovementsIsNotOffered() {
        val stocked = catalog.all.map { it.equipment }.toSet()
        val empty = Equipment.entries - stocked

        empty.forEach { kit ->
            assertThat(equipmentFor(stocked)).doesNotContain(kit)
        }
    }

    // Staples are what the shortlist reaches for first; if most things are
    // staples the ordering stops meaning anything.
    @Test
    fun staplesAreAMinorityAndCoverTheMovementsThatMatter() {
        val staples = catalog.all.filter { it.staple }

        assertThat(staples.size).isLessThan(catalog.all.size / 2)
        assertThat(staples.map { it.pattern }).containsAtLeast(
            MovementPattern.SQUAT,
            MovementPattern.HINGE,
            MovementPattern.HORIZONTAL_PUSH,
            MovementPattern.HORIZONTAL_PULL
        )
    }

    @Test
    fun everyMovementIsNamed() {
        assertThat(catalog.all.filter { it.name.isBlank() }).isEmpty()
    }

    // Pinned rather than matched on the name, because the name does not
    // settle it: a walking lunge alternates inside the set and a dumbbell row
    // does not say which arm. Where the name is ambiguous the movement is left
    // bilateral, so the chip understates the work rather than doubling it.
    @Test
    fun onlyTheMovementsReviewedAsPerSideAreMarkedUnilateral() {
        val marked = catalog.all.filter { it.unilateral }.map { it.key }.sorted()

        assertThat(marked).containsExactlyElementsIn(ReviewedUnilateral.sorted()).inOrder()
    }

    // A movement worked one side at a time is never one the client could not
    // hold on one side.
    @Test
    fun everyUnilateralMovementStillExistsAndIsPerformable() {
        ReviewedUnilateral.forEach { key ->
            assertThat(catalog[key]).isNotNull()
        }
    }

    // The load rules only ever run on movements that carry a weight, so the
    // measure has to be what decides it: assisted work is on a machine and has
    // no weight to choose.
    @Test
    fun onlyWeightedMovementsAreLoadable() {
        val loadable = catalog.all.filter { it.isLoadable }

        assertThat(loadable).isNotEmpty()
        assertThat(loadable.map { it.measure }.distinct())
            .containsExactly(ExerciseMeasure.WEIGHT_AND_REPS)
        assertThat(catalog.all.filter { it.key.startsWith("assisted_") }.filter { it.isLoadable })
            .isEmpty()
    }

    // Rep windows and rest are chosen by role, so the role has to follow the
    // measure: a clean is tagged CONDITIONING and is still a loaded lift that
    // would be nonsense prescribed in seconds.
    @Test
    fun aMovementIsTimedOnlyWhenItIsMeasuredInSeconds() {
        val timed = catalog.all.filter { it.role == ExerciseRole.TIMED }

        assertThat(timed.map { it.measure }.distinct()).containsExactly(ExerciseMeasure.DURATION)
        assertThat(catalog.all.filter { it.measure == ExerciseMeasure.DURATION }.map { it.role })
            .doesNotContain(ExerciseRole.COMPOUND)
        assertThat(catalog["clean"]?.role).isEqualTo(ExerciseRole.COMPOUND)
        assertThat(catalog.all.map { it.role }.distinct())
            .containsAtLeast(ExerciseRole.COMPOUND, ExerciseRole.ISOLATION, ExerciseRole.TIMED)
    }

    // A key renamed in the catalog would stop the guard excluding anything,
    // and nothing would say so.
    @Test
    fun everyMovementTheInjuryGuardNamesExists() {
        val missing = InjuryGuard.NamedKeys.filter { catalog[it] == null }

        assertThat(missing).isEmpty()
    }

    // The guard is only safe if it cannot make a week unbuildable. The worst
    // case the setup screen allows is every injury and no equipment, and that
    // client still has to be able to squat, press and pull.
    @Test
    fun everyInjuryAtOnceWithNoEquipmentStillLeavesASquatAPressAndAPull() {
        val left = catalog.availableWith(setOf(Equipment.NONE))
            .filterNot { InjuryGuard.excludes(it, Injury.entries) }

        assertThat(left.any { it.pattern.isLowerPush }).isTrue()
        assertThat(left.any { it.pattern.isPush }).isTrue()
        assertThat(left.any { it.pattern.isPull }).isTrue()
    }

    // The engine calibrates every movement for whoever turns up; none of those
    // first weeks may carry a number the parser would reject or the kit
    // cannot make.
    @Test
    fun everyMovementCalibratesToNumbersTheParserAccepts() {
        val people = listOf(
            UserProfile(age = 30, gender = Gender.MALE, weight = 110f,
                fitnessGoal = FitnessGoal.STRENGTH, experienceLevel = ExperienceLevel.ADVANCED),
            UserProfile(age = 72, gender = Gender.FEMALE, weight = 45f,
                fitnessGoal = FitnessGoal.ENDURANCE, experienceLevel = ExperienceLevel.BEGINNER),
            UserProfile(age = 15, gender = Gender.PREFER_NOT_TO_SAY, weight = 55f,
                fitnessGoal = FitnessGoal.FLEXIBILITY, experienceLevel = ExperienceLevel.BEGINNER)
        )

        catalog.all.forEach { movement ->
            people.forEach { person ->
                val target = ProgressionEngine.next(ProgressionRequest(person, movement, sets = 3))
                val where = "${movement.key} for ${person.fitnessGoal}"

                assertWithMessage(where).that(target.sets).isNotEmpty()
                target.sets.forEach { set ->
                    when (movement.measure) {
                        ExerciseMeasure.DURATION ->
                            assertWithMessage(where).that(set.targetSeconds in 5..5400).isTrue()
                        else -> assertWithMessage(where).that(set.targetReps in 1..100).isTrue()
                    }
                    set.targetWeightKg?.let { kg ->
                        assertWithMessage(where).that(kg).isAtLeast(0.5f)
                        assertWithMessage(where).that(kg).isAtMost(LoadStep.ceilingKg(movement.equipment))
                    }
                    if (movement.measure == ExerciseMeasure.WEIGHT_AND_REPS) {
                        assertWithMessage(where).that(set.targetWeightKg).isNotNull()
                    }
                }
            }
        }
    }
}
