package com.jericx.trainr.data.catalog

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.equipmentFor
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog
import java.io.File
import org.junit.Test

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

        WorkoutLocation.entries.forEach { location ->
            val offered = equipmentFor(location, stocked)
            assertThat(offered).isNotEmpty()
            offered.forEach { kit ->
                assertThat(catalog.availableWith(setOf(kit))).isNotEmpty()
            }
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
            WorkoutLocation.entries.forEach { location ->
                assertThat(equipmentFor(location, stocked)).doesNotContain(kit)
            }
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
}
