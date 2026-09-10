package com.jericx.trainr.data.catalog

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog
import java.io.File
import org.junit.Test

// The catalog is data, and data that ships wrong is a plan that reads wrong.
// These hold the file itself to account rather than the code that reads it.
class ExerciseCatalogIntegrityTest {

    private val source = File("src/main/assets/exercise-catalog.json").readText()
    private val catalog = ExerciseCatalogReader.read(source)

    // The size of each category is the source's own, so a movement quietly
    // added or lost shows up here rather than in someone's plan.
    @Test
    fun eachCategoryHoldsExactlyTheMovementsItShould() {
        val counted = catalog.all.groupingBy { it.equipment }.eachCount()

        assertThat(counted).containsExactlyEntriesIn(
            mapOf(
                Equipment.NONE to 105,
                Equipment.BARBELL to 74,
                Equipment.DUMBBELL to 70,
                Equipment.KETTLEBELL to 13,
                Equipment.MACHINE to 145,
                Equipment.PLATE to 8,
                Equipment.RESISTANCE_BAND to 13,
                Equipment.SUSPENSION_BAND to 7,
                Equipment.OTHER to 17
            )
        )
        assertThat(catalog.all).hasSize(452)
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

    // These keys already index saved history and hand-verified tutorials.
    // Renaming one silently splits a client's log and drops their video.
    @Test
    fun theKeysTutorialsAreIndexedOnAreAllPresent() {
        val missing = ExerciseVideoCatalog.videoIds.keys.filter { catalog[it] == null }

        assertThat(missing).isEmpty()
    }

    // Someone who owns nothing must still get a whole week, or the app's own
    // "bodyweight only" answer leads to a plan it cannot build.
    @Test
    fun aClientWithNoEquipmentCanStillTrainEveryRegion() {
        val bodyweight = catalog.availableWith(setOf(Equipment.NONE))
        val reachable = bodyweight.map { it.muscle.region }.toSet()

        MuscleRegion.entries.filter { it.isTrainable }.forEach { region ->
            assertThat(reachable).contains(region)
        }
    }

    @Test
    fun aClientWithNoEquipmentCanPushPullAndSquat() {
        val bodyweight = catalog.availableWith(setOf(Equipment.NONE))

        assertThat(bodyweight.any { it.pattern.isLowerPush }).isTrue()
        assertThat(bodyweight.any { it.pattern.isPush }).isTrue()
        assertThat(bodyweight.any { it.pattern.isPull }).isTrue()
    }

    // The catalog is a closed vocabulary, so a movement naming a category the
    // profile cannot hold is a movement nobody will ever be offered.
    @Test
    fun everyMovementSitsInExactlyOneOfTheNineCategories() {
        assertThat(Equipment.entries).hasSize(9)
        assertThat(catalog.all.map { it.equipment }.toSet()).hasSize(9)
    }

    // A movement listing kit that is never offered on the setup screen can
    // never be selected, so it is dead weight in every schema that carries it.
    @Test
    fun nothingRequiresEquipmentTheAppNeverAsksAbout() {
        val askedAbout = Equipment.entries.toSet()
        val unknown = catalog.all.map { it.equipment }.toSet() - askedAbout

        assertThat(unknown).isEmpty()
    }

    // Every category the setup screen offers has to lead somewhere, or a
    // client ticks a chip and the plan ignores it.
    @Test
    fun everyEquipmentTheSetupScreenOffersHasMovements() {
        val empty = Equipment.entries.filter { kit ->
            catalog.all.none { it.equipment == kit }
        }

        assertThat(empty).isEmpty()
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
    fun everyMovementIsNamedInBothLanguagesTheCatalogCarries() {
        assertThat(catalog.all.filter { it.name.isBlank() || it.nameJa.isBlank() }).isEmpty()
    }
}
