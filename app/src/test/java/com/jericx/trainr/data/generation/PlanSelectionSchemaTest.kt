package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.generation.SessionFocus
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot
import com.jericx.trainr.domain.generation.SlotTier
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class PlanSelectionSchemaTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val builder = PlanSkeletonBuilder(catalog)

    private fun user(
        goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
        days: Int = 4,
        minutes: Int = 60,
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE
    ) = UserProfile(
        id = 1, age = 34, weight = 80f, fitnessGoal = goal, experienceLevel = experience,
        availableEquipment = Equipment.entries.toList(), workoutDaysPerWeek = days, workoutDuration = minutes
    )

    private fun skeleton(user: UserProfile = user()) = builder.build(PlanRequest(user, 1, 0L))

    private fun slot(id: String, vararg candidates: String) = SkeletonSlot(
        id = id, label = "the $id", tier = SlotTier.ACCESSORY, patterns = emptyList(), muscles = emptySet(),
        candidates = candidates.toList(), sets = 3, restSeconds = 60
    )

    private fun handBuilt(vararg days: SkeletonDay) = PlanSkeleton(
        title = "Test Week", days = days.toList(), units = UnitSystem.METRIC, maxSetsPerSession = 20,
        sessionCeilingMinutes = 90, weeklySetsByRegion = emptyMap(), uncoveredPatterns = emptySet()
    )

    private fun SelectionSchema.Obj.child(name: String) = properties.toMap().getValue(name)

    @Test
    fun everyOpenSlotIsAnEnumOfItsOwnCandidatesInRankOrder() {
        val skeleton = skeleton()
        val schema = planSelectionSchema(skeleton)

        skeleton.days.filter { it.openSlots.isNotEmpty() }.forEach { day ->
            val session = schema.child(day.id) as SelectionSchema.Obj
            day.openSlots.forEach { slot ->
                assertThat(session.child(slot.id)).isEqualTo(SelectionSchema.OneOf(slot.candidates, slot.label))
            }
        }
    }

    // Disjoint lists are what make a movement used twice in one session
    // something the model cannot write.
    @Test
    fun noSessionOffersOneMovementInTwoSlots() {
        FitnessGoal.entries.forEach { goal ->
            planSelectionSchema(skeleton(user(goal = goal, days = 5))).properties.forEach { (day, session) ->
                val offered = (session as SelectionSchema.Obj).properties.map { it.second }
                    .filterIsInstance<SelectionSchema.OneOf>().flatMap { it.values }
                assertWithMessage("$goal $day").that(offered).containsNoDuplicates()
            }
        }
    }

    @Test
    fun settledSlotsAndSettledSessionsAreNotAsked() {
        val schema = planSelectionSchema(
            handBuilt(
                SkeletonDay(1, SessionFocus.FULL_BODY, listOf(slot("warm_up", "arm_circles"), slot("primary", "a", "b"))),
                SkeletonDay(4, SessionFocus.MOBILITY_FLOW, listOf(slot("mobility", "stretching")))
            )
        )

        assertThat(schema.properties.map { it.first }).containsExactly("day1")
        assertThat((schema.child("day1") as SelectionSchema.Obj).properties.map { it.first })
            .containsExactly("primary", "title").inOrder()
    }

    @Test
    fun aSlotWithNothingToOfferAsksForTextRatherThanAnImpossibleEnum() {
        val schema = planSelectionSchema(
            handBuilt(SkeletonDay(1, SessionFocus.FULL_BODY, listOf(slot("primary"), slot("secondary", "a", "b"))))
        )

        assertThat((schema.child("day1") as SelectionSchema.Obj).child("primary"))
            .isEqualTo(SelectionSchema.Text("the primary"))
    }

    // Both platforms render the same bytes from the same week, so the two
    // apps cannot drift apart in what they ask a model for.
    @Test
    fun theSchemaMatchesTheSharedFixture() {
        val fixture = File("../docs/fixtures/plan-selection-schema.json").readText()

        assertThat(planSelectionSchema(skeleton()).toJson()).isEqualTo(fixture)
    }

    @Test
    fun theRenderingIsJsonThatRequiresEverything() {
        val rendered = Json.parseToJsonElement(planSelectionSchema(skeleton()).toJson()).jsonObject

        assertThat(rendered["required"].toString()).isEqualTo(
            rendered["properties"]!!.jsonObject.keys.joinToString(",", "[", "]") { "\"$it\"" }
        )
    }

    // The schema is paid for on every request, so the largest week the setup
    // screen allows has to stay cheap.
    @Test
    fun theLargestWeekStaysUnderTwelveKilobytes() {
        FitnessGoal.entries.forEach { goal ->
            (1..7).forEach { days ->
                val bytes = planSelectionSchema(skeleton(user(goal, days, 90, ExperienceLevel.ADVANCED)))
                    .toJson().toByteArray().size
                assertWithMessage("$goal ${days}d").that(bytes).isLessThan(12 * 1024)
            }
        }
    }
}
