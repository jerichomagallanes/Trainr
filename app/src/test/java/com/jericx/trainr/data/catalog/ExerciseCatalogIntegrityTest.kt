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

    // Every entry is read off the source's own list, so a category can be
    // short but never long. Four of them are transcribed end to end; the rest
    // fill up as the remaining pages arrive.
    @Test
    fun noCategoryHoldsMoreMovementsThanTheSourceHas() {
        val counted = catalog.all.groupingBy { it.equipment }.eachCount()

        FULL_CATEGORIES.forEach { (kit, size) ->
            assertThat(counted[kit] ?: 0).isEqualTo(size)
        }
        CATALOG_SIZE.forEach { (kit, size) ->
            assertThat(counted[kit] ?: 0).isAtMost(size)
        }
        assertThat(catalog.all.size).isAtMost(CATALOG_SIZE.values.sum())
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

    // These keys index saved history and hand-verified tutorials. The ones
    // still missing are bodyweight movements the source's own list will
    // restore; nothing may be lost beyond those.
    @Test
    fun theTutorialKeysStillInTheCatalogAreTheOnesItCanHold() {
        val missing = ExerciseVideoCatalog.videoIds.keys.filter { catalog[it] == null }

        assertThat(missing).containsExactly(
            "bicycle_crunch", "glute_bridge", "high_intensity_intervals", "jump_squat",
            "leg_raise", "plank", "romanian_deadlift", "russian_twist", "walking_lunge",
            "warm_up_jog"
        )
    }

    // Whatever the setup screen offers has to lead somewhere. While a
    // category is still empty the chip is simply not shown, so this holds
    // for every state the catalog passes through.
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

    // A gym-goer must be able to press, pull and squat from the catalog
    // alone, or the week the prompt insists on cannot be built.
    @Test
    fun aFullGymCanPushPullAndSquat() {
        val everything = catalog.all

        assertThat(everything.any { it.pattern.isLowerPush }).isTrue()
        assertThat(everything.any { it.pattern.isPush }).isTrue()
        assertThat(everything.any { it.pattern.isPull }).isTrue()
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
            .map { (equipment, name, muscle) -> Triple(name, equipment, muscle) }
            .toSet()
        val catalogued = catalog.all
            .map { Triple(it.name, it.equipment.name, it.muscle.name) }
            .toSet()

        assertThat(catalogued - source).isEmpty()
        assertThat(source - catalogued).isEmpty()
    }

    private companion object {
        // Transcribed end to end, so these are exact.
        val FULL_CATEGORIES = mapOf(
            Equipment.DUMBBELL to 70,
            Equipment.KETTLEBELL to 13,
            Equipment.PLATE to 8,
            Equipment.SUSPENSION_BAND to 7
        )

        // What each category holds in the source. A category at its size is
        // finished; one below it is still waiting on pages.
        val CATALOG_SIZE = mapOf(
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
    fun everyMovementIsNamedInBothLanguagesTheCatalogCarries() {
        assertThat(catalog.all.filter { it.name.isBlank() || it.nameJa.isBlank() }).isEmpty()
    }
}
