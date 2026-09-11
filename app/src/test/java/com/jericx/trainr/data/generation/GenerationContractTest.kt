package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import java.io.File
import org.junit.Test

// The document is read, not paraphrased: a contract nothing executes drifts
// from the code within a release.
class GenerationContractTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val contract = File("../docs/generation-contract.md").readText()

    // The client the shared fixture was rendered for.
    private val user = UserProfile(
        id = 1, age = 34, weight = 80f, fitnessGoal = FitnessGoal.MUSCLE_GAIN,
        experienceLevel = ExperienceLevel.INTERMEDIATE, availableEquipment = Equipment.entries.toList(),
        workoutDaysPerWeek = 4, workoutDuration = 60
    )

    private val request = PlanRequest(user, weekNumber = 1, startDateMillis = 0L)
    private val skeleton = PlanSkeletonBuilder(catalog).build(request)

    private fun jsonBlocks(): List<String> = Regex("```json\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        .findAll(contract).map { it.groupValues[1] }.toList()

    // Both platforms answer the same example, so it is a shared file rather
    // than prose each repo keeps its own copy of.
    private val example = File("../docs/fixtures/plan-selection-example.json").readText()

    @Test
    fun theSchemaTheContractShowsIsTheSchemaTheAppSends() {
        val excerpt = jsonBlocks().first()
        val rendered = planSelectionSchema(skeleton).toJson()

        // An excerpt, so every line of it must appear in the real thing.
        excerpt.lines().filter { it.isNotBlank() }.drop(1).dropLast(1).forEach { line ->
            assertThat(rendered).contains(line.trim().trimEnd(','))
        }
    }

    @Test
    fun theExampleTheContractShowsIsTheSharedOne() {
        assertThat(jsonBlocks().last()).isEqualTo(example)
    }

    @Test
    fun theContractsWorkedExampleNeedsNoRepairAndAssemblesIntoAWeek() {
        val repaired = PlanSelectionRepair().repair(example, skeleton) as SelectionRepairResult.Accepted

        assertThat(repaired.repairs).isEqualTo(0)
        val week = PlanAssembler(catalog).assemble(skeleton, repaired.selection, request)
        assertThat(week).isNotNull()
        assertThat(week!!.workoutDays).hasSize(user.workoutDaysPerWeek)
        assertThat(week.workoutDays.map { it.title })
            .containsExactly("Chest and Back", "Squats and Hamstrings", "Shoulders and Arms", "Glutes and Quads")
    }

    // The worked example is an answer to the fixture, so every key it names
    // must be one that week actually offered.
    @Test
    fun everyMovementTheExampleNamesWasOnTheSlotsOwnList() {
        val repaired = PlanSelectionRepair().repair(example, skeleton) as SelectionRepairResult.Accepted

        skeleton.days.forEach { day ->
            val chosen = repaired.selection.days[day.id]?.slots.orEmpty()
            day.openSlots.forEach { slot ->
                assertThat(slot.candidates).contains(chosen.getValue(slot.id))
            }
        }
    }
}
